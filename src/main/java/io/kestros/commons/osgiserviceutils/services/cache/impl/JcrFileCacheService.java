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

import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.closeServiceResourceResolver;
import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.getOpenServiceResourceResolverOrNullAndLogExceptions;
import static io.kestros.commons.osgiserviceutils.utils.ResourceCreationUtils.createTextFileResourceAndCommit;
import static io.kestros.commons.structuredslingmodels.utils.FileModelUtils.adaptToFileType;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.adaptToBaseResource;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getChildrenAsBaseResource;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getResourceAsBaseResource;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

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
import javax.annotation.Nullable;
import org.apache.felix.hc.api.FormattingResultLog;
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

  protected ResourceResolver serviceResourceResolver;

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
    log.info("Activating {}.", getDisplayName());
    serviceResourceResolver = getOpenServiceResourceResolverOrNullAndLogExceptions(
        getServiceUserName(), getServiceResourceResolver(), getResourceResolverFactory(), this);
  }

  /**
   * Deactivates the service and closes the associated service ResourceResolver.
   *
   * @param componentContext ComponentContext.
   */
  @Deactivate
  public void deactivate(ComponentContext componentContext) {
    log.info("Deactivating {}.", getDisplayName());
    try {
      purgeAll(getServiceResourceResolver());
    } catch (final CachePurgeException e) {
      log.error(e.getMessage());
    }
    closeServiceResourceResolver(getServiceResourceResolver(), this);
  }


  @Override
  public void runAdditionalHealthChecks(FormattingResultLog log) {
    if (getServiceResourceResolver() == null) {
      log.critical("Service ResourceResolver was null.");
    } else {
      if (!getServiceResourceResolver().isLive()) {
        log.critical("Service ResourceResolver is not live.");
      } else {
        for (String requiredResourcePath : getRequiredResourcePaths()) {
          if (getServiceResourceResolver().getResource(requiredResourcePath) == null) {
            log.critical(
                String.format("Required resource %s was not found.", requiredResourcePath));
          }
        }
      }
    }
  }

  /**
   * ServiceResourceResolver.
   *
   * @return ServiceResourceResolver.
   */
  @Nullable
  public ResourceResolver getServiceResourceResolver() {
    return this.serviceResourceResolver;
  }

  protected ResourceResolver getNewServiceResourceResolver() {
    return getOpenServiceResourceResolverOrNullAndLogExceptions(getServiceUserName(),
        getServiceResourceResolver(), getResourceResolverFactory(), this);
  }

  protected void createCacheFile(final String content, final String relativePath,
      final FileType type) throws CacheBuilderException {
    final String parentPath = getParentPathFromPath(getServiceCacheRootPath() + relativePath);
    final String newFileName = relativePath.split("/")[relativePath.split("/").length - 1];

    if (getServiceResourceResolver() != null) {
      if (getServiceResourceResolver().isLive()) {
        if (getServiceResourceResolver().getResource(parentPath) == null) {
          try {
            createResourcesFromPath(parentPath, getServiceResourceResolver());
          } catch (final ResourceNotFoundException | PersistenceException exception) {
            throw new CacheBuilderException(String.format(
                "%s was unable to create jcr file cache for '%s'. Cache root resource not found. "
                + "%s", getClass().getSimpleName(), relativePath, exception.getMessage()));
          }
        }
        try {
          final BaseResource parentResource = getResourceAsBaseResource(parentPath,
              getServiceResourceResolver());
          createTextFileResourceAndCommit(content, type.getOutputContentType(),
              parentResource.getResource(), newFileName, getServiceResourceResolver());
        } catch (final ResourceNotFoundException | PersistenceException exception) {
          throw new CacheBuilderException(
              String.format("%s failed to create jcr cache file for '%s'. %s",
                  getClass().getSimpleName(), relativePath, exception.getMessage()));
        }
      } else {
        throw new CacheBuilderException(String.format(
            "%s failed to create jcr cache file for %s due to closed service resourceResolver.",
            getClass().getSimpleName(), relativePath));
      }
    } else {
      throw new CacheBuilderException(
          String.format("%s failed to create jcr cache file for %s due to null resourceResolver.",
              getClass().getSimpleName(), relativePath));
    }
  }

  protected <T extends BaseFile> T getCachedFile(final String path, final Class<T> type)
      throws ResourceNotFoundException, InvalidResourceTypeException {
    if (getServiceResourceResolver() != null) {
      final BaseResource cachedFileResource = getResourceAsBaseResource(
          getServiceCacheRootPath() + path, getServiceResourceResolver());
      return adaptToFileType(cachedFileResource, type);
    }
    throw new ResourceNotFoundException("No service resolver to retrieve cached file.");
  }

  protected boolean isFileCached(final String relativePath) {
    if (getServiceResourceResolver() != null) {
      return getServiceResourceResolver().getResource(getServiceCacheRootPath() + relativePath)
             != null;
    }
    return false;
  }

  @Override
  protected void doPurge(final ResourceResolver resourceResolver) throws CachePurgeException {
    if (getServiceResourceResolver() != null) {
      getServiceResourceResolver().refresh();
      final Resource serviceCacheRootResource = getServiceResourceResolver().getResource(
          getServiceCacheRootPath());
      log.info("{} purging cache.", getClass().getSimpleName());
      if (serviceCacheRootResource != null) {
        List<BaseResource> resourceToPurgeList = getChildrenAsBaseResource(
            serviceCacheRootResource);
        log.debug("Purging {} top level resource.", resourceToPurgeList.size());
        for (final BaseResource cacheRootChild : resourceToPurgeList) {
          if (!cacheRootChild.getName().equals("rep:policy")) {
            try {
              getServiceResourceResolver().delete(cacheRootChild.getResource());
              getServiceResourceResolver().commit();
            } catch (final PersistenceException exception) {
              log.warn("Unable to delete {} while purging cache.", cacheRootChild.getPath());
            }
          }
        }
        log.info("{} successfully purged cache.", getClass().getSimpleName());
      } else {
        throw new CachePurgeException(
            "Failed to purge cache " + getClass().getSimpleName() + ". Cache root resource "
            + getServiceCacheRootPath() + " not found.");
      }
    } else {
      throw new CachePurgeException("Failed to purge cache " + getClass().getSimpleName()
                                    + ". Null service ResourceResolver.");
    }
  }

  protected void doPurge(String resourcePath, final ResourceResolver resourceResolver)
      throws CachePurgeException {
    if (getServiceResourceResolver() != null) {
      final Resource serviceCacheRootResource = getServiceResourceResolver().getResource(
          getServiceCacheRootPath());
      log.info("{} purging cache.", getClass().getSimpleName());
      if (serviceCacheRootResource != null) {
        List<BaseResource> resourceToPurgeList = getChildrenAsBaseResource(
            serviceCacheRootResource);
        log.debug("Purging {} top level resource.", resourceToPurgeList.size());
        for (final BaseResource cacheRootChild : resourceToPurgeList) {
          if (!cacheRootChild.getName().equals("rep:policy")) {
            try {
              getServiceResourceResolver().delete(cacheRootChild.getResource());
              getServiceResourceResolver().commit();
            } catch (final PersistenceException exception) {
              log.warn("Unable to delete {} while purging cache.", cacheRootChild.getPath());
            }
          }
        }
        log.info("{} successfully purged cache.", getClass().getSimpleName());
      } else {
        throw new CachePurgeException(
            "Failed to purge cache " + getClass().getSimpleName() + ". Cache root resource "
            + getServiceCacheRootPath() + " not found.");
      }
    } else {
      throw new CachePurgeException("Failed to purge cache " + getClass().getSimpleName()
                                    + ". Null service ResourceResolver.");
    }
  }

  String getParentPathFromPath(final String path) {
    return path.substring(0, path.lastIndexOf('/'));
  }

  void createResourcesFromPath(String path, final ResourceResolver resolver)
      throws ResourceNotFoundException, PersistenceException {
    if (path.startsWith(getServiceCacheRootPath())) {
      path = path.split(getServiceCacheRootPath())[1];
    }
    final String[] pathSegments = path.split("/");
    final Map<String, Object> properties = new HashMap<>();
    properties.put(JCR_PRIMARYTYPE, "sling:Folder");
    BaseResource parentResource = getResourceAsBaseResource(getServiceCacheRootPath(), resolver);
    for (final String resourceName : pathSegments) {
      final Resource resourceToCreate = resolver.getResource(
          parentResource.getPath() + "/" + resourceName);
      if (resourceToCreate == null) {
        final Resource newResource = resolver.create(parentResource.getResource(), resourceName,
            properties);
        parentResource = adaptToBaseResource(newResource);
      } else {
        parentResource = adaptToBaseResource(resourceToCreate);
      }
    }
  }


}