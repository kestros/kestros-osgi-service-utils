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

import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.closeServiceResourceResolver;
import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.getOpenServiceResourceResolverOrNullAndLogExceptions;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getResourceAsBaseResource;

import io.kestros.commons.structuredslingmodels.BaseResource;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Baseline OSGI Service which when activated, creates a ResourceResolver for a service User.  When
 * deactivated the ResourceResolver is closed.
 */
public abstract class BaseServiceResolverService implements ManagedService {

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
   *
   * @param componentContext ComponentContext.
   */
  @Deactivate
  public void deactivate(ComponentContext componentContext) {
    closeServiceResourceResolver(getServiceResourceResolver(), this);
  }

  protected ResourceResolver getNewServiceResourceResolver() {
    return getOpenServiceResourceResolverOrNullAndLogExceptions(getServiceUserName(),
        getServiceResourceResolver(), getResourceResolverFactory(), this);
  }

  @Override
  public void runAdditionalHealthChecks(FormattingResultLog log) {
    if (getServiceResourceResolver() == null) {
      log.critical("Service ResourceResolver is null.");
    } else if (!getServiceResourceResolver().isLive()) {
      log.critical("Service ResourceResolver has closed.");
    }
    if (getRequiredResourcePaths() != null) {
      for (String path : getRequiredResourcePaths()) {
        try {
          BaseResource requiredResource = getResourceAsBaseResource(path,
              getServiceResourceResolver());
          log.debug(String.format("Found resource at path %s", requiredResource.getPath()));
        } catch (ResourceNotFoundException e) {
          log.critical(String.format("Failed to find resource at path %s", path));
        }
      }
    } else {
      log.warn(
          "Required resources paths was null. Return Collections.emptyList() if no resource paths"
          + " are required.");
    }
  }

  protected abstract List<String> getRequiredResourcePaths();

  protected abstract ResourceResolverFactory getResourceResolverFactory();

  protected ComponentContext getComponentContext() {
    return this.componentContext;
  }

  @Nullable
  protected ResourceResolver getServiceResourceResolver() {
    return this.serviceResourceResolver;
  }

}
