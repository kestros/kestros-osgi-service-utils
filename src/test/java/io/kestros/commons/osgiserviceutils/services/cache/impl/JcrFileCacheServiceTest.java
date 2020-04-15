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

import static io.kestros.commons.osgiserviceutils.SampleFileType.SAMPLE_FILE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.kestros.commons.osgiserviceutils.SampleFile;
import io.kestros.commons.osgiserviceutils.exceptions.CacheBuilderException;
import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.structuredslingmodels.exceptions.InvalidResourceTypeException;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import java.util.Collections;
import java.util.Map;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JcrFileCacheServiceTest {

  @Rule
  public SlingContext context = new SlingContext();

  private SampleJcrCacheService jcrFileCacheService;

  private ResourceResolverFactory resourceResolverFactory;

  private ResourceResolver resourceResolver;

  private Exception exception;

  @Before
  public void setUp() throws Exception {
    context.addModelsForPackage("io.kestros");
    resourceResolverFactory = mock(ResourceResolverFactory.class);

    jcrFileCacheService = spy(new SampleJcrCacheService());

    resourceResolver = spy(context.resourceResolver());
    doReturn(resourceResolverFactory).when(jcrFileCacheService).getResourceResolverFactory();
    when(resourceResolver.getUserID()).thenReturn("test-jcr-cache-service-user");
    Map<String, Object> params = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
        "test-jcr-cache-service-user");
    when(resourceResolverFactory.getServiceResourceResolver(params)).thenReturn(resourceResolver);

    context.create().resource("/var/cache/test");
  }

  @Test
  public void testActivate() throws LoginException {
    assertNull(jcrFileCacheService.getServiceResourceResolver());
    jcrFileCacheService.activate();
    assertNotNull(jcrFileCacheService.getServiceResourceResolver());
    assertEquals("test-jcr-cache-service-user",
        jcrFileCacheService.getServiceResourceResolver().getUserID());
    verify(resourceResolverFactory, times(1)).getServiceResourceResolver(any());
    verify(jcrFileCacheService, times(4)).getServiceResourceResolver();
  }

  @Test
  public void testActivateWhenLoginException()
      throws LoginException, CacheBuilderException, ResourceNotFoundException,
             InvalidResourceTypeException {
    assertNull(jcrFileCacheService.getServiceResourceResolver());
    when(resourceResolverFactory.getServiceResourceResolver(any())).thenThrow(LoginException.class);

    jcrFileCacheService.activate();
    assertNull(jcrFileCacheService.getServiceResourceResolver());
  }

  @Test
  public void testDeactivate() {
    jcrFileCacheService.activate();
    assertNotNull(jcrFileCacheService.getServiceResourceResolver());

    jcrFileCacheService.deactivate();
    verify(resourceResolver, times(1)).close();
  }

  @Test
  public void getCacheRootPath() {
    assertEquals("/var/cache/test", jcrFileCacheService.getServiceCacheRootPath());
  }

  @Test
  public void getCachedFile()
      throws ResourceNotFoundException, InvalidResourceTypeException, CacheBuilderException,
             PersistenceException {
    jcrFileCacheService.activate();
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
        SAMPLE_FILE_TYPE);

    assertEquals("/var/cache/test/resource/new-cache-file",
        jcrFileCacheService.getCachedFile("/resource/new-cache-file", SampleFile.class).getPath());
    verify(resourceResolver, times(1)).commit();
  }

  @Test
  public void getCachedFileWhenServiceResourceResolverIsNotLive() {
    jcrFileCacheService.activate();
    when(resourceResolver.isLive()).thenReturn(false);
    try {
      jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
          SAMPLE_FILE_TYPE);
    } catch (CacheBuilderException e) {
      exception = e;
    }
    //    assertEquals("", exception.getMessage());
    assertTrue(exception.getMessage().startsWith("SampleJcrCacheService"));
    assertTrue(exception.getMessage().endsWith(
        " failed to create jcr cache file for /resource/new-cache-file due to closed service "
        + "resourceResolver."));
  }

  @Test
  public void getCachedFileWhenRelativePathDoesNotStartWithSlash() throws PersistenceException {
    jcrFileCacheService.activate();
    try {
      jcrFileCacheService.createCacheFile("Cache Content", "resource/new-cache-file",
          SAMPLE_FILE_TYPE);
    } catch (CacheBuilderException e) {
      exception = e;
    }
    //    assertEquals("", exception.getMessage());
    assertTrue(exception.getMessage().startsWith("SampleJcrCacheService"));
    assertTrue(exception.getMessage().endsWith(
        "failed to create jcr cache file for 'resource/new-cache-file'. Unable to adapt "
        + "'/var/cache/testresource': Resource not found."));
    try {
      assertEquals("/var/cache/test/resource/new-cache-file",
          jcrFileCacheService.getCachedFile("/resource/new-cache-file",
              SampleFile.class).getPath());
    } catch (ResourceNotFoundException e) {
      exception = e;
    } catch (InvalidResourceTypeException e) {
    }
    assertEquals("Unable to adapt '/var/cache/test/resource/new-cache-file': Resource not found.",
        exception.getMessage());
    verify(resourceResolver, times(0)).commit();
  }

  @Test
  public void getCachedFileWhenCreateResourcesFromPathFails()
      throws ResourceNotFoundException, PersistenceException {
    jcrFileCacheService.activate();
    Exception exception = null;
    doThrow(new ResourceNotFoundException("resource")).when(
        jcrFileCacheService).createResourcesFromPath(any(), any());
    try {
      jcrFileCacheService.createCacheFile("Cache Content", "resource/new-cache-file",
          SAMPLE_FILE_TYPE);
    } catch (CacheBuilderException e) {
      exception = e;
    }
    assertEquals(CacheBuilderException.class, exception.getClass());
    assertTrue(exception.getMessage().startsWith("SampleJcrCacheService"));
    assertTrue(exception.getMessage().endsWith(
        " was unable to create jcr file cache for 'resource/new-cache-file'. Cache root resource "
        + "not found. Unable to adapt 'resource': Resource not found."));
  }

  @Test
  public void getCachedFileWhenCacheRootResourceNotFound() throws PersistenceException {
    context.resourceResolver().delete(context.resourceResolver().getResource("/var/cache/test"));
    jcrFileCacheService.activate();
    try {
      jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
          SAMPLE_FILE_TYPE);
    } catch (CacheBuilderException e) {
      exception = e;
    }
    assertEquals(CacheBuilderException.class, exception.getClass());
    //    assertEquals("", exception.getMessage());
    assertTrue(exception.getMessage().startsWith("SampleJcrCacheService"));
    assertTrue(exception.getMessage().endsWith(
        " was unable to create jcr file cache for '/resource/new-cache-file'. Cache root resource"
        + " not found. Unable to adapt '/var/cache/test': Resource not found."));
  }

  @Test
  public void getCachedFileWhenMultipleCachedFiles()
      throws ResourceNotFoundException, InvalidResourceTypeException, CacheBuilderException {
    jcrFileCacheService.activate();
    jcrFileCacheService.createCacheFile("Cache Content-1", "/resource/new-cache-file-1",
        SAMPLE_FILE_TYPE);
    jcrFileCacheService.createCacheFile("Cache Content-2", "/resource/new-cache-file-2",
        SAMPLE_FILE_TYPE);

    assertEquals("/var/cache/test/resource/new-cache-file-1",
        jcrFileCacheService.getCachedFile("/resource/new-cache-file-1",
            SampleFile.class).getPath());

    assertEquals("/var/cache/test/resource/new-cache-file-2",
        jcrFileCacheService.getCachedFile("/resource/new-cache-file-2",
            SampleFile.class).getPath());
  }

  @Test
  public void getParentPathFromPath() {
    assertEquals("/var/cache/test",
        jcrFileCacheService.getParentPathFromPath("/var/cache/test/resource"));
  }


  @Test
  public void testIsFileCached() throws CacheBuilderException {
    jcrFileCacheService.activate();

    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
        SAMPLE_FILE_TYPE);

    assertTrue("/var/cache/test/resource/new-cache-file",
        jcrFileCacheService.isFileCached("/resource/new-cache-file"));
  }

  @Test
  public void testDoPurge()
      throws CachePurgeException, CacheBuilderException, PersistenceException {
    jcrFileCacheService.activate();
    verify(resourceResolver, never()).commit();
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
        SAMPLE_FILE_TYPE);
    verify(resourceResolver, times(1)).commit();
    jcrFileCacheService.doPurge(resourceResolver);

    assertFalse(jcrFileCacheService.isFileCached("/resource/new-cache-file"));

    verify(resourceResolver, times(2)).commit();
  }

  @Test
  public void testDoPurgeWhenDeletePersistenceException()
      throws CacheBuilderException, PersistenceException, CachePurgeException {
    jcrFileCacheService.activate();
    verify(resourceResolver, never()).commit();
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
        SAMPLE_FILE_TYPE);

    verify(resourceResolver, times(1)).commit();
    doThrow(new PersistenceException("persistence error")).when(resourceResolver).delete(any());
    jcrFileCacheService.doPurge(resourceResolver);

    assertTrue(jcrFileCacheService.isFileCached("/resource/new-cache-file"));
    verify(resourceResolver, times(1)).commit();
  }

  @Test
  public void testDoPurgeWhenServiceCacheRootResourceIsMissing() throws PersistenceException {
    jcrFileCacheService.activate();
    verify(resourceResolver, never()).commit();
    Exception exception = null;

    Resource cacheRootResource = context.resourceResolver().getResource(
        jcrFileCacheService.getServiceCacheRootPath());
    context.resourceResolver().delete(cacheRootResource);
    try {
      doThrow(new PersistenceException("persistence error")).when(resourceResolver).delete(any());
      jcrFileCacheService.doPurge(resourceResolver);
    } catch (PersistenceException e) {
    } catch (CachePurgeException e) {
      exception = e;
    }
    assertEquals(CachePurgeException.class, exception.getClass());
    assertTrue(exception.getMessage().startsWith("Failed to purge cache SampleJcrCacheService"));
    assertTrue(exception.getMessage().endsWith("Cache root resource /var/cache/test not found."));
    verify(resourceResolver, never()).delete(any());
    verify(resourceResolver, never()).commit();
  }

  @Test
  public void testDoPurgeWhenMultipleFiles()
      throws CachePurgeException, CacheBuilderException, PersistenceException {
    jcrFileCacheService.activate();
    verify(resourceResolver, never()).commit();
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
        SAMPLE_FILE_TYPE);
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file-2",
        SAMPLE_FILE_TYPE);
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file-3",
        SAMPLE_FILE_TYPE);
    verify(resourceResolver, times(3)).commit();
    jcrFileCacheService.doPurge(resourceResolver);

    assertFalse(jcrFileCacheService.isFileCached("/resource/new-cache-file"));
    verify(resourceResolver, times(1)).delete(any());
    verify(resourceResolver, times(4)).commit();
  }

  @Test
  public void testDoPurgeWhenMultipleDirectChildren()
      throws CachePurgeException, CacheBuilderException, PersistenceException {
    jcrFileCacheService.activate();
    verify(resourceResolver, never()).commit();
    jcrFileCacheService.createCacheFile("Cache Content", "/new-cache-file", SAMPLE_FILE_TYPE);
    jcrFileCacheService.createCacheFile("Cache Content", "/new-cache-file-2", SAMPLE_FILE_TYPE);
    jcrFileCacheService.createCacheFile("Cache Content", "/new-cache-file-3", SAMPLE_FILE_TYPE);
    verify(resourceResolver, times(3)).commit();
    jcrFileCacheService.doPurge(resourceResolver);

    assertFalse("/var/cache/test/resource/new-cache-file",
        jcrFileCacheService.isFileCached("/resource/new-cache-file"));
    verify(resourceResolver, times(3)).delete(any());
    verify(resourceResolver, times(6)).commit();
  }

  @Test
  public void testDoPurgeWhenCacheRootResourceDoesNotExist()
      throws CacheBuilderException, PersistenceException {
    jcrFileCacheService.activate();
    jcrFileCacheService.createCacheFile("Cache Content", "/resource/new-cache-file",
        SAMPLE_FILE_TYPE);
    Resource cacheRootResource = context.resourceResolver().getResource(
        jcrFileCacheService.getServiceCacheRootPath());
    context.resourceResolver().delete(cacheRootResource);
    try {
      jcrFileCacheService.doPurge(resourceResolver);
    } catch (CachePurgeException e) {
      exception = e;
    }
    assertTrue(exception.getMessage().startsWith("Failed to purge cache SampleJcrCacheService"));
    assertTrue(exception.getMessage().endsWith(". Cache root resource /var/cache/test not found."));

    verify(resourceResolver, never()).delete(any());
    verify(resourceResolver, times(1)).commit();
  }

  @Test
  public void createResourcesFromPath() throws ResourceNotFoundException, PersistenceException {
    assertNotNull(resourceResolver.getResource("/var/cache/test"));
    assertNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));

    jcrFileCacheService.createResourcesFromPath("/resource/new-cache-file", resourceResolver);

    assertNotNull(resourceResolver.getResource("/var/cache/test/resource"));
    assertEquals("sling:Folder", resourceResolver.getResource(
        "/var/cache/test/resource").getValueMap().get("jcr:primaryType", ""));
    assertNotNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));
    assertEquals("sling:Folder", resourceResolver.getResource(
        "/var/cache/test/resource/new-cache-file").getValueMap().get("jcr:primaryType", ""));
  }

  @Test
  public void createResourcesFromPathWhenCreateFromPathHasServiceRootPath()
      throws ResourceNotFoundException, PersistenceException {
    assertNotNull(resourceResolver.getResource("/var/cache/test"));
    assertNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));

    jcrFileCacheService.createResourcesFromPath("/var/cache/test/resource/new-cache-file",
        resourceResolver);

    assertNotNull(resourceResolver.getResource("/var/cache/test/resource"));
    assertEquals("sling:Folder", resourceResolver.getResource(
        "/var/cache/test/resource").getValueMap().get("jcr:primaryType", ""));
    assertNotNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));
    assertEquals("sling:Folder", resourceResolver.getResource(
        "/var/cache/test/resource/new-cache-file").getValueMap().get("jcr:primaryType", ""));
  }

  @Test
  public void createResourcesFromPathWhenResourcesExist()
      throws ResourceNotFoundException, PersistenceException {
    assertNotNull(resourceResolver.getResource("/var/cache/test"));
    assertNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));

    verify(resourceResolver, never()).create(any(), any(), any());

    jcrFileCacheService.createResourcesFromPath("/var/cache/test/resource/new-cache-file",
        resourceResolver);
    verify(resourceResolver, times(2)).create(any(), any(), any());

    jcrFileCacheService.createResourcesFromPath("/var/cache/test/resource/new-cache-file",
        resourceResolver);

    verify(resourceResolver, times(2)).create(any(), any(), any());

    verify(resourceResolver, never()).commit();

    assertNotNull(resourceResolver.getResource("/var/cache/test/resource"));
    assertEquals("sling:Folder", resourceResolver.getResource(
        "/var/cache/test/resource").getValueMap().get("jcr:primaryType", ""));
    assertNotNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));
    assertEquals("sling:Folder", resourceResolver.getResource(
        "/var/cache/test/resource/new-cache-file").getValueMap().get("jcr:primaryType", ""));
  }

  @Test
  public void createResourcesFromPathWhenNoPath()
      throws ResourceNotFoundException, PersistenceException {
    assertNotNull(resourceResolver.getResource("/var/cache/test"));
    assertNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));

    jcrFileCacheService.createResourcesFromPath("/var/cache/test/  /   /", resourceResolver);

    assertNull(resourceResolver.getResource("/var/cache/test/resource"));
    assertNull(resourceResolver.getResource("/var/cache/test/resource/new-cache-file"));
  }

}