package io.kestros.commons.osgiserviceutils.services.cache.impl;

import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.closeServiceResourceResolver;
import static io.kestros.commons.osgiserviceutils.utils.OsgiServiceUtils.getOpenServiceResourceResolverOrNullAndLogExceptions;
import static io.kestros.commons.osgiserviceutils.utils.ResourceCreationUtils.createTextFileResourceAndCommit;
import static io.kestros.commons.structuredslingmodels.utils.FileModelUtils.adaptToFileType;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.adaptToBaseResource;
import static io.kestros.commons.structuredslingmodels.utils.SlingModelUtils.getResourceAsBaseResource;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import io.kestros.commons.osgiserviceutils.services.exceptions.CacheBuilderException;
import io.kestros.commons.osgiserviceutils.services.exceptions.CachePurgeException;
import io.kestros.commons.structuredslingmodels.BaseResource;
import io.kestros.commons.structuredslingmodels.exceptions.InvalidResourceTypeException;
import io.kestros.commons.structuredslingmodels.exceptions.ResourceNotFoundException;
import io.kestros.commons.structuredslingmodels.filetypes.BaseFile;
import io.kestros.commons.structuredslingmodels.filetypes.FileType;
import io.kestros.commons.structuredslingmodels.utils.SlingModelUtils;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

public abstract class JcrFileCacheService extends BaseCacheService {

  private ResourceResolver serviceResourceResolver;

  public abstract String getServiceCacheRootPath();

  protected abstract String getServiceUserName();

  protected abstract ResourceResolverFactory getResourceResolverFactory();

  /**
   * Activates Cache service. Opens service ResourceResolver, which is used to build cached files.
   */
  @Activate
  public void activate() {
    serviceResourceResolver = getOpenServiceResourceResolverOrNullAndLogExceptions(
        getServiceUserName(), getServiceResourceResolver(), getResourceResolverFactory(), this);
    try {
      purgeAll(getServiceResourceResolver());
    } catch (CachePurgeException e) {
      e.printStackTrace();
    }
  }

  /**
   * Deactivates the service and closes the associated service ResourceResolver.
   */
  @Deactivate
  public void deactivate() {
    try {
      purgeAll(getServiceResourceResolver());
    } catch (CachePurgeException e) {
      e.printStackTrace();
    }
    closeServiceResourceResolver(getServiceResourceResolver(), this);
  }

  @Nullable
  public ResourceResolver getServiceResourceResolver() {
    return this.serviceResourceResolver;
  }

  protected void createCacheFile(String content, String relativePath, FileType type)
      throws CacheBuilderException {
    String parentPath = getParentPathFromPath(getServiceCacheRootPath() + relativePath);
    String newFileName = relativePath.split("/")[relativePath.split("/").length - 1];

    if (getServiceResourceResolver() != null && getServiceResourceResolver().isLive()) {
      if (getServiceResourceResolver().getResource(parentPath) == null) {
        try {
          createResourcesFromPath(parentPath, getServiceResourceResolver());
        } catch (ResourceNotFoundException | PersistenceException exception) {
          throw new CacheBuilderException(String.format(
              "%s was unable to create jcr file cache for '%s'. Cache root resource not found. %s",
              getClass().getSimpleName(), relativePath, exception.getMessage()));
        }
      }
      try {
        BaseResource parentResource = getResourceAsBaseResource(parentPath,
            getServiceResourceResolver());
        createTextFileResourceAndCommit(content, type.getOutputContentType(),
            parentResource.getResource(), newFileName, getServiceResourceResolver());
      } catch (ResourceNotFoundException | PersistenceException exception) {
        throw new CacheBuilderException(
            String.format("%s failed to create jcr cache file for '%s'. %s",
                getClass().getSimpleName(), relativePath, exception.getMessage()));
      }
    } else {
      throw new CacheBuilderException(String.format(
          "%s failed to create jcr cache file for %s due to closed service resourceResolver.",
          getClass().getSimpleName(), relativePath));
    }
  }

  protected <T extends BaseFile> T getCachedFile(String path, Class<T> type)
      throws ResourceNotFoundException, InvalidResourceTypeException {
    if (getServiceResourceResolver() != null) {
      BaseResource cachedFileResource = getResourceAsBaseResource(getServiceCacheRootPath() + path,
          getServiceResourceResolver());
      return adaptToFileType(cachedFileResource, type);
    }
    throw new ResourceNotFoundException("No service resolver to retrieve cached file.");
  }

  protected boolean isFileCached(String relativePath) {
    if (getServiceResourceResolver() != null) {
      return getServiceResourceResolver().getResource(getServiceCacheRootPath() + relativePath)
             != null;
    }
    return false;
  }

  @Override
  protected void doPurge(ResourceResolver resourceResolver) throws CachePurgeException {
    log.info("{} purging cache.", getClass().getSimpleName());
    Resource serviceCacheRootResource = resourceResolver.getResource(getServiceCacheRootPath());
    if (serviceCacheRootResource != null) {

      for (BaseResource cacheRootChild : SlingModelUtils.getChildrenAsBaseResource(
          serviceCacheRootResource)) {
        try {
          resourceResolver.delete(cacheRootChild.getResource());
        } catch (PersistenceException exception) {
          log.debug("Unable to delete {} while purging cache.", cacheRootChild.getPath());
        }
      }
      //      try {
      log.info("{} successfully purged cache.", getClass().getSimpleName());
      //        resourceResolver.commit();
      //      } catch (PersistenceException exception) {
      //        throw new CachePurgeException(exception.getMessage());
      //      }
    } else {
      throw new CachePurgeException(
          "Failed to purge cache " + getClass().getSimpleName() + ". Cache root resource "
          + getServiceCacheRootPath() + " not found.");
    }
  }

  String getParentPathFromPath(String path) {
    return path.substring(0, path.lastIndexOf('/'));
  }

  void createResourcesFromPath(String path, ResourceResolver resolver)
      throws ResourceNotFoundException, PersistenceException {
    if (path.startsWith(getServiceCacheRootPath())) {
      path = path.split(getServiceCacheRootPath())[1];
    }
    String[] pathSegments = path.split("/");
    Map<String, Object> properties = new HashMap<>();
    properties.put(JCR_PRIMARYTYPE, "sling:Folder");
    BaseResource parentResource = getResourceAsBaseResource(getServiceCacheRootPath(), resolver);
    for (String resourceName : pathSegments) {
      Resource resourceToCreate = resolver.getResource(
          parentResource.getPath() + "/" + resourceName);
      if (resourceToCreate == null) {
        Resource newResource = resolver.create(parentResource.getResource(), resourceName,
            properties);
        parentResource = adaptToBaseResource(newResource);
      } else {
        parentResource = adaptToBaseResource(resourceToCreate);
      }
    }
  }


}