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

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.osgiserviceutils.services.BaseServiceResolverService;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.cache.ManagedCacheService;
import java.util.Date;
import java.util.Map;
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

  private static final long serialVersionUID = -4534057590200718400L;
  protected final Logger log = LoggerFactory.getLogger(getClass());
  private boolean isLive = true;
  private Date lastPurged;
  private String lastPurgedBy;

  protected abstract void doPurge(ResourceResolver resourceResolver) throws CachePurgeException;

  /**
   * Logic run after cache purge is completed.
   *
   * @param resourceResolver ResourceResolver.
   */
  protected abstract void afterCachePurgeComplete(ResourceResolver resourceResolver);

  /**
   * Adds cache creation job to the job queue, if the CacheService has been configured with a
   * CacheCreationJobName.
   *
   * @param jobProperties Property valueMap to send to the CacheService's JobConsumer, if one has
   *                      been configured.
   */
  public void addCacheCreationJob(final Map<String, Object> jobProperties) {
    if (getJobManager() != null && StringUtils.isNotEmpty(getCacheCreationJobName())) {
      log.info("Starting cache job {}. {}", getCacheCreationJobName(), jobProperties);
      getJobManager().addJob(getCacheCreationJobName(), jobProperties);
    }
  }

  protected abstract long getMinimumTimeBetweenCachePurges();

  protected abstract String getCacheCreationJobName();

  protected abstract JobManager getJobManager();

  @Override
  public void purgeAll(ResourceResolver resourceResolver) throws CachePurgeException {
    if (isCachePurgeTimeoutExpired()) {
      try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
        if (serviceResourceResolver.isLive()) {
          this.lastPurged = new Date();
          this.lastPurgedBy = resourceResolver.getUserID();
          log.info("{}: Clearing all cached data.", getDisplayName());
          doPurge(serviceResourceResolver);
          this.afterCachePurgeComplete(serviceResourceResolver);
        } else {
          log.error(
              "{}: Failed to clear cached data. Service ResourceResolver was not live or was null",
              getDisplayName());
          throw new CachePurgeException(String.format(
              "Failed to purge cache %s. Resource Resolver was either null, or already closed.",
              getDisplayName()));
        }
      } catch (LoginException e) {
        log.error("{}: Failed to clear cached data.", getDisplayName());
        throw new CachePurgeException(String.format(
            "Failed to purge cache %s. %s",
            getDisplayName(), e.getMessage()));
      }
    } else {
      log.debug("{}: Skipping cache purge, minimum time between purges has not elapsed.",
          getDisplayName());
    }
  }

  @Override
  public void enable(final ResourceResolver resourceResolver) throws CachePurgeException {
    this.purgeAll(resourceResolver);
    this.isLive = true;
  }

  @Override
  public void disable(final ResourceResolver resourceResolver) throws CachePurgeException {
    this.purgeAll(resourceResolver);
    this.isLive = false;
  }

  /**
   * Whether the cacheService is live or not.  If a cache service is not live, no values will be
   * cached.
   *
   * @return Whether the cacheService is live or not.  If a cache service is not live, no values
   *     will be cached.
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
  @Override
  public String getLastPurgedBy() {
    return lastPurgedBy;
  }

  /**
   * Service display name.
   *
   * @return Service display name.
   */
  public String getServiceClassName() {
    return getClass().getAnnotatedInterfaces()[0].getType().getTypeName();
  }

  protected boolean isCachePurgeTimeoutExpired() {
    return getTimeSinceLastPurge() == null
           || getTimeSinceLastPurge() > getMinimumTimeBetweenCachePurges();
  }

  private Long getTimeSinceLastPurge() {
    if (getLastPurged() != null) {
      return new Date().getTime() - getLastPurged().getTime();
    }
    return null;
  }
}
