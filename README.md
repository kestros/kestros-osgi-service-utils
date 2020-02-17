# Kestros OSGI Service Utils
Foundational and utility logic for building OSGI Services on Kestros/Sling instances.

- [Baseline Services](#baseline-services)
  * [Service User Resource Resolver Service](#service-user-resource-resolver-service)
    + [Mapping Service Users](#mapping-service-users)
  * [Cache Service](#cache-service)
    + [Base Cache Service](#base-cache-service)
    + [Jcr File Cache Service](#jcr-file-cache-service)
    <!-- + [Managed Cache Service](#managed-cache-service) -->
  * [Cache Purge Event Listener Service](#cache-purge-event-listener-service)
- [Utilities](#utilities)
  * [OSGI Service Utils](#osgi-service-utils)
  * [Resource Creation Utils](#resource-creation-utils)


## Baseline Services

### Service User Resource Resolver Service
OSGI services that use service users can extend the `BaseServiceResolverService` abstract service class.  `BaseServiceResolverService` handles Service `ResourceResolver` creation on service activation and closes the service `ResourceResolver` when the service deactivates.

```
@Component(immediate = true, service = MyServiceResourceResolverService.class)
public class MyServiceResourceResolverService extends BaseServiceResolverService {

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Override
  protected String getServiceUserName() {
    return "my-service-user-name";
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }
}
```

#### Mapping Service Users
Service User Mapper configurations are used to map service user names to JCR system / principal users.

To map service users, create a new configuration under a app's `/config` folder. (/apps/my-site/config).  
 
Resource name: `org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-my-principal-name.xml`
```
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
  jcr:primaryType="sling:OsgiConfig"
  user.mapping="[com.mysite.my-artifact:my-service-user-name=my-principal-name]"/>
```
For more information see [Sling Documentation] (https://sling.apache.org/documentation/the-sling-engine/service-authentication.html#service-user-mappings)

### Cache Service
`CacheService` is a baseline interface for OSGI Services that will handle cache building, retrieval, and purges.  The interface only contains methods for purging cache, extending interfaces should provide methods for building and retrieving cached values.

#### Interfacing the CacheService Interface
When building new cache services, create a new interface Class which extends `CacheService`.
```
public interface MyCacheService extends CacheService {
   void cacheMyString(String content);
   void cacheMyPage(BaseContentPage page);
}
```
Implementing Services are prioritized by service `rank`.
```
@Component(immediate = true, service = {ManagedCacheService.class, MyCacheService.class},
            property = "service.ranking:Integer=1")
public class MyCacheService extends BaseCacheService implements MyCacheService {
  // This cache service will not be used when interacting with `MyCacheService` due to its lower rank.
}

@Component(immediate = true, service = {ManagedCacheService.class, MyCacheService.class},
            property = "service.ranking:Integer=100")
public class MyPrioritizedCacheService extends BaseCacheService implements MyCacheService {
  // This cache service will be used when interacting with `MyCacheService` due to its higher rank.
}
```

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
Provides caching for services that will use files stored in the JCR their cache.

```
@Component(immediate = true, service = {MyCacheService.class})
public class MyCacheServiceImpl extends JcrFileCacheService implements MyCacheService {

   // Used for building service user ResourceResolver. See Service User Resource Resolver for registering service users.
  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  // If the cache service will build/purge caches asynchronously using the Sling JobManager.
  @Reference
  private JobManager jobManager;

  @Override
  public String getServiceCacheRootPath() {
    // Path to cache root.
    return "/var/cache/my-jcr-file-cache";
  }

  @Override
  protected String getServiceUserName() {
    // Service user mapping id.
    return "my-jcr-cache-service-user";
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }

  @Override
  public String getDisplayName() {
    // Cache display name, only required for managed caches.
    return "Sample Jcr Cache Service";
  }

  @Override
  public JobManager getJobManager() {
    // Only required if using JobManager to build/purge cache asynchronously.
    return jobManager;
  }

  @Override
  protected long getMinimumTimeBetweenCachePurges() {
    // Time before a cache can be purged after its last purge. 
    // The prevents cache purges from triggering too frequent, 
    // for example from an event listener during a code deployment.
    return 1000;
  }

  @Override
  public String getCacheCreationJobName() {
    // Only required if using JobManager to build/purge cache asynchronously.
    return "sample-creation";
  }
}
```


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
To create an EventListener that purges one or more `CacheService` implementation, extend `BaseCachePurgeOnResourceChangeEventListener`.

```
// Changes to listen for.
@Component(service = ResourceChangeListener.class,
           property = {ResourceChangeListener.CHANGES + "=ADDED",
               ResourceChangeListener.CHANGES + "=CHANGED",
               ResourceChangeListener.CHANGES + "=REMOVED",
               ResourceChangeListener.CHANGES + "=PROVIDER_ADDED",
               ResourceChangeListener.CHANGES + "=PROVIDER_REMOVED",
               ResourceChangeListener.PATHS + "=/apps"},
           immediate = true)
public class MyCachePurgeOnResourceChangeEventListener
    extends BaseCachePurgeOnResourceChangeEventListener {

  @Reference(cardinality = ReferenceCardinality.OPTIONAL,
             policyOption = ReferencePolicyOption.GREEDY)
  private MyCacheService cacheService;

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Override
  protected String getServiceUserName() {
    // mapped service user that will perform the cache clear.
    return "my-service-user-name";
  }

  @Override
  public <T extends CacheService> List<T> getCacheServices() {
    // caches services to purge.
    return Arrays.asList((T) cacheService);
  }

  @Override
  public ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }
}
```

## Utilities
### OSGI Service Utils
### Resource Creation Utils