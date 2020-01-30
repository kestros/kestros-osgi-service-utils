package io.kestros.commons.osgiserviceutils.utils;

import io.kestros.commons.osgiserviceutils.exceptions.OsgiServiceTrackingException;
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

  /**
   * Retrieves all Services which are registered to the specified service class.
   *
   * @param componentContext componentContext
   * @param type Service class to retrieve instances of.
   * @param <T> Generic type.
   * @return All Services which are registered to the specified service class.
   * @throws OsgiServiceTrackingException Failed to build service tracker.
   */
  public static <T> List<T> getAllOsgiServicesOfType(ComponentContext componentContext,
      Class<T> type) throws OsgiServiceTrackingException {
    ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(), type,
        null);
    return getAllOsgiServicesOfType(type.getName(), serviceTracker);
  }

  /**
   * Retrieves all Services which are registered to the specified service class.
   *
   * @param componentContext componentContext
   * @param serviceClassName Service class name to retrieve instances of.
   * @param <T> Generic type.
   * @return All Services which are registered to the specified service class.
   * @throws OsgiServiceTrackingException Failed to build service tracker.
   */
  public static <T> List<T> getAllOsgiServicesOfType(ComponentContext componentContext,
      String serviceClassName) throws OsgiServiceTrackingException {
    ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(),
        serviceClassName, null);

    return getAllOsgiServicesOfType(serviceClassName, serviceTracker);
  }

  private static <T> List<T> getAllOsgiServicesOfType(String serviceName,
      ServiceTracker serviceTracker) throws OsgiServiceTrackingException {
    serviceTracker.open();
    List<T> osgiServices = new ArrayList<>();
    Object[] services = serviceTracker.getTracked().values().toArray();
    if (services != null) {
      if (services.length != 0) {
        for (Object service : services) {
          osgiServices.add((T) service);
        }
      } else {
        LOG.debug("No services found for '{}'.", serviceName);
      }
    } else {
      LOG.warn("Failed to build service tracker while attempting to track '{}'.", serviceName);
      throw new OsgiServiceTrackingException();
    }
    return osgiServices;
  }
}
