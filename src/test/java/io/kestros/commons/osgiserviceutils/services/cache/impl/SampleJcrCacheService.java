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

import java.util.ArrayList;
import java.util.List;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Reference;

public class SampleJcrCacheService extends JcrFileCacheService {

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Reference
  private JobManager jobManager;

  @Override
  public String getServiceCacheRootPath() {
    return "/var/cache/test";
  }

  @Override
  protected String getServiceUserName() {
    return "test-jcr-cache-service-user";
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }

  @Override
  protected List<String> getRequiredResourcePaths() {
    List<String> pathList = new ArrayList<>();
    pathList.add("/content");
    pathList.add("/libs");
    return pathList;
  }

  @Override
  public String getDisplayName() {
    return "Sample Jcr Cache Service";
  }

  @Override
  public void runAdditionalHealthChecks(FormattingResultLog log) {

  }

  @Override
  public JobManager getJobManager() {
    return jobManager;
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
  public String getCacheCreationJobName() {
    return "sample-creation";
  }
}
