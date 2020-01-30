package io.kestros.commons.osgiserviceutils.services.cache.impl;

import io.kestros.commons.osgiserviceutils.services.cache.impl.JcrFileCacheService;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Reference;

public class SampleJcrCacheService extends JcrFileCacheService {

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Reference
  private JobManager jobManager;

  @Override
  public String getServiceCacheRootPath() {
    return "/var/cache/test";
  }

  @Override
  protected String getServiceUserName() {
    return "test-jcr-cache-service-user";
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }

  @Override
  public String getDisplayName() {
    return "Sample Jcr Cache Service";
  }

  @Override
  public JobManager getJobManager() {
    return jobManager;
  }

  @Override
  protected long getMinimumTimeBetweenCachePurges() {
    return 1000;
  }

  @Override
  public String getCacheCreationJobName() {
    return "sample-creation";
  }
}
