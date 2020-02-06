package io.kestros.commons.osgiserviceutils.services.cache;

import io.kestros.commons.osgiserviceutils.exceptions.CachePurgeException;
import java.util.Date;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

/**
 * Manages the managing, building, purging and retrieval of Cached values.
 */
public interface CacheService {

  /**
   * Activates the CacheService.
   */
  @Activate
  void activate();

  /**
   * Deactivates the Cache Service.
   */
  @Deactivate
  void deactivate();

  /**
   * Name of cache, to be displayed within a UI.
   *
   * @return Displayed name of cache.
   */
  String getDisplayName();

  /**
   * Enables cache service to allow caching and value retrieval. Purges caches before enabling.
   *
   * @param resourceResolver ResourceResolver for user performing cache purge.
   * @throws CachePurgeException CacheService failed to purge all cached values.
   */
  void enable(ResourceResolver resourceResolver) throws CachePurgeException;

  /**
   * Disables cache service to prevent caching and value retrieval. Purges caches disabling.
   *
   * @param resourceResolver ResourceResolver for user performing cache purge.
   * @throws CachePurgeException CacheService failed to purge all cached values.
   */
  void disable(ResourceResolver resourceResolver) throws CachePurgeException;

  /**
   * Whether the cache service is enabled.
   *
   * @return Whether the cache service is enabled.
   */
  boolean isLive();

  /**
   * Date the last purgeAll was performed.
   *
   * @return Date the last purgeAll was performed.
   */
  Date getLastPurged();

  /**
   * UserID that performed the last purgeAll.
   *
   * @return UserID that performed the last purgeAll.
   */
  String getLastPurgedBy();

  /**
   * Purges entire cache.
   *
   * @param resourceResolver ResourceResolver for user performing cache purge.
   * @throws CachePurgeException CacheService failed to purge all cached values.
   */
  void purgeAll(ResourceResolver resourceResolver) throws CachePurgeException;

}
