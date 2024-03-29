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

package io.kestros.commons.osgiserviceutils.services.cache.impl;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;

public class SampleCacheService extends BaseCacheService {

  @Reference
  private JobManager jobManager;

  @Override
  protected void doPurge(ResourceResolver resourceResolver) throws CachePurgeException {
    // Do nothing.
  }

  @Override
  protected void afterCachePurgeComplete(ResourceResolver resourceResolver) {
    this.getDisplayName();
  }

  @Override
  protected long getMinimumTimeBetweenCachePurges() {
    return 1000;
  }

  @Override
  protected String getCacheCreationJobName() {
    return "sample-creation-job-name";
  }

  @Override
  protected JobManager getJobManager() {
    return jobManager;
  }

  @Override
  protected String getServiceUserName() {
    return "sample-cache-service";
  }

  @Override
  public void activate(ComponentContext componentContext) {
  }

  @Override
  public void deactivate(ComponentContext componentContext) {
  }

  @Override
  public String getDisplayName() {
    return "sample cache service";
  }

  @Override
  public void runAdditionalHealthChecks(FormattingResultLog log) {
  }

  @Nonnull
  @Override
  protected Logger getLogger() {
    return null;
  }

  @Override
  protected List<String> getRequiredResourcePaths() {
    return null;
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return null;
  }
}
