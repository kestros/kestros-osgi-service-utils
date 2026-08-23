/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.kestros.commons.osgiserviceutils.services.cache.impl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.osgiserviceutils.services.BaseServiceResolverService;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.cache.ManagedCacheService;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Baseline logic for managing CacheServices. Allows purging, enabling, disabling and tracking cache
 * purge actions.
 */
public abstract class BaseCacheService extends BaseServiceResolverService
        implements CacheService, ManagedCacheService {

  private static final long serialVersionUID = 1L;

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private boolean isLive = true;
  private final Object purgeLock = new Object();
  private final Object purgeExecutionLock = new Object();
  private volatile Date lastPurged;
  private volatile String lastPurgedBy;
  private boolean pendingDeferredPurge = false;
  private String pendingDeferredPurgeBy;
  private boolean deferredPurgeRetried = false;
  private transient ScheduledFuture<?> pendingDeferredPurgeFuture;

  /**
   * Per-instance scheduler for deferred purges. Created lazily; its single worker thread is a
   * daemon with a 30s idle timeout, so an idle or abandoned scheduler releases its thread (and
   * with it the bundle classloader) on its own — important because several subclasses override
   * {@code deactivate} without calling super, so shutdown cannot be relied on.
   */
  private transient ScheduledThreadPoolExecutor deferredPurgeScheduler;

  @Nonnull
  private ScheduledThreadPoolExecutor getDeferredPurgeScheduler() {
    synchronized (purgeLock) {
      if (deferredPurgeScheduler == null || deferredPurgeScheduler.isShutdown()) {
        deferredPurgeScheduler = new ScheduledThreadPoolExecutor(1,
                (@Nonnull final Runnable runnable) -> {
                  Thread thread = new Thread(runnable, "kestros-cache-deferred-purge-" + getClass()
                          .getSimpleName());
                  thread.setDaemon(true);
                  return thread;
                });
        deferredPurgeScheduler.setKeepAliveTime(30, TimeUnit.SECONDS);
        deferredPurgeScheduler.allowCoreThreadTimeOut(true);
        deferredPurgeScheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      }
      return deferredPurgeScheduler;
    }
  }

  protected abstract void doPurge(@Nonnull ResourceResolver resourceResolver) throws
          CachePurgeException;

  /**
   * Logic run after cache purge is completed.
   *
   * @param resourceResolver ResourceResolver.
   */
  protected abstract void afterCachePurgeComplete(@Nonnull ResourceResolver resourceResolver);

  /**
   * Adds cache creation job to the job queue, if the CacheService has been configured with a
   * CacheCreationJobName.
   *
   * @param jobProperties Property valueMap to send to the CacheService's JobConsumer, if
   *         one has been configured.
   */
  public void addCacheCreationJob(@Nonnull final Map<String, Object> jobProperties) {
    if (getJobManager() != null && StringUtils.isNotEmpty(getCacheCreationJobName())) {
      log.info("Starting cache job {}", getCacheCreationJobName().replaceAll("[\r\n]", ""));
      getJobManager().addJob(getCacheCreationJobName(), jobProperties);
    }
  }

  protected abstract long getMinimumTimeBetweenCachePurges();

  @Nonnull
  protected abstract String getCacheCreationJobName();

  @Nullable
  protected abstract JobManager getJobManager();

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Override
  public void purgeAll(@Nonnull ResourceResolver resourceResolver) throws CachePurgeException {
    String requestedBy = resourceResolver.getUserID();
    synchronized (purgeLock) {
      if (!isCachePurgeTimeoutExpired()) {
        // A purge request during the cooldown must never be lost — content written after the
        // last executed purge would otherwise stay stale until an unrelated change (e.g. the
        // trailing writes of a package install). Coalesce all such requests into one deferred
        // purge that runs as soon as the cooldown expires.
        this.pendingDeferredPurgeBy = requestedBy;
        armDeferredPurge();
        return;
      }
      // An immediate purge makes any pending deferred purge redundant.
      clearPendingDeferredPurge();
    }
    try {
      executePurge(requestedBy);
      synchronized (purgeLock) {
        this.deferredPurgeRetried = false;
      }
    } catch (CachePurgeException e) {
      // The immediate purge replaced (cancelled) any pending deferred purge and then failed —
      // re-arm a deferred purge so the request is still never lost.
      synchronized (purgeLock) {
        this.pendingDeferredPurgeBy = requestedBy;
        armDeferredPurge();
      }
      throw e;
    }
  }

  /**
   * Schedules (or keeps) the single coalesced deferred purge. Caller must hold {@code purgeLock}.
   */
  private void armDeferredPurge() {
    if (!pendingDeferredPurge) {
      this.pendingDeferredPurge = true;
      long delayMs = getRemainingCooldownMillis() + 50;
      this.pendingDeferredPurgeFuture = getDeferredPurgeScheduler()
              .schedule(this::runDeferredPurge, delayMs, TimeUnit.MILLISECONDS);
      log.debug("{}: Purge requested during cooldown — deferred purge scheduled in {}ms.",
              getDisplayName().replaceAll("[\r\n]", ""), delayMs);
    }
  }

  /**
   * Clears the pending deferred purge and cancels its scheduled task, so an orphaned task can
   * never fire against a later pending state. Caller must hold {@code purgeLock}.
   */
  private void clearPendingDeferredPurge() {
    this.pendingDeferredPurge = false;
    this.pendingDeferredPurgeBy = null;
    if (pendingDeferredPurgeFuture != null) {
      pendingDeferredPurgeFuture.cancel(false);
      pendingDeferredPurgeFuture = null;
    }
  }

  /**
   * Opens a service resource resolver and runs {@link #doPurge}. {@code lastPurged}/
   * {@code lastPurgedBy} are stamped only once the resolver is confirmed live, preserving the
   * contract that a resolver-acquisition failure leaves them untouched. Execution is serialized
   * on its own lock so {@code doPurge} implementations are never invoked concurrently, without
   * blocking {@code purgeAll} callers on a slow purge.
   *
   * @param purgedBy User ID to record as the purger.
   * @throws CachePurgeException Failed to purge the cache.
   */
  private void executePurge(@Nullable String purgedBy) throws CachePurgeException {
    synchronized (purgeExecutionLock) {
      try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
        if (serviceResourceResolver.isLive()) {
          this.lastPurged = new Date();
          this.lastPurgedBy = purgedBy;
          log.info("{}: Clearing all cached data.", getDisplayName().replaceAll("[\r\n]", ""));
          doPurge(serviceResourceResolver);
          this.afterCachePurgeComplete(serviceResourceResolver);
        } else {
          log.error(
                  "{}: Failed to clear cached data. Service ResourceResolver was not live or was "
                          + "null",
                  getDisplayName().replaceAll("[\r\n]", ""));
          throw new CachePurgeException(String.format(
                  "Failed to purge cache %s. Resource Resolver was either null, or already "
                          + "closed.",
                  getDisplayName()));
        }
      } catch (LoginException e) {
        log.error("{}: Failed to clear cached data.", getDisplayName().replaceAll("[\r\n]", ""));
        throw new CachePurgeException(String.format(
                "Failed to purge cache %s. %s",
                getDisplayName(), e.getMessage()), e);
      }
    }
  }

  /**
   * Runs a purge that was deferred because it was requested during the cooldown window. Re-checks
   * the cooldown first (the task may have adopted a pending state armed after a newer purge) and
   * reschedules instead of purging early. Failures are logged, and the purge is re-armed once —
   * there is no caller to receive an exception.
   */
  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private void runDeferredPurge() {
    String purgedBy;
    synchronized (purgeLock) {
      if (!pendingDeferredPurge) {
        return;
      }
      if (!isCachePurgeTimeoutExpired()) {
        // A newer immediate purge restarted the cooldown after this task was scheduled.
        // Reschedule for the remaining cooldown rather than bypassing the throttle.
        long delayMs = getRemainingCooldownMillis() + 50;
        this.pendingDeferredPurgeFuture = getDeferredPurgeScheduler()
                .schedule(this::runDeferredPurge, delayMs, TimeUnit.MILLISECONDS);
        log.debug("{}: Deferred purge rescheduled in {}ms (cooldown restarted).",
                getDisplayName().replaceAll("[\r\n]", ""), delayMs);
        return;
      }
      this.pendingDeferredPurge = false;
      this.pendingDeferredPurgeFuture = null;
      purgedBy = pendingDeferredPurgeBy != null ? pendingDeferredPurgeBy : "deferred-purge";
      this.pendingDeferredPurgeBy = null;
    }
    try {
      executePurge(purgedBy);
      synchronized (purgeLock) {
        this.deferredPurgeRetried = false;
      }
    } catch (Exception e) {
      log.error(String.format("%s: Deferred cache purge failed. %s",
              getDisplayName().replaceAll("[\r\n]", ""),
              e.getMessage() != null ? e.getMessage().replaceAll("[\r\n]", "") : e.toString()));
      synchronized (purgeLock) {
        if (!deferredPurgeRetried) {
          // One bounded retry so a transient failure cannot strand the cache stale; a
          // persistent failure (e.g. missing service user mapping) logs twice and stops.
          this.deferredPurgeRetried = true;
          this.pendingDeferredPurgeBy = purgedBy;
          armDeferredPurge();
        }
      }
    }
  }

  private long getRemainingCooldownMillis() {
    Date purged = this.lastPurged;
    if (purged == null) {
      return 0;
    }
    long elapsed = new Date().getTime() - purged.getTime();
    return Math.max(0, getMinimumTimeBetweenCachePurges() - elapsed);
  }

  /**
   * Deactivates the service: any pending deferred purge is executed immediately (best effort) so
   * a purge requested before shutdown is not lost, and the scheduler is shut down. Subclasses
   * overriding {@code deactivate} without calling super leave only an idle daemon thread that
   * times out on its own.
   *
   * @param componentContext ComponentContext.
   */
  @Override
  public void deactivate(@Nonnull org.osgi.service.component.ComponentContext componentContext) {
    String purgedBy;
    ScheduledThreadPoolExecutor scheduler;
    synchronized (purgeLock) {
      purgedBy = pendingDeferredPurge ? (pendingDeferredPurgeBy != null ? pendingDeferredPurgeBy
              : "deferred-purge") : null;
      clearPendingDeferredPurge();
      scheduler = deferredPurgeScheduler;
      deferredPurgeScheduler = null;
    }
    if (purgedBy != null) {
      try {
        executePurge(purgedBy);
      } catch (Exception e) {
        log.warn(String.format("%s: Could not run pending deferred purge during deactivation. %s",
                getDisplayName().replaceAll("[\r\n]", ""),
                e.getMessage() != null ? e.getMessage().replaceAll("[\r\n]", "") : e.toString()));
      }
    }
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
    super.deactivate(componentContext);
  }

  @Override
  public void enable(@Nonnull final ResourceResolver resourceResolver) throws CachePurgeException {
    this.purgeAll(resourceResolver);
    this.isLive = true;
  }

  @Override
  public void disable(@Nonnull final ResourceResolver resourceResolver) throws CachePurgeException {
    this.purgeAll(resourceResolver);
    this.isLive = false;
  }

  /**
   * Whether the cacheService is live or not.  If a cache service is not live, no values will be
   * cached.
   *
   * @return Whether the cacheService is live or not.  If a cache service is not live, no values
   *         will be cached.
   */
  @Override
  public boolean isLive() {
    return isLive;
  }

  /**
   * Date that the cache was last purged.
   *
   * @return Date that the cache was last purged.
   */
  @Nullable
  @Override
  public Date getLastPurged() {
    if (lastPurged == null) {
      return null;
    }
    return new Date(lastPurged.getTime());
  }

  /**
   * User ID of user or service user who lasted purged the cache.
   *
   * @return User ID of user or service user who lasted purged the cache.
   */
  @Nullable
  @Override
  public String getLastPurgedBy() {
    return lastPurgedBy;
  }

  /**
   * Service display name.
   *
   * @return Service display name.
   */
  @Nonnull
  public String getServiceClassName() {
    return getClass().getAnnotatedInterfaces()[0].getType().getTypeName();
  }

  protected boolean isCachePurgeTimeoutExpired() {
    Long timeSinceLastPurge = getTimeSinceLastPurge();
    return timeSinceLastPurge == null
            || timeSinceLastPurge > getMinimumTimeBetweenCachePurges();
  }

  @Nullable
  private Long getTimeSinceLastPurge() {
    Date lastPurged = getLastPurged();
    if (lastPurged != null) {
      return new Date().getTime() - lastPurged.getTime();
    }
    return null;
  }
}
