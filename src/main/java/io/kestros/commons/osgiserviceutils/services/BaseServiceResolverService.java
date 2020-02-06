package io.kestros.commons.osgiserviceutils.services;

import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.closeServiceResourceResolver;
import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.getOpenServiceResourceResolverOrNullAndLogExceptions;

import javax.annotation.Nullable;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Baseline OSGI Service which when activated, creates a ResourceResolver for a service User.  When
 * deactivated the ResourceResolver is closed.
 */
public abstract class BaseServiceResolverService {

  private ResourceResolver serviceResourceResolver;

  private ComponentContext componentContext;

  protected abstract String getServiceUserName();

  /**
   * Activates Cache service. Opens service ResourceResolver, which is used to build cached files.
   *
   * @param ctx ComponentContext.
   */
  @Activate
  public void activate(final ComponentContext ctx) {
    serviceResourceResolver = getOpenServiceResourceResolverOrNullAndLogExceptions(
        getServiceUserName(), getServiceResourceResolver(), getResourceResolverFactory(), this);
    componentContext = ctx;
  }

  /**
   * Deactivates the Service and closes the Service ResourceResolver.
   */
  @Deactivate
  public void deactivate() {
    closeServiceResourceResolver(getServiceResourceResolver(), this);
  }

  protected abstract ResourceResolverFactory getResourceResolverFactory();

  protected ComponentContext getComponentContext() {
    return this.componentContext;
  }

  @Nullable
  protected ResourceResolver getServiceResourceResolver() {
    return this.serviceResourceResolver;
  }

}
