package io.kestros.commons.osgiserviceutils.services.cache.impl;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.cache.ManagedCacheService;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseCacheService implements CacheService, ManagedCacheService {

  private boolean isLive;
  private Date lastPurged;
  private String lastPurgedBy;

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected abstract void doPurge(ResourceResolver resourceResolver) throws CachePurgeException;

  /**
   * Adds cache creation job to the job queue, if the CacheService has been configured with a
   * CacheCreationJobName.
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
  public void purgeAll(final ResourceResolver resourceResolver) throws CachePurgeException {
    if (isCachePurgeTimeoutExpired()) {
      this.lastPurged = new Date();
      if (resourceResolver != null) {
        this.lastPurgedBy = resourceResolver.getUserID();
        doPurge(resourceResolver);
      }
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

  @Override
  public boolean isLive() {
    return isLive;
  }

  @Override
  public Date getLastPurged() {
    if (lastPurged == null) {
      return null;
    }
    return new Date(lastPurged.getTime());
  }

  @Override
  public String getLastPurgedBy() {
    return lastPurgedBy;
  }

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
