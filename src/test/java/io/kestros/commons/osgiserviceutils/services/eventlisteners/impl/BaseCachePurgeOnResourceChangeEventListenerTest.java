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

package io.kestros.commons.osgiserviceutils.services.eventlisteners.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.osgiserviceutils.services.cache.CacheService;
import io.kestros.commons.osgiserviceutils.services.cache.impl.SampleJcrCacheService;
import java.util.ArrayList;
import java.util.List;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BaseCachePurgeOnResourceChangeEventListenerTest {

  @Rule
  public SlingContext context = new SlingContext();

  private SampleCachePurgeOnResourceChangeEventListener eventListener;

  private List<CacheService> cacheServices = new ArrayList<>();

  private SampleJcrCacheService cacheService;

  private ResourceResolverFactory resourceResolverFactory;

  private ResourceResolver serviceResourceResolver;

  @Before
  public void setUp() throws Exception {
    cacheService = mock(SampleJcrCacheService.class);
    resourceResolverFactory = mock(ResourceResolverFactory.class);
    serviceResourceResolver = mock(ResourceResolver.class);
    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(
        serviceResourceResolver);
  }

  @Test
  public void testGetServiceUserName() {
    eventListener = new SampleCachePurgeOnResourceChangeEventListener();
    assertEquals("service-user", eventListener.getServiceUserName());
  }


  @Test
  public void testActivate() throws LoginException {
    eventListener = spy(new SampleCachePurgeOnResourceChangeEventListener());
    doReturn(resourceResolverFactory).when(eventListener).getResourceResolverFactory();

    assertNull(eventListener.getServiceResourceResolver());

    eventListener.activate(context.componentContext());

    assertNotNull(eventListener.getServiceResourceResolver());
    verify(resourceResolverFactory, times(1)).getServiceResourceResolver(any());
    verify(eventListener, times(1)).getServiceUserName();
  }

  @Test
  public void testActivateWhenServiceResourceResolverIsLive() throws LoginException {
    eventListener = spy(new SampleCachePurgeOnResourceChangeEventListener());
    doReturn(resourceResolverFactory).when(eventListener).getResourceResolverFactory();

    when(eventListener.getServiceResourceResolver()).thenReturn(serviceResourceResolver);
    when(serviceResourceResolver.isLive()).thenReturn(true);

    assertNotNull(eventListener.getServiceResourceResolver());

    eventListener.activate(context.componentContext());

    assertNotNull(eventListener.getServiceResourceResolver());

    verify(resourceResolverFactory, times(0)).getServiceResourceResolver(any());
    verify(eventListener, times(1)).getServiceUserName();
  }

  @Test
  public void testActivateWhenLoginException() throws LoginException {
    eventListener = spy(new SampleCachePurgeOnResourceChangeEventListener());
    doReturn(resourceResolverFactory).when(eventListener).getResourceResolverFactory();
    when(resourceResolverFactory.getServiceResourceResolver(any())).thenThrow(LoginException.class);
    eventListener.activate(context.componentContext());
    assertNull(eventListener.getServiceResourceResolver());
    verify(eventListener, times(1)).getServiceUserName();
  }

  @Test
  public void testDeactivate() {
    eventListener = spy(new SampleCachePurgeOnResourceChangeEventListener());
    doReturn(resourceResolverFactory).when(eventListener).getResourceResolverFactory();
    when(serviceResourceResolver.isLive()).thenReturn(true);

    eventListener.activate(context.componentContext());
    eventListener.deactivate(context.componentContext());

    assertNotNull(eventListener.getServiceResourceResolver());
    verify(serviceResourceResolver, times(1)).close();
  }

  @Test
  public void testOnChange() throws Exception {
    eventListener = spy(new SampleCachePurgeOnResourceChangeEventListener());
    doReturn(resourceResolverFactory).when(eventListener).getResourceResolverFactory();
    cacheServices.add(cacheService);
    doReturn(cacheServices).when(eventListener).getCacheServices();

    List<ResourceChange> changeList = new ArrayList<>();
    ResourceChange resourceChange = mock(ResourceChange.class);
    when(resourceChange.getPath()).thenReturn("/apps/test");

    eventListener.activate(context.componentContext());
    eventListener.onChange(changeList);

    verify(cacheService, times(1)).purgeAll(serviceResourceResolver);
  }

  @Test
  public void testOnChangeWhenCacheServiceIsNull() throws Exception {
    eventListener = new SampleCachePurgeOnResourceChangeEventListener();

    List<ResourceChange> changeList = new ArrayList<>();
    ResourceChange resourceChange = mock(ResourceChange.class);
    when(resourceChange.getPath()).thenReturn("/apps/test");

    eventListener.onChange(changeList);
    verify(cacheService, never()).purgeAll(any());
  }

  @Test
  public void testOnChangeWhenCachePurgeException() throws Exception {
    doThrow(CachePurgeException.class).when(cacheService).purgeAll(any());
    eventListener = spy(new SampleCachePurgeOnResourceChangeEventListener());
    doReturn(resourceResolverFactory).when(eventListener).getResourceResolverFactory();
    cacheServices.add(cacheService);
    doReturn(cacheServices).when(eventListener).getCacheServices();

    List<ResourceChange> changeList = new ArrayList<>();
    ResourceChange resourceChange = mock(ResourceChange.class);
    when(resourceChange.getPath()).thenReturn("/apps/test");

    eventListener.onChange(changeList);
    verify(cacheService, times(1)).purgeAll(any());
  }

}