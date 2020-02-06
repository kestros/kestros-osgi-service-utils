package io.kestros.commons.osgiserviceutils.services.eventlisteners;

import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import java.util.List;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.ComponentContext;

/**
 * Listens for ResourceChanges and purges the specified CacheServices when a change is detected.
 */
public interface CachePurgeOnResourceChangeEventListener extends ResourceChangeListener {

  /**
   * Activates the EventListener Service.
   *
   * @param ctx ComponentContext.
   */
  void activate(ComponentContext ctx);

  /**
   * Deactivates the EventListener Service.
   */
  void deactivate();

  /**
   * {@link CacheService} implementations to purge.
   *
   * @param <T> Extends {@link CacheService}
   * @return {@link CacheService} implementations to purge.
   */
  <T extends CacheService> List<T> getCacheServices();

  /**
   * ResourceResolverFactory used to get ServiceResourceResolver.
   *
   * @return ResourceResolverFactory used to get ServiceResourceResolver.
   */
  ResourceResolverFactory getResourceResolverFactory();

}
