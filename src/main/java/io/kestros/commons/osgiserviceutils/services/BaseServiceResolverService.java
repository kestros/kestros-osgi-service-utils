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

import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getResourceAsBaseResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kestros.commons.structuredslingmodels.BaseResource;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

/**
 * Baseline OSGI Service which when activated, creates a ResourceResolver for a service User.  When
 * deactivated the ResourceResolver is closed.
 */
public abstract class BaseServiceResolverService implements ManagedService {

  private ComponentContext componentContext;

  protected abstract String getServiceUserName();

  /**
   * Activates Cache service. Opens service ResourceResolver, which is used to build cached files.
   *
   * @param ctx ComponentContext.
   */
  @Activate
  public void activate(final ComponentContext ctx) {
    componentContext = ctx;
  }

  /**
   * Deactivates the Service and closes the Service ResourceResolver.
   *
   * @param componentContext ComponentContext.
   */
  @Deactivate
  public void deactivate(ComponentContext componentContext) {
  }

  /**
   * Retrieves a resource resolver as the service user.
   *
   * @return ResourceResolver.
   *
   * @throws LoginException If unable to login as service user.
   */
  public ResourceResolver getServiceResourceResolver() throws LoginException {
    final Map<String, Object> params = Collections.singletonMap(
        ResourceResolverFactory.SUBSERVICE, getServiceUserName());
    if (getResourceResolverFactory() != null) {
      if (getLogger() != null) {
        getLogger().debug("Getting service resource resolver for {}.",
                getServiceUserName().replaceAll("[\r\n]", ""));
      }
      return getResourceResolverFactory().getServiceResourceResolver(params);
    } else {
      throw new LoginException("Resource resolver factory was null");
    }
  }


  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Override
  public void runAdditionalHealthChecks(FormattingResultLog log) {
    try (ResourceResolver resourceResolver = getServiceResourceResolver()) {
      if (resourceResolver.isLive()) {
        log.debug("Service ResourceResolver is live.");
      } else {
        log.critical("Service ResourceResolver is not live.");
      }
      if (getRequiredResourcePaths() != null) {
        for (String path : getRequiredResourcePaths()) {
          try {
            BaseResource requiredResource = getResourceAsBaseResource(path, resourceResolver);
            log.debug(String.format("Found resource at path %s", requiredResource.getPath()));
          } catch (ResourceNotFoundException e) {
            log.critical(String.format("Failed to find resource at path %s", path));
          }
        }
      }
    } catch (LoginException e) {
      log.critical("Unable to get Service ResourceResolver: {}", e.getMessage());
    }

  }

  protected abstract Logger getLogger();

  protected abstract List<String> getRequiredResourcePaths();

  protected abstract ResourceResolverFactory getResourceResolverFactory();

  protected ComponentContext getComponentContext() {
    return this.componentContext;
  }

}
