package io.kestros.commons.osgiserviceutils.services.cache.impl;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Reference;

public class SampleCacheService extends BaseCacheService {

  @Reference
  private JobManager jobManager;

  @Override
  protected void doPurge(ResourceResolver resourceResolver) throws CachePurgeException {
    // Do nothing.
  }

  @Override
  protected long getMinimumTimeBetweenCachePurges() {
    return 1000;
  }

  @Override
  protected String getCacheCreationJobName() {
    return "sample-creation-job-name";
  }

  @Override
  protected JobManager getJobManager() {
    return jobManager;
  }

  @Override
  public void activate() {
  }

  @Override
  public void deactivate() {
  }

  @Override
  public String getDisplayName() {
    return "sample cache service";
  }
}
