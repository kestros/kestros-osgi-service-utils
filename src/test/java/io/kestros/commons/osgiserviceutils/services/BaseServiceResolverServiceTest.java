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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.Result;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BaseServiceResolverServiceTest {

  @Rule
  public SlingContext context = new SlingContext();

  private SampleServiceResolverService serviceResolverService;

  private ResourceResolverFactory resourceResolverFactory;

  private ResourceResolver resourceResolver;

  @Before
  public void setUp() throws Exception {
    context.addModelsForPackage("io.kestros");

    resourceResolverFactory = mock(ResourceResolverFactory.class);
    resourceResolver = spy(context.resourceResolver());

    serviceResolverService = spy(new SampleServiceResolverService());

    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(resourceResolver);
    doReturn(resourceResolverFactory).when(serviceResolverService).getResourceResolverFactory();
  }

  @Test
  public void testGetServiceUserName() {
    assertEquals("user-name", serviceResolverService.getServiceUserName());
  }

  @Test
  public void testActivate() throws LoginException {
    serviceResolverService.activate(context.componentContext());
  }

  @Test
  public void testDeactivate() {
    serviceResolverService.activate(context.componentContext());
    serviceResolverService.deactivate(context.componentContext());
  }

  @Test
  public void testGetResourceResolverFactory() {
    assertEquals(resourceResolverFactory, serviceResolverService.getResourceResolverFactory());
  }

  @Test
  public void testGetServiceResourceResolver() throws LoginException {
    serviceResolverService.activate(context.componentContext());
    assertEquals(resourceResolver, serviceResolverService.getServiceResourceResolver());
  }

  @Test
  public void testRunAdditionalHealthChecks() {
    FormattingResultLog log = new FormattingResultLog();
    serviceResolverService.runAdditionalHealthChecks(log);
    assertEquals(Result.Status.OK, log.getAggregateStatus());
  }

  @Test
  public void testRunAdditionalHealthChecksWhenResolverIsNotLive() throws LoginException {
    ResourceResolver mockResourceResolver = mock(ResourceResolver.class);
    when(mockResourceResolver.isLive()).thenReturn(false);
    FormattingResultLog log = new FormattingResultLog();
    doReturn(mockResourceResolver).when(serviceResolverService).getServiceResourceResolver();

    serviceResolverService.runAdditionalHealthChecks(log);
    assertEquals(Result.Status.CRITICAL, log.getAggregateStatus());
  }

  @Test
  public void testRunAdditionalHealthChecksWhenLoginException() throws LoginException {
    doThrow(new LoginException("Test Exception")).when(serviceResolverService)
            .getServiceResourceResolver();
    FormattingResultLog log = new FormattingResultLog();
    serviceResolverService.runAdditionalHealthChecks(log);
    assertEquals(Result.Status.CRITICAL, log.getAggregateStatus());
  }

  @Test
  public void testRunAdditionalHealthChecksWhenMissingRequiredResource() {
    doReturn(Arrays.asList("/content")).when(serviceResolverService).getRequiredResourcePaths();
    FormattingResultLog log = new FormattingResultLog();
    serviceResolverService.runAdditionalHealthChecks(log);
    assertEquals(Result.Status.CRITICAL, log.getAggregateStatus());
  }
}