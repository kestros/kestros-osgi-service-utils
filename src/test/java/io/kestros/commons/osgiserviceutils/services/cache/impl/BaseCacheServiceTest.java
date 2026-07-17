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
    // CONTRACT CHANGE: a request during the cooldown is no longer silently dropped — it records
    // the requester and schedules a deferred purge, so getUserID is consulted for it as well.
    verify(resourceResolver, times(2)).getUserID();
  }

  @Test
  public void testPurgeRequestDuringCooldownIsDeferredNotDropped()
          throws CachePurgeException, InterruptedException, LoginException {
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.purgeAll(resourceResolver);
    verify(baseCacheService, times(1)).doPurge(resourceResolver);
    Date firstPurgeDate = baseCacheService.getLastPurged();

    // Request during the cooldown — previously this was silently dropped, leaving anything
    // written after the first purge stale forever (the package-install staleness bug).
    when(resourceResolver.getUserID()).thenReturn("burst-user");
    baseCacheService.purgeAll(resourceResolver);
    verify(baseCacheService, times(1)).doPurge(resourceResolver);

    // The deferred purge must fire on its own once the cooldown (1000ms) expires.
    Thread.sleep(1500);
    verify(baseCacheService, times(2)).doPurge(resourceResolver);
    assertTrue(baseCacheService.getLastPurged().getTime() > firstPurgeDate.getTime());
    assertEquals("burst-user", baseCacheService.getLastPurgedBy());
  }

  @Test
  public void testPurgeRequestsDuringCooldownCoalesceIntoOneDeferredPurge()
          throws CachePurgeException, InterruptedException, LoginException {
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    baseCacheService.purgeAll(resourceResolver);
    verify(baseCacheService, times(1)).doPurge(resourceResolver);

    baseCacheService.purgeAll(resourceResolver);
    baseCacheService.purgeAll(resourceResolver);
    baseCacheService.purgeAll(resourceResolver);

    Thread.sleep(1500);
    // One immediate purge + exactly one coalesced deferred purge for the whole burst.
    verify(baseCacheService, times(2)).doPurge(resourceResolver);
  }

  @Test
  public void testImmediatePurgeCancelsPendingDeferredPurge()
          throws CachePurgeException, InterruptedException, LoginException {
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    // Deterministic cooldown control: real time is not consulted.
    doReturn(true).when(baseCacheService).isCachePurgeTimeoutExpired();
    baseCacheService.purgeAll(resourceResolver);           // purge #1
    doReturn(false).when(baseCacheService).isCachePurgeTimeoutExpired();
    baseCacheService.purgeAll(resourceResolver);           // deferred armed (~1050ms out)
    doReturn(true).when(baseCacheService).isCachePurgeTimeoutExpired();
    baseCacheService.purgeAll(resourceResolver);           // immediate purge #2 — cancels deferred
    Thread.sleep(1400);                                    // past the deferred task's fire time
    // Exactly two purges: the cancelled deferred task must not produce a third.
    verify(baseCacheService, times(2)).doPurge(resourceResolver);
  }

  @Test
  public void testDeferredPurgeRearmedWhenImmediatePurgeFails()
          throws CachePurgeException, InterruptedException, LoginException {
    doReturn(resourceResolver).when(baseCacheService).getServiceResourceResolver();
    doReturn(true).when(baseCacheService).isCachePurgeTimeoutExpired();
    baseCacheService.purgeAll(resourceResolver);           // purge #1 succeeds
    doReturn(false).when(baseCacheService).isCachePurgeTimeoutExpired();
    baseCacheService.purgeAll(resourceResolver);           // deferred armed

    // Immediate purge cancels the deferred one, then fails.
    doReturn(true).when(baseCacheService).isCachePurgeTimeoutExpired();
    try {
      doThrow(new CachePurgeException("transient failure")).when(baseCacheService).doPurge(any());
    } catch (CachePurgeException e) {
      // stubbing only
    }
    exception = null;
    try {
      baseCacheService.purgeAll(resourceResolver);
    } catch (CachePurgeException e) {
      exception = e;
    }
    assertNotNull(exception);

    // Heal doPurge; the re-armed deferred purge must still fire on its own.
    try {
      org.mockito.Mockito.doCallRealMethod().when(baseCacheService).doPurge(any());
    } catch (CachePurgeException e) {
      // stubbing only
    }
    Thread.sleep(1400);
    // #1 success + #2 failed attempt + #3 re-armed deferred success.
    verify(baseCacheService, times(3)).doPurge(any());
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