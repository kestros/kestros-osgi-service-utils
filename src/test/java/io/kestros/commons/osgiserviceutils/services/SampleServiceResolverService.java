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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true,
           service = SampleServiceResolverService.class)
public class SampleServiceResolverService extends BaseServiceResolverService {

  private static final Logger LOG = LoggerFactory.getLogger(SampleServiceResolverService.class);
  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Override
  protected String getServiceUserName() {
    return "user-name";
  }

  @Nonnull
  @Override
  protected Logger getLogger() {
    return LOG;
  }

  @Override
  protected List<String> getRequiredResourcePaths() {
    return Collections.emptyList();
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }

  @Override
  public String getDisplayName() {
    return "Sample Service Resolver Service";
  }

}
