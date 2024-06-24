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

package io.kestros.commons.osgiserviceutils.services;

import io.kestros.commons.osgiserviceutils.healthchecks.BaseManagedServiceHealthCheck;
import javax.annotation.Nonnull;
import org.apache.felix.hc.api.FormattingResultLog;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Baseline OSGI Service interface.
 */
public interface ManagedService {

  /**
   * Name of service, to be displayed within a UI.
   *
   * @return Displayed name of cache.
   */
  @Nonnull
  String getDisplayName();

  /**
   * Activates the Service.
   *
   * @param componentContext ComponentContext.
   */
  @Activate
  void activate(@Nonnull final ComponentContext componentContext);

  /**
   * Deactivates the Service.
   *
   * @param componentContext ComponentContext.
   */
  @Deactivate
  void deactivate(@Nonnull final ComponentContext componentContext);

  /**
   * Specific health checks to run on service health check, if using
   * {@link BaseManagedServiceHealthCheck}.
   *
   * @param log HealthCheck log.
   */
  void runAdditionalHealthChecks(@Nonnull final FormattingResultLog log);

}
