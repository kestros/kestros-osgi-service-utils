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

import io.kestros.commons.osgiserviceutils.services.exceptions.CachePurgeException;
import java.util.Date;
import java.util.HashMap;
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
  }

  @Test
  public void testPurgeAll() throws CachePurgeException, InterruptedException {
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
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
  public void testPurgeAllWhenMultipleAttempts() throws CachePurgeException, InterruptedException {
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
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
      throws CachePurgeException, InterruptedException {
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
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
  public void testPurgeAllWhenWhenCachePurgeException() {
    try {
      doThrow(new CachePurgeException("cache purge exception")).when(baseCacheService).doPurge(
          any());
    } catch (CachePurgeException e) {
    }
    assertNull(baseCacheService.getLastPurged());
    assertNull(baseCacheService.getLastPurgedBy());
    try {
      baseCacheService.purgeAll(resourceResolver);
    } catch (CachePurgeException e) {
      exception = e;
    }
    assertEquals("cache purge exception", exception.getMessage());
  }

  @Test
  public void testEnable() throws CachePurgeException {
    baseCacheService.disable(resourceResolver);
    assertNotNull(baseCacheService.getLastPurged());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    baseCacheService.enable(resourceResolver);
    assertNotNull(baseCacheService.getLastPurged());
    assertEquals("test-user", baseCacheService.getLastPurgedBy());
    assertTrue(baseCacheService.isLive());
  }

  @Test
  public void testDisable() throws CachePurgeException {
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
    assertFalse(baseCacheService.isLive());
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
    baseCacheService.activate();
    assertEquals(jobManager, baseCacheService.getJobManager());
  }
}