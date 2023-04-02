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

package io.kestros.commons.osgiserviceutils.utils;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.*;

public class OsgiServiceUtilsTest {

  @Rule
  public SlingContext context = new SlingContext();

  private ResourceResolver resourceResolver;

  private ResourceResolverFactory resourceResolverFactory;

  private Object service;

  @Before
  public void setUp() throws Exception {
    resourceResolverFactory = mock(ResourceResolverFactory.class);
    service = new Object();
  }

  @Test
  public void setOpenServiceResourceResolver() throws LoginException {
    resourceResolver = context.resourceResolver();
    assertNotNull(resourceResolver);

    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);

    assertNotNull(resourceResolver);
    assertTrue(resourceResolver.isLive());
  }

  @Test
  public void setOpenServiceResourceResolverWhenResolverHasBeenClosed() throws LoginException {
    resourceResolver = spy(context.resourceResolver());
    doReturn(false).when(resourceResolver).isLive();

    assertFalse(resourceResolver.isLive());

    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(
        context.resourceResolver());
    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);

    assertNotNull(resourceResolver);
    assertTrue(resourceResolver.isLive());
  }

  @Test
  public void setOpenServiceResourceResolverWhenResourceResolverIsNull() throws LoginException {
    assertNull(resourceResolver);
    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(
        context.resourceResolver());
    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);

    assertNotNull(resourceResolver);
    assertTrue(resourceResolver.isLive());
  }

  @Test(expected = LoginException.class)
  public void setOpenServiceResourceResolverWhenLoginException() throws LoginException {
    assertNull(resourceResolver);

    when(resourceResolverFactory.getServiceResourceResolver(any())).thenThrow(LoginException.class);

    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);
  }

  @Test
  public void closeServiceResourceResolver() {
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.isLive()).thenReturn(true);

    OsgiServiceUtils.closeServiceResourceResolver(resourceResolver, service);

    verify(resourceResolver, times(1)).close();
  }

  @Test
  public void closeServiceResourceResolverWhenResolverIsNull() {
    OsgiServiceUtils.closeServiceResourceResolver(resourceResolver, service);

    assertNull(resourceResolver);
  }

  @Test
  public void closeServiceResourceResolverWhenResourceResolverIsNotLive() {
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.isLive()).thenReturn(false);

    OsgiServiceUtils.closeServiceResourceResolver(resourceResolver, service);

    verify(resourceResolver, never()).close();
    assertNotNull(resourceResolver);
  }

  @Test
  public void testGetOsgiServicesOfType() {
    ComponentContext mockContext = mock(ComponentContext.class);
    BundleContext bundleContext = mock(BundleContext.class);
    ServiceTracker mockServiceTracker = mock(ServiceTracker.class);
    when(mockContext.getBundleContext()).thenReturn(bundleContext);
    assertNotNull(OsgiServiceUtils.getAllOsgiServicesOfType(context.componentContext(), String.class));
  }

  @Test
  public void testGetOsgiServicesOfTypeWhenStringClassName() {
    ComponentContext mockContext = mock(ComponentContext.class);
    BundleContext bundleContext = mock(BundleContext.class);
    ServiceTracker mockServiceTracker = mock(ServiceTracker.class);
    when(mockContext.getBundleContext()).thenReturn(bundleContext);
    assertNotNull(OsgiServiceUtils.getAllOsgiServicesOfType(context.componentContext(), "String"));
  }

  @Test
  public void testGetOsgiServicesOfTypeWhenServiceTracker() {
    ServiceTracker mockServiceTracker = mock(ServiceTracker.class);
    SortedMap sortedMap = mock(SortedMap.class);
    List<Object> classes = new ArrayList<>();
    classes.add(String.class);
    when(sortedMap.values()).thenReturn(classes);
    when(mockServiceTracker.getTracked()).thenReturn(sortedMap);

    assertEquals(1, OsgiServiceUtils.getAllOsgiServicesOfType("String", mockServiceTracker).size());

  }

}