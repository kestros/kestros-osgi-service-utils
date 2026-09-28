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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class BaseCacheServiceDebounceTest {

  private SampleCacheService cacheService;
  private ResourceResolver resourceResolver;
  private Scheduler scheduler;
  private ArgumentCaptor<Object> jobCaptor;

  @Before
  public void setUp() throws Exception {
    cacheService = spy(new SampleCacheService());
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.getUserID()).thenReturn("burst-user");
    when(resourceResolver.isLive()).thenReturn(true);
    doReturn(resourceResolver).when(cacheService).getServiceResourceResolver();

    scheduler = mock(Scheduler.class);
    ScheduleOptions options = mock(ScheduleOptions.class);
    when(scheduler.AT(any(Date.class))).thenReturn(options);
    when(options.name(anyString())).thenReturn(options);
    when(options.canRunConcurrently(anyBoolean())).thenReturn(options);
    jobCaptor = ArgumentCaptor.forClass(Object.class);
    when(scheduler.schedule(jobCaptor.capture(), eq(options))).thenReturn(true);
    doReturn(scheduler).when(cacheService).getScheduler();
  }

  @Test
  public void testBurstOfRequestsEndsInExactlyOnePurge() throws Exception {
    cacheService.purgeAll(resourceResolver);
    cacheService.purgeAll(resourceResolver);
    cacheService.purgeAll(resourceResolver);

    verify(cacheService, never()).doPurge(any());
    String jobName = cacheService.getPurgeJobName();
    verify(scheduler, times(3)).unschedule(jobName);
    verify(scheduler, times(3)).schedule(any(), any(ScheduleOptions.class));

    ((Runnable) jobCaptor.getValue()).run();

    verify(cacheService, times(1)).doPurge(resourceResolver);
    assertEquals("burst-user", cacheService.getLastPurgedBy());
  }

  @Test
  public void testScheduledPurgeWaitsOutTheCooldown() throws Exception {
    cacheService.purgeAll(resourceResolver);
    ((Runnable) jobCaptor.getValue()).run();
    verify(cacheService, times(1)).doPurge(resourceResolver);

    cacheService.purgeAll(resourceResolver);
    ((Runnable) jobCaptor.getValue()).run();

    // Inside the 1000ms cooldown: rescheduled, not purged.
    verify(cacheService, times(1)).doPurge(resourceResolver);
    verify(scheduler, times(3)).schedule(any(), any(ScheduleOptions.class));
  }

  @Test
  public void testRefusedJobPurgesImmediately() throws Exception {
    when(scheduler.schedule(any(), any(ScheduleOptions.class))).thenReturn(false);

    cacheService.purgeAll(resourceResolver);

    verify(cacheService, times(1)).doPurge(resourceResolver);
  }
}
