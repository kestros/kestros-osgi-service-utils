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

/**
 * Baseline logic for clearing cache based on a ResourceChange Event.
 */
public abstract class BaseCachePurgeOnResourceChangeEventListener
    implements CachePurgeOnResourceChangeEventListener {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  private ComponentContext componentContext;

  private ResourceResolver serviceResourceResolver;

  protected abstract String getServiceUserName();

  /**
   * Activates event lister service. Opens ResourceResolver for service user.
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
   * Deactivates event lister service. Closes resource resolver if open.
   */
  @Deactivate
  public void deactivate() {
    closeServiceResourceResolver(getServiceResourceResolver(), this);
  }

  @Override
  public void onChange(@Nonnull final List<ResourceChange> list) {
    for (final CacheService cacheService : getCacheServices()) {
      try {
        if (cacheService != null) {
          cacheService.purgeAll(getServiceResourceResolver());
        } else {
          log.error("Failed to purge cache for{}. No cache service detected.",
              getClass().getSimpleName());
        }
      } catch (final CachePurgeException exception) {
        log.error("Failed to create cache purge job. {}", exception.getMessage());
      }
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
