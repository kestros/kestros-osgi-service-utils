package io.kestros.commons.osgiserviceutils.services.eventlisteners.impl;

import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.closeServiceResourceResolver;
import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.getOpenServiceResourceResolverOrNullAndLogExceptions;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.eventlisteners.CachePurgeOnResourceChangeEventListener;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseCachePurgeOnResourceChangeEventListener
    implements CachePurgeOnResourceChangeEventListener {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected abstract String getServiceUserName();

  private ComponentContext componentContext;

  private ResourceResolver serviceResourceResolver;

  /**
   * Activates event lister service. Opens ResourceResolver for service user.
   */
  @Activate
  public void activate(ComponentContext ctx) {
    serviceResourceResolver = getOpenServiceResourceResolverOrNullAndLogExceptions(
        getServiceUserName(), getServiceResourceResolver(), getResourceResolverFactory(), this);
    componentContext = ctx;
  }

  /**
   * Deactivates event lister service. Closes resource resolver if open.
   */
  @Deactivate
  public void deactivate() {
    closeServiceResourceResolver(getServiceResourceResolver(), this);
  }

  @Override
  public void onChange(@Nonnull List<ResourceChange> list) {
    try {
      for (CacheService cacheService : getCacheServices()) {
        if (cacheService != null) {
          cacheService.purgeAll(getServiceResourceResolver());
        } else {
          log.error("Failed to create cache purge job. No cache service detected for {}.",
              getClass().getSimpleName());
        }
      }
    } catch (CachePurgeException exception) {
      log.error("Failed to create cache purge job. {}", exception.getMessage());
    }
  }

  protected ComponentContext getComponentContext() {
    return this.componentContext;
  }

  @Nonnull
  protected ResourceResolver getServiceResourceResolver() {
    return this.serviceResourceResolver;
  }
}
