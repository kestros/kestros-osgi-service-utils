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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    verify(resourceResolverFactory, times(1)).getServiceResourceResolver(any());
    verify(serviceResolverService, times(1)).getServiceUserName();
    verify(serviceResolverService, times(1)).getServiceResourceResolver();
  }

  @Test
  public void testDeactivate() {
    serviceResolverService.activate(context.componentContext());
    serviceResolverService.deactivate(context.componentContext());
    verify(resourceResolver, times(1)).close();
  }

  @Test
  public void testGetResourceResolverFactory() {
    assertEquals(resourceResolverFactory, serviceResolverService.getResourceResolverFactory());
  }

  @Test
  public void testGetServiceResourceResolver() {
    serviceResolverService.activate(context.componentContext());
    assertEquals(resourceResolver, serviceResolverService.getServiceResourceResolver());
  }
}