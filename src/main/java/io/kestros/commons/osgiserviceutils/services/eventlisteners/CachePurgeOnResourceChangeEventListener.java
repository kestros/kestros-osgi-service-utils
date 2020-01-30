package io.kestros.commons.osgiserviceutils.services.eventlisteners;

import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import java.util.List;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.ComponentContext;

public interface CachePurgeOnResourceChangeEventListener extends ResourceChangeListener {

  void activate(ComponentContext ctx);

  void deactivate();

  <T extends CacheService> List<T> getCacheServices();

  ResourceResolverFactory getResourceResolverFactory();

}
