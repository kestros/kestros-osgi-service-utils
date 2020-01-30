package io.kestros.commons.osgiserviceutils.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiServiceUtils {

  private OsgiServiceUtils() {
  }

  private static final Logger LOG = LoggerFactory.getLogger(OsgiServiceUtils.class);

  /**
   * Sets the specified resource resolver to a live service ResourceResolver, if it is not already
   * live.
   *
   * @param serviceName Service to open ResourceResolver as.
   * @param existingResourceResolver ResourceResolver to set live.
   * @param resourceResolverFactory ResourceResolveFactory to get ResourceResolver from if
   *     needed.
   * @param service Service the ResourceResolver will be used for.
   * @return Newly opened service ResourceResolver or existing resourceResolver if it is still live.
   */
  @Nonnull
  public static ResourceResolver getOpenServiceResourceResolver(@Nonnull final String serviceName,
      final ResourceResolver existingResourceResolver,
      @Nonnull final ResourceResolverFactory resourceResolverFactory, @Nonnull final Object service)
      throws LoginException {
    ResourceResolver resourceResolver = existingResourceResolver;
    if (resourceResolver != null && resourceResolver.isLive()) {
      LOG.info("Attempted to open ResourceResolver for service user {}, for service {}, but a live "
               + "ResourceResolver already exists.", service, service.getClass().getSimpleName());
    } else {

      Map<String, Object> params = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
          serviceName);

      resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
      LOG.info("Opened service user {} resourceResolver for service {}.", serviceName,
          service.getClass().getSimpleName());
    }
    return resourceResolver;
  }

  /**
   * Sets the specified resource resolver to a live service ResourceResolver, if it is not already
   * live.
   *
   * @param serviceName Service to open ResourceResolver as.
   * @param existingResourceResolver ResourceResolver to set live.
   * @param resourceResolverFactory ResourceResolveFactory to get ResourceResolver from if
   *     needed.
   * @param service Service the ResourceResolver will be used for.
   * @return Newly opened service ResourceResolver or existing resourceResolver if it is still live.
   */
  @Nullable
  public static ResourceResolver getOpenServiceResourceResolverOrNullAndLogExceptions(
      @Nonnull final String serviceName, final ResourceResolver existingResourceResolver,
      @Nonnull final ResourceResolverFactory resourceResolverFactory,
      @Nonnull final Object service) {
    try {
      return getOpenServiceResourceResolver(serviceName, existingResourceResolver,
          resourceResolverFactory, service);
    } catch (LoginException exception) {
      LOG.error("Failed to log into service user {} resourceResolver for {}. {}", serviceName,
          service.getClass().getSimpleName(), exception.getMessage());
    }
    return null;
  }

  /**
   * Closes a resource resolver if it is open.
   *
   * @param resourceResolver ResourceResolver to close.
   * @param service Service the ResourceResolver is used for.
   */
  public static void closeServiceResourceResolver(final ResourceResolver resourceResolver,
      @Nonnull final Object service) {
    LOG.trace("Checking if resourceResolver needs to be closed for {}",
        service.getClass().getSimpleName());
    if (resourceResolver != null && resourceResolver.isLive()) {
      LOG.info("Closing resourceResolver for {}", service.getClass().getSimpleName());
      resourceResolver.close();
    }
  }

  public static <T> List<T> getAllOsgiServicesOfType(ComponentContext componentContext,
      Class<T> type) {
    List<T> osgiServices = new ArrayList<>();
    ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(), type,
        null);
    serviceTracker.open();
    Object[] services = serviceTracker.getTracked().values().toArray();
    if (services != null && services.length != 0) {
      for (Object service : services) {
        osgiServices.add((T) service);
      }
    } else {
      LOG.error("Unable to build Service list for type {}", type.getName());
    }
    return osgiServices;
  }

  public static <T> List<T> getAllOsgiServicesOfType(ComponentContext componentContext,
      String serviceClassName) {
    List<T> osgiServices = new ArrayList<>();
    ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(),
        serviceClassName, null);
    serviceTracker.open();
    Object[] services = serviceTracker.getTracked().values().toArray();
    if (services != null && services.length != 0) {
      for (Object service : services) {
        osgiServices.add((T) service);
      }
    } else {
      LOG.error("Unable to build Service list for type {}", serviceClassName);
    }
    return osgiServices;
  }
}
