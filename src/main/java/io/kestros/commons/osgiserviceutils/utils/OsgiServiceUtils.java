/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

/**
 * Utility methods for OSGI services.
 */
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
   * @throws LoginException ResourceResolver factory failed to login for the given service
   *     UserID.
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

      final Map<String, Object> params = Collections.singletonMap(
          ResourceResolverFactory.SUBSERVICE, serviceName);

      if (resourceResolverFactory != null) {
        resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
        LOG.info("Opened service user {} resourceResolver for service {}.", serviceName,
            service.getClass().getSimpleName());
      } else {
        LOG.warn("Failed to open service user {} resourceResolver for service {}. "
                 + "ResourceResolverFactory was null.", serviceName,
            service.getClass().getSimpleName());
        throw new LoginException();
      }
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
    } catch (final LoginException exception) {
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
   * Retrieves the top ranked service registered to a specified class.
   *
   * @param componentContext Component Context.
   * @param type Service class to retrieve instances of.
   * @param <T> Generic Type.
   * @return The top ranked service registered to a specified class.
   */
  public static <T> T getOsgiServiceOfType(ComponentContext componentContext, Class<T> type) {
    final ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(),
        type.getName(), null);
    serviceTracker.open();
    T service = (T) serviceTracker.getService();
    serviceTracker.close();
    return service;
  }

  /**
   * Retrieves all Services which are registered to the specified service class.
   *
   * @param componentContext componentContext
   * @param type Service class to retrieve instances of.
   * @param <T> Generic type.
   * @return All Services which are registered to the specified service class.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public static <T> List<T> getAllOsgiServicesOfType(final ComponentContext componentContext,
      final Class<T> type) {
    final ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(),
        type, null);
    return getAllOsgiServicesOfType(type.getName(), serviceTracker);
  }

  /**
   * Retrieves all Services which are registered to the specified service class.
   *
   * @param componentContext componentContext
   * @param serviceClassName Service class name to retrieve instances of.
   * @param <T> Generic type.
   * @return All Services which are registered to the specified service class.
   */
  @SuppressWarnings("unchecked")
  @Nonnull
  public static <T> List<T> getAllOsgiServicesOfType(final ComponentContext componentContext,
      final String serviceClassName) {
    final ServiceTracker serviceTracker = new ServiceTracker(componentContext.getBundleContext(),
        serviceClassName, null);

    return getAllOsgiServicesOfType(serviceClassName, serviceTracker);
  }

  @SuppressWarnings("unchecked")
  @Nonnull
  static <T> List<T> getAllOsgiServicesOfType(@Nonnull final String serviceName,
      @Nonnull final ServiceTracker serviceTracker) {
    serviceTracker.open();
    final List<T> osgiServices = new ArrayList<>();
    final Object[] services = serviceTracker.getTracked().values().toArray();
    if (services.length != 0) {
      for (final Object service : services) {
        osgiServices.add((T) service);
      }
    } else {
      LOG.debug("No services found for '{}'.", serviceName);
    }
    serviceTracker.close();
    return osgiServices;
  }
}
