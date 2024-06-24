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

package io.kestros.commons.osgiserviceutils.services.eventlisteners;

import io.kestros.commons.osgiserviceutils.services.ManagedService;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChangeListener;

/**
 * Listens for ResourceChanges and purges the specified CacheServices when a change is detected.
 */
public interface CachePurgeOnResourceChangeEventListener
    extends ResourceChangeListener, ManagedService {

  /**
   * {@link CacheService} implementations to purge.
   *
   * @param <T> Extends {@link CacheService}
   * @return {@link CacheService} implementations to purge.
   */
  @Nonnull
  <T extends CacheService> List<T> getCacheServices();

  /**
   * ResourceResolverFactory used to get ServiceResourceResolver.
   *
   * @return ResourceResolverFactory used to get ServiceResourceResolver.
   */
  @Nullable
  ResourceResolverFactory getResourceResolverFactory();

}
