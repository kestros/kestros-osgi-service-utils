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

package io.kestros.commons.osgiserviceutils.services.cache;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import java.io.Serializable;
import java.util.Date;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Manages the managing, building, purging and retrieval of Cached values.
 */
public interface CacheService extends Serializable {

  /**
   * Activates the CacheService.
   *
   * @param componentContext ComponentContext.
   */
  @Activate
  void activate(ComponentContext componentContext);

  /**
   * Deactivates the Cache Service.
   *
   * @param componentContext ComponentContext.
   */
  @Deactivate
  void deactivate(ComponentContext componentContext);

  /**
   * Name of cache, to be displayed within a UI.
   *
   * @return Displayed name of cache.
   */
  String getDisplayName();

  /**
   * Enables cache service to allow caching and value retrieval. Purges caches before enabling.
   *
   * @param resourceResolver ResourceResolver for user performing cache purge.
   * @throws CachePurgeException CacheService failed to purge all cached values.
   */
  void enable(ResourceResolver resourceResolver) throws CachePurgeException;

  /**
   * Disables cache service to prevent caching and value retrieval. Purges caches disabling.
   *
   * @param resourceResolver ResourceResolver for user performing cache purge.
   * @throws CachePurgeException CacheService failed to purge all cached values.
   */
  void disable(ResourceResolver resourceResolver) throws CachePurgeException;

  /**
   * Whether the cache service is enabled.
   *
   * @return Whether the cache service is enabled.
   */
  boolean isLive();

  /**
   * Date the last purgeAll was performed.
   *
   * @return Date the last purgeAll was performed.
   */
  Date getLastPurged();

  /**
   * UserID that performed the last purgeAll.
   *
   * @return UserID that performed the last purgeAll.
   */
  String getLastPurgedBy();

  /**
   * Purges entire cache.
   *
   * @param resourceResolver ResourceResolver for user performing cache purge.
   * @throws CachePurgeException CacheService failed to purge all cached values.
   */
  void purgeAll(ResourceResolver resourceResolver) throws CachePurgeException;

}
