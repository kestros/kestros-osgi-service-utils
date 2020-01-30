package io.kestros.commons.osgiserviceutils.services.eventlisteners.impl;

import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.cache.impl.SampleCacheService;
import java.util.Arrays;
import java.util.List;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service = ResourceChangeListener.class,
           property = {ResourceChangeListener.CHANGES + "=ADDED",
               ResourceChangeListener.CHANGES + "=CHANGED",
               ResourceChangeListener.CHANGES + "=REMOVED",
               ResourceChangeListener.CHANGES + "=PROVIDER_ADDED",
               ResourceChangeListener.CHANGES + "=PROVIDER_REMOVED",
               ResourceChangeListener.PATHS + "=/apps"},
           immediate = true)
public class SampleCachePurgeOnResourceChangeEventListener
    extends BaseCachePurgeOnResourceChangeEventListener {

  @Reference(cardinality = ReferenceCardinality.OPTIONAL,
             policyOption = ReferencePolicyOption.GREEDY)
  private SampleCacheService cacheService;

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Override
  protected String getServiceUserName() {
    return "service-user";
  }

  @Override
  public <T extends CacheService> List<T> getCacheServices() {
    return Arrays.asList((T) cacheService);
  }

  @Override
  public ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }
}
