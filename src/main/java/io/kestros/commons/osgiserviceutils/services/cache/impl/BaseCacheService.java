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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

  /**
   * Single shared daemon thread that runs deferred cache purges. Deferred purge tasks are tiny
   * (they call the same purge path an event-driven purge uses), so one thread serves every cache
   * service. Daemon so it never blocks JVM shutdown.
   */
  private static final ScheduledExecutorService DEFERRED_PURGE_SCHEDULER
          = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kestros-cache-deferred-purge");
            thread.setDaemon(true);
            return thread;
          });

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private boolean isLive = true;
  private final Object purgeLock = new Object();
  private Date lastPurged;
  private String lastPurgedBy;
  private boolean pendingDeferredPurge = false;
  private String pendingDeferredPurgeBy;

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
    synchronized (purgeLock) {
      if (!isCachePurgeTimeoutExpired()) {
        // A purge request during the cooldown must never be lost — content written after the
        // last executed purge would otherwise stay stale until an unrelated change (e.g. the
        // trailing writes of a package install). Coalesce all such requests into one deferred
        // purge that runs as soon as the cooldown expires.
        this.pendingDeferredPurgeBy = resourceResolver.getUserID();
        if (!pendingDeferredPurge) {
          this.pendingDeferredPurge = true;
          long delayMs = getRemainingCooldownMillis() + 50;
          DEFERRED_PURGE_SCHEDULER.schedule(this::runDeferredPurge, delayMs,
                  TimeUnit.MILLISECONDS);
          log.debug("{}: Purge requested during cooldown — deferred purge scheduled in {}ms.",
                  getDisplayName().replaceAll("[\r\n]", ""), delayMs);
        }
        return;
      }
      // An immediate purge makes any pending deferred purge redundant.
      this.pendingDeferredPurge = false;
      this.pendingDeferredPurgeBy = null;
    }
    executePurge(resourceResolver.getUserID());
  }

  /**
   * Opens a service resource resolver and runs {@link #doPurge}. {@code lastPurged}/
   * {@code lastPurgedBy} are stamped only once the resolver is confirmed live, preserving the
   * contract that a failed purge leaves them untouched.
   *
   * @param purgedBy User ID to record as the purger.
   * @throws CachePurgeException Failed to purge the cache.
   */
  private void executePurge(@Nullable String purgedBy) throws CachePurgeException {
    try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
      if (serviceResourceResolver.isLive()) {
        synchronized (purgeLock) {
          this.lastPurged = new Date();
          this.lastPurgedBy = purgedBy;
        }
        log.info("{}: Clearing all cached data.", getDisplayName().replaceAll("[\r\n]", ""));
        doPurge(serviceResourceResolver);
        this.afterCachePurgeComplete(serviceResourceResolver);
      } else {
        log.error(
                "{}: Failed to clear cached data. Service ResourceResolver was not live or was "
                        + "null",
                getDisplayName().replaceAll("[\r\n]", ""));
        throw new CachePurgeException(String.format(
                "Failed to purge cache %s. Resource Resolver was either null, or already closed.",
                getDisplayName()));
      }
    } catch (LoginException e) {
      log.error("{}: Failed to clear cached data.", getDisplayName().replaceAll("[\r\n]", ""));
      throw new CachePurgeException(String.format(
              "Failed to purge cache %s. %s",
              getDisplayName(), e.getMessage()), e);
    }
  }

  /**
   * Runs a purge that was deferred because it was requested during the cooldown window. Failures
   * are logged rather than thrown — there is no caller to receive them.
   */
  private void runDeferredPurge() {
    String purgedBy;
    synchronized (purgeLock) {
      if (!pendingDeferredPurge) {
        return;
      }
      this.pendingDeferredPurge = false;
      purgedBy = pendingDeferredPurgeBy != null ? pendingDeferredPurgeBy : "deferred-purge";
      this.pendingDeferredPurgeBy = null;
    }
    try {
      executePurge(purgedBy);
    } catch (CachePurgeException e) {
      log.error("{}: Deferred cache purge failed. {}",
              getDisplayName().replaceAll("[\r\n]", ""), e.getMessage());
    }
  }

  private long getRemainingCooldownMillis() {
    Date purged;
    synchronized (purgeLock) {
      purged = this.lastPurged;
    }
    if (purged == null) {
      return 0;
    }
    long elapsed = new Date().getTime() - purged.getTime();
    return Math.max(0, getMinimumTimeBetweenCachePurges() - elapsed);
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
