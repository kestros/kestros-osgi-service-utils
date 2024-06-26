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

package io.kestros.commons.osgiserviceutils.healthchecks;

import io.kestros.commons.osgiserviceutils.services.ManagedService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;

/**
 * Baseline Health check for cache services.
 */
public abstract class BaseManagedServiceHealthCheck implements HealthCheck {

  /**
   * Service to run HealthChecks on.
   *
   * @return Service to run HealthChecks on.
   */
  @Nullable
  public abstract ManagedService getManagedService();

  /**
   * Name of service that is being checked.
   *
   * @return Name of service that is being checked.
   */
  @Nonnull
  public abstract String getServiceName();

  @Nonnull
  @Override
  public Result execute() {
    FormattingResultLog log = new FormattingResultLog();
    log.debug("Starting Health Check for managed service.");
    if (getManagedService() == null) {
      log.critical(String.format("%s is not registered.", getServiceName()));
    } else {
      getManagedService().runAdditionalHealthChecks(log);
      if (log.getAggregateStatus().name().equals(Status.OK.name())) {
        log.info(String.format("%s is registered and running properly.",
                               getManagedService().getDisplayName()));
      }
    }
    return new Result(log);
  }
}
