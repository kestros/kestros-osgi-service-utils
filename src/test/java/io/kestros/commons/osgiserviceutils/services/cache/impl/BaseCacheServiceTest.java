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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import java.util.Date;
import java.util.HashMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BaseCacheServiceTest {

  @Rule
  public SlingContext context = new SlingContext();

  private SampleCacheService baseCacheService;

  private ResourceResolver resourceResolver;

  private JobManager jobManager;

  private Exception exception;

  @Before
  public void setUp() throws Exception {
    context.addModelsForPackage("io.kestros");
    baseCacheService = spy(new SampleCacheService());

    resourceResolver = mock(ResourceResolver.class);
    jobManager = mock(JobManager.class);

    context.registerService(JobManager.class, jobManager);

    when(resourceResolver.getUserID()).thenReturn("test-user");
    doReturn(jobManager).when(baseCacheService).getJobManager();
    when(resourceResolver.isLive()).thenReturn(true);
  }

  @Test
  public void testPurgeAll() throws CachePurgeException, InterruptedException, LoginException {
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.purgeAll(resourceResolver);

    verify(baseCacheService, times(1)).doPurge(resourceResolver);
    Date firstPurgeDate = baseCacheService.getLastPurged();
    assertNotNull(baseCacheService.getLastPurged());
    assertNotNull(baseCacheService.getLastPurgedBy());
    assertEquals(Date.class, baseCacheService.getLastPurged().getClass());
    assertTrue(baseCacheService.getLastPurged().getTime() > 0);
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
  }

  @Test
  public void testPurgeAllWhenResourceResolverIsNotLive() throws CachePurgeException,
          InterruptedException, LoginException {
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.isLive()).thenReturn(false);
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    exception = null;
    try {
      baseCacheService.purgeAll(resourceResolver);
    } catch (CachePurgeException e) {
      exception = e;
    }
    assertNotNull(exception);
    assertEquals(
            "Failed to purge cache sample cache service. Resource Resolver was either null, or "
                    + "already closed.",
            exception.getMessage());

    verify(baseCacheService, times(0)).doPurge(resourceResolver);
    Date firstPurgeDate = baseCacheService.getLastPurged();
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
  }

  @Test
  public void testPurgeAllWhenMultipleAttempts()
          throws CachePurgeException, InterruptedException, LoginException {
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.purgeAll(resourceResolver);

    verify(baseCacheService, times(1)).doPurge(resourceResolver);
    Date firstPurgeDate = baseCacheService.getLastPurged();
    assertNotNull(baseCacheService.getLastPurged());
    assertNotNull(baseCacheService.getLastPurgedBy());
    assertEquals(Date.class, baseCacheService.getLastPurged().getClass());
    assertTrue(baseCacheService.getLastPurged().getTime() > 0);
    assertEquals("test-user", baseCacheService.getLastPurgedBy());

    when(resourceResolver.getUserID()).thenReturn("test-user-2");
    baseCacheService.purgeAll(resourceResolver);
    verify(baseCacheService, times(1)).doPurge(resourceResolver);
    assertEquals(baseCacheService.getLastPurged().getTime(), firstPurgeDate.getTime());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    verify(resourceResolver, times(1)).getUserID();
  }

  @Test
  public void testPurgeAllWhenMultipleAttemptsAfterExpiration()
          throws CachePurgeException, InterruptedException, LoginException {
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.purgeAll(resourceResolver);

    verify(baseCacheService, times(1)).doPurge(resourceResolver);
    Date firstPurgeDate = baseCacheService.getLastPurged();
    assertNotNull(baseCacheService.getLastPurged());
    assertNotNull(baseCacheService.getLastPurgedBy());
    assertEquals(Date.class, baseCacheService.getLastPurged().getClass());
    assertTrue(baseCacheService.getLastPurged().getTime() > 0);
    assertEquals("test-user", baseCacheService.getLastPurgedBy());

    Thread.sleep(1001);

    when(resourceResolver.getUserID()).thenReturn("test-user-2");
    baseCacheService.purgeAll(resourceResolver);
    verify(baseCacheService, times(2)).doPurge(resourceResolver);
    assertTrue(baseCacheService.getLastPurged().getTime() > firstPurgeDate.getTime());
    assertEquals("test-user-2", baseCacheService.getLastPurgedBy());
    verify(resourceResolver, times(2)).getUserID();
  }

  @Test
  public void testPurgeAllWhenWhenCachePurgeException() throws LoginException {
    try {
      doThrow(new CachePurgeException("cache purge exception")).when(baseCacheService).doPurge(
              any());
    } catch (CachePurgeException e) {
    }
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    try {
      baseCacheService.purgeAll(resourceResolver);
    } catch (CachePurgeException e) {
      exception = e;
    }
    assertEquals("cache purge exception", exception.getMessage());
  }

  @Test
  public void testEnable() throws CachePurgeException, LoginException {
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.disable(resourceResolver);
    assertNotNull(baseCacheService.getLastPurged());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    baseCacheService.enable(resourceResolver);
    assertNotNull(baseCacheService.getLastPurged());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    assertTrue(baseCacheService.isLive());
  }

  @Test
  public void testDisable() throws CachePurgeException, LoginException {
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.enable(resourceResolver);
    assertNotNull(baseCacheService.getLastPurged());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    baseCacheService.disable(resourceResolver);
    assertNotNull(baseCacheService.getLastPurged());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    assertFalse(baseCacheService.isLive());
  }

  @Test
  public void testIsLive() {
    assertTrue(baseCacheService.isLive());
  }

  @Test
  public void testGetServiceClassName() {
    assertEquals("org.mockito.internal.creation.bytebuddy.MockAccess",
            baseCacheService.getServiceClassName());
  }

  @Test
  public void testAddCacheCreationJob() {
    baseCacheService.addCacheCreationJob(new HashMap<>());
    verify(jobManager, times(1)).addJob("sample-creation-job-name", new HashMap<>());
  }

  @Test
  public void testGetJobManager() {
    baseCacheService.activate(context.componentContext());
    assertEquals(jobManager, baseCacheService.getJobManager());
  }
}