# Kestros OSGI Service Utils
Foundational and utility logic for building OSGI Services on Kestros/Sling instances.

## Baseline Services
### Service User Resource Resolver Service
### Cache Service
#### Base Cache Service
Baseline abstract CacheService class which handles cache purge management logic (last purged, last purged by, enable/disable).  All cache building, cache retrieval, and cache purging logic will need to be provided on extending classes.

```
@Component(immediate = true, service = {ManagedCacheService.class, MyCacheService.class})
public class MyCacheServiceImpl extends BaseCacheService implements MyCacheService {
   
  // If the cache service will build/purge caches asynchronously using the Sling JobManager. 
  @Reference
  private JobManager jobManager;

  @Override
  protected void doPurge(ResourceResolver resourceResolver) throws CachePurgeException {
    // Purge logic.
  }

  @Override
  protected long getMinimumTimeBetweenCachePurges() {
    // Time before a cache can be purged after its last purge. 
    // The prevents cache purges from triggering too frequent, 
    // for example from an event listener during a code deployment.
    return 1000;
  }

  @Override
  protected String getCacheCreationJobName() {
    // Only required if using JobManager to build/purge cache asynchronously.
    return "sample-creation-job-name";
  }

  @Override
  protected JobManager getJobManager() {
    // Only required if using JobManager to build/purge cache asynchronously.
    return jobManager;
  }

  @Override
  public void activate() {
    // OSGI Component activation logic.
  }

  @Override
  public void deactivate() {
    // OSGI Component activation logic.
  }

  @Override
  public String getDisplayName() {
    // Cache display name, only required for managed caches.
    return "sample cache service";
  }
}
```
#### Jcr File Cache Service



<!-- 
#### Managed Cache Service
A cache services can be managed from the Kestros UI by registering it as a `ManagedCacheService` component.
```
@Component(immediate = true, service = {ManagedCacheService.class, MyCacheService.class})
public class MyCacheServiceImpl extends JcrFileCacheService implements MyCacheService {
}
```
--> 
### Cache Purge Event Listener Service

## Utilities
### OSGI Service Utils
### Resource Creation Utils