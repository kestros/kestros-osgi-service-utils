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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Baseline logic for managing CacheServices. Allows purging, enabling, disabling and tracking cache
 * purge actions.
 */
public abstract class BaseCacheService extends BaseServiceResolverService
        implements CacheService, ManagedCacheService {

  private static final long serialVersionUID = 1L;

  private static final String DEFERRED_PURGE_USER = "deferred-purge";

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private boolean isLive = true;
  private volatile Date lastPurged;
  private volatile String lastPurgedBy;
  private volatile String pendingPurgeBy;

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

  /**
   * Sling Scheduler used to debounce purge requests. When null, a purge request runs immediately
   * if the cooldown has expired and is skipped otherwise.
   *
   * @return Sling Scheduler, or null.
   */
  @Nullable
  protected Scheduler getScheduler() {
    return null;
  }

  /**
   * Quiet period after the most recent purge request before the debounced purge runs.
   *
   * @return Quiet period in milliseconds.
   */
  protected long getPurgeDebounceMillis() {
    return 500L;
  }

  /**
   * Name of the one-shot Sling Scheduler job that runs the debounced purge. One per service.
   *
   * @return Scheduler job name.
   */
  @Nonnull
  protected String getPurgeJobName() {
    return "kestros-cache-purge-" + getClass().getName();
  }

  /**
   * Requests a purge. With a Sling Scheduler, each request (re)schedules one named one-shot job a
   * quiet period out, so a burst of requests ends in exactly one purge. Without one, the purge runs
   * immediately if the cooldown has expired.
   *
   * @param resourceResolver ResourceResolver of the user requesting the purge.
   * @throws CachePurgeException Failed to purge the cache.
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Override
  public void purgeAll(@Nonnull ResourceResolver resourceResolver) throws CachePurgeException {
    Scheduler scheduler = getScheduler();
    if (scheduler == null) {
      if (isCachePurgeTimeoutExpired()) {
        executePurge(resourceResolver.getUserID());
      } else {
        log.debug("{}: Skipping cache purge, minimum time between purges has not elapsed.",
                getDisplayName().replaceAll("[\r\n]", ""));
      }
      return;
    }
    String requestedBy = resourceResolver.getUserID();
    this.pendingPurgeBy = requestedBy != null ? requestedBy : DEFERRED_PURGE_USER;
    if (!schedulePurgeJob(scheduler, getPurgeDebounceMillis())) {
      this.pendingPurgeBy = null;
      executePurge(requestedBy);
    }
  }

  /**
   * Replaces the named purge job with one that fires after the given delay.
   *
   * @param scheduler Sling Scheduler.
   * @param delayMillis Delay before the job fires.
   * @return Whether the scheduler accepted the job.
   */
  private boolean schedulePurgeJob(@Nonnull Scheduler scheduler, long delayMillis) {
    String jobName = getPurgeJobName();
    scheduler.unschedule(jobName);
    ScheduleOptions options = scheduler.AT(new Date(System.currentTimeMillis() + delayMillis))
            .name(jobName)
            .canRunConcurrently(false);
    boolean scheduled = scheduler.schedule((Runnable) this::runScheduledPurge, options);
    if (!scheduled) {
      log.warn("{}: Sling Scheduler refused the purge job, purging immediately.",
              getDisplayName().replaceAll("[\r\n]", ""));
    }
    return scheduled;
  }

  /**
   * Body of the debounced purge job. Honours the cooldown by rescheduling for its remainder
   * instead of purging early. There is no caller to receive an exception, so failures are logged.
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  void runScheduledPurge() {
    String purgedBy = this.pendingPurgeBy;
    if (purgedBy == null) {
      return;
    }
    Scheduler scheduler = getScheduler();
    long remainingCooldown = getRemainingCooldownMillis();
    if (scheduler != null && remainingCooldown > 0
        && schedulePurgeJob(scheduler, remainingCooldown)) {
      return;
    }
    this.pendingPurgeBy = null;
    try {
      executePurge(purgedBy);
    } catch (CachePurgeException e) {
      log.error(getDisplayName().replaceAll("[\r\n]", "") + ": Deferred cache purge failed. "
              + String.valueOf(e.getMessage()).replaceAll("[\r\n]", ""));
    }
  }

  /**
   * Opens a service resource resolver and runs {@link #doPurge}. {@code lastPurged}/
   * {@code lastPurgedBy} are stamped only once the resolver is confirmed live.
   *
   * @param purgedBy User ID to record as the purger.
   * @throws CachePurgeException Failed to purge the cache.
   */
  private void executePurge(@Nullable String purgedBy) throws CachePurgeException {
    try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
      if (serviceResourceResolver.isLive()) {
        this.lastPurged = new Date();
        this.lastPurgedBy = purgedBy;
        log.info("{}: Clearing all cached data.", getDisplayName().replaceAll("[\r\n]", ""));
        doPurge(serviceResourceResolver);
        this.afterCachePurgeComplete(serviceResourceResolver);
      } else {
        log.error("{}: Failed to purge cache. Resource Resolver was either closed or "
                        + "null",
                getDisplayName().replaceAll("[\r\n]", ""));
        throw new CachePurgeException(String.format(
                "Failed to purge cache %s. Resource Resolver was either null, or already "
                        + "closed.",
                getDisplayName()));
      }
    } catch (LoginException e) {
      throw new CachePurgeException(String.format(
              "Failed to purge cache %s. %s",
              getDisplayName(), e.getMessage()), e);
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
   * Deactivates the service. A purge still waiting on the scheduler is unscheduled and run now, so
   * a purge requested just before shutdown is not lost.
   *
   * @param componentContext ComponentContext.
   */
  @SuppressFBWarnings("CRLF_INJECTION_LOGS")
  @Override
  public void deactivate(@Nonnull ComponentContext componentContext) {
    Scheduler scheduler = getScheduler();
    if (scheduler != null) {
      scheduler.unschedule(getPurgeJobName());
    }
    String purgedBy = this.pendingPurgeBy;
    this.pendingPurgeBy = null;
    if (purgedBy != null) {
      try {
        executePurge(purgedBy);
      } catch (CachePurgeException e) {
        log.warn(getDisplayName().replaceAll("[\r\n]", "")
                + ": Could not run pending purge during deactivation. "
                + String.valueOf(e.getMessage()).replaceAll("[\r\n]", ""));
      }
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
