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

import static io.kestros.commons.osgiserviceutils.utils.ResourceCreationUtils.createTextFileResourceAndCommit;
import static io.kestros.commons.structuredslingmodels.utils.FileModelUtils.adaptToFileType;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.adaptToBaseResource;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getChildrenAsBaseResource;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getResourceAsBaseResource;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kestros.commons.osgiserviceutils.exceptions.CacheBuilderException;
import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import io.kestros.commons.structuredslingmodels.BaseResource;
import io.kestros.commons.structuredslingmodels.exceptions.InvalidResourceTypeException;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import io.kestros.commons.structuredslingmodels.filetypes.BaseFile;
import io.kestros.commons.structuredslingmodels.filetypes.FileType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Baseline logic for building a cache within the JCR. Caches output as nt:file Resources.
 */
public abstract class JcrFileCacheService extends BaseCacheService {

  private static final long serialVersionUID = 7012577452405327834L;

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Root Resource path to build the cache from. If /content/sites/page is cached, it will cache to
   * /var/cache/pages/content/sites/page.html, with /var/cache/pages being the cache root path.
   *
   * @return Root Resource path to build the cache from.
   */
  public abstract String getServiceCacheRootPath();

  protected abstract String getServiceUserName();

  protected abstract ResourceResolverFactory getResourceResolverFactory();

  protected abstract List<String> getRequiredResourcePaths();

  /**
   * Activates Cache service. Opens service ResourceResolver, which is used to build cached files.
   *
   * @param componentContext ComponentContext.
   */
  @Activate
  public void activate(ComponentContext componentContext) {
    log.info("Activating {}.", getDisplayName().replaceAll("[\r\n]", ""));
  }

  /**
   * Deactivates the service and closes the associated service ResourceResolver.
   *
   * @param componentContext ComponentContext.
   */
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Deactivate
  public void deactivate(ComponentContext componentContext) {
    log.info("Deactivating {}.", getDisplayName().replaceAll("[\r\n]", ""));
    try (ResourceResolver resourceResolver = getServiceResourceResolver()) {
      purgeAll(resourceResolver);
    } catch (final CachePurgeException e) {
      if (e.getMessage() != null) {
        log.error(e.getMessage().replaceAll("[\r\n]", ""));
      } else {
        log.error("Unable to clear cache on deactivation and exception message was null.");
      }
    } catch (LoginException e) {
      log.error("Unable to close service ResourceResolver.", e);
    }
  }


  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Override
  public void runAdditionalHealthChecks(FormattingResultLog log) {
    try (ResourceResolver serviceResourceResolver = getServiceResourceResolver()) {
      if (!serviceResourceResolver.isLive()) {
        log.critical("Service ResourceResolver is not live.");
      } else {
        for (String requiredResourcePath : getRequiredResourcePaths()) {
          if (serviceResourceResolver.getResource(requiredResourcePath) == null) {
            log.critical(
                    String.format("Required resource %s was not found.", requiredResourcePath));
          }
        }
      }
    } catch (LoginException e) {
      log.critical(String.format("Unable to open service ResourceResolver: %s", e.getMessage()));
    }
  }

  protected void createCacheFile(final String content, final String relativePath,
          final FileType type, ResourceResolver resourceResolver) throws CacheBuilderException {
    final String parentPath = getParentPathFromPath(getServiceCacheRootPath() + relativePath);
    final String newFileName = relativePath.split("/")[relativePath.split("/").length - 1];

    if (resourceResolver.getResource(parentPath) == null) {
      try {
        createResourcesFromPath(parentPath, resourceResolver);
      } catch (final ResourceNotFoundException
                     |
                     PersistenceException exception) {
        throw new CacheBuilderException(String.format(
                "%s was unable to create jcr file cache for '%s'. Cache root resource not found. "
                        + "%s", getClass().getSimpleName(), relativePath, exception.getMessage()));
      }
    }
    try {
      final BaseResource parentResource = getResourceAsBaseResource(parentPath, resourceResolver);
      createTextFileResourceAndCommit(content, type.getOutputContentType(),
              parentResource.getResource(), newFileName, resourceResolver);
    } catch (final ResourceNotFoundException | PersistenceException exception) {
      throw new CacheBuilderException(
              String.format("%s failed to create jcr cache file for '%s'. %s",
                      getClass().getSimpleName(), relativePath, exception.getMessage()));
    }
  }

  protected <T extends BaseFile> T getCachedFile(final String path, final Class<T> type,
          ResourceResolver resourceResolver)
          throws ResourceNotFoundException, InvalidResourceTypeException {

    final BaseResource cachedFileResource = getResourceAsBaseResource(
            getServiceCacheRootPath() + path, resourceResolver);
    return adaptToFileType(cachedFileResource, type);
  }

  protected boolean isFileCached(final String relativePath, ResourceResolver resourceResolver) {
    return resourceResolver.getResource(getServiceCacheRootPath() + relativePath)
            != null;
  }

  @Override
  protected void doPurge(final ResourceResolver resourceResolver) throws CachePurgeException {
    final Resource serviceCacheRootResource = resourceResolver.getResource(
            getServiceCacheRootPath());
    log.info("{} purging cache.", getClass().getSimpleName().replaceAll("[\r\n]", ""));
    if (serviceCacheRootResource != null) {
      List<BaseResource> resourceToPurgeList = getChildrenAsBaseResource(
              serviceCacheRootResource);
      log.debug("Purging {} top level resource.", resourceToPurgeList.size());
      for (final BaseResource cacheRootChild : resourceToPurgeList) {
        if (!cacheRootChild.getName().equals("rep:policy")) {
          try {
            resourceResolver.delete(cacheRootChild.getResource());
            resourceResolver.commit();
          } catch (final PersistenceException exception) {
            log.warn("Unable to delete {} while purging cache.",
                    cacheRootChild.getPath().replaceAll("[\r\n]", ""));
          }
        }
      }
      log.info("{} successfully purged cache.",
              getClass().getSimpleName().replaceAll("[\r\n]", ""));
    } else {
      throw new CachePurgeException(
              "Failed to purge cache " + getClass().getSimpleName() + ". Cache root resource "
                      + getServiceCacheRootPath() + " not found.");
    }
  }

  protected void doPurge(String resourcePath, final ResourceResolver resourceResolver)
          throws CachePurgeException {
    final Resource serviceCacheRootResource = resourceResolver.getResource(
            getServiceCacheRootPath());
    log.info("{} purging cache.", getClass().getSimpleName().replaceAll("[\r\n]", ""));
    if (serviceCacheRootResource != null) {
      List<BaseResource> resourceToPurgeList = getChildrenAsBaseResource(
              serviceCacheRootResource);
      log.debug("Purging {} top level resource.", resourceToPurgeList.size());
      for (final BaseResource cacheRootChild : resourceToPurgeList) {
        if (!cacheRootChild.getName().equals("rep:policy")) {
          try {
            resourceResolver.delete(cacheRootChild.getResource());
            resourceResolver.commit();
          } catch (final PersistenceException exception) {
            log.warn("Unable to delete {} while purging cache.",
                    cacheRootChild.getPath().replaceAll("[\r\n]", ""));
          }
        }
      }
      log.info("{} successfully purged cache.",
              getClass().getSimpleName().replaceAll("[\r\n]", ""));
    } else {
      throw new CachePurgeException(
              "Failed to purge cache " + getClass().getSimpleName() + ". Cache root resource "
                      + getServiceCacheRootPath() + " not found.");
    }

  }

  String getParentPathFromPath(final String path) {
    return path.substring(0, path.lastIndexOf('/'));
  }

  void createResourcesFromPath(String path, final ResourceResolver resourceResolver)
          throws ResourceNotFoundException, PersistenceException {
    if (path.startsWith(getServiceCacheRootPath())) {
      path = path.split(getServiceCacheRootPath())[1];
    }
    final String[] pathSegments = path.split("/");
    final Map<String, Object> properties = new HashMap<>();
    properties.put(JCR_PRIMARYTYPE, "sling:Folder");
    BaseResource parentResource = getResourceAsBaseResource(getServiceCacheRootPath(),
            resourceResolver);
    for (final String resourceName : pathSegments) {
      final Resource resourceToCreate = resourceResolver.getResource(
              parentResource.getPath() + "/" + resourceName);
      if (resourceToCreate == null) {
        final Resource newResource = resourceResolver.create(parentResource.getResource(),
                resourceName,
                properties);
        parentResource = adaptToBaseResource(newResource);
      } else {
        parentResource = adaptToBaseResource(resourceToCreate);
      }
    }
  }
}