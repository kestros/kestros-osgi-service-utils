# Kestros OSGi Service Utils

Foundational and utility logic for building OSGi services on Kestros/Sling instances.

## Purpose

`kestros-osgi-service-utils` provides the base abstract classes and interfaces that all Kestros OSGi services extend. It is the lowest-level service framework in the Kestros architecture, handling service user authentication, ResourceResolver lifecycle management, cache infrastructure, health checks, event listeners, and JCR resource creation utilities.

Nearly every Kestros module that registers an OSGi service depends on this bundle. If you are writing a new service for Kestros, you will extend one of the base classes provided here.

## Installation & Build

**Maven coordinates:**

```xml
<dependency>
  <groupId>io.kestros.commons</groupId>
  <artifactId>kestros-osgi-service-utils</artifactId>
  <version>0.1.11</version>
</dependency>
```

**Build:**

```bash
mvn clean package
```

**Deploy to a Sling instance:**

```bash
curl -u admin:admin \
  -F "action=install" \
  -F "bundlestart=true" \
  -F "bundlefile=@target/kestros-osgi-service-utils-0.1.11.jar" \
  "http://localhost:8080/system/console/bundles"
```

## Configuration

### Service User Mapping

Services that extend `BaseServiceResolverService` require a service user mapping. Create an OSGi configuration under your application's `/config` folder:

**Resource name:** `org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-<principal-name>.xml`

```xml
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0"
          xmlns:jcr="http://www.jcp.org/jcr/1.0"
  jcr:primaryType="sling:OsgiConfig"
  user.mapping="[<bundle-symbolic-name>:<service-user-name>=<principal-name>]"/>
```

See the [Sling Service Authentication documentation](https://sling.apache.org/documentation/the-sling-engine/service-authentication.html#service-user-mappings) for details.

No additional OSGi configuration properties are required by this bundle itself.

## API / Service Usage

### Interfaces

#### `ManagedService`

Base interface for all Kestros OSGi services. Provides lifecycle hooks and health check integration.

```java
public interface ManagedService {
    String getDisplayName();
    void activate(ComponentContext componentContext);
    void deactivate(ComponentContext componentContext);
    void runAdditionalHealthChecks(FormattingResultLog log);
}
```

#### `CacheService`

Interface for services that manage cache building, retrieval, and purging. Extends `ManagedService`.

```java
public interface CacheService extends ManagedService {
    void enable(ResourceResolver resolver) throws CachePurgeException;
    void disable(ResourceResolver resolver) throws CachePurgeException;
    boolean isLive();
    Date getLastPurged();
    String getLastPurgedBy();
    void purgeAll(ResourceResolver resolver) throws CachePurgeException;
}
```

#### `ManagedCacheService`

Marker interface extending `CacheService` that designates a cache service as manageable within the Kestros UI.

#### `ExternalConnectionService`

Interface for services that connect to external systems. Tracks connection success/failure timestamps. Extends `ManagedService`.

```java
public interface ExternalConnectionService extends ManagedService {
    void connectionSuccessful();
    void connectionFailed(String reason);
    Date getLastSuccessfulConnection();
    Date getLastFailedConnection();
    String getLastFailedConnectionReason();
}
```

#### `CachePurgeOnResourceChangeEventListener`

Interface for event listeners that purge cache services when JCR resources change. Extends `ResourceChangeListener` and `ManagedService`.

```java
public interface CachePurgeOnResourceChangeEventListener
    extends ResourceChangeListener, ManagedService {
    <T extends CacheService> List<T> getCacheServices();
    ResourceResolverFactory getResourceResolverFactory();
}
```

### Abstract Base Classes

#### `BaseServiceResolverService`

The most commonly extended class. Creates a service ResourceResolver on activation and provides it to subclasses. All JCR operations in Kestros services go through this class.

**Required overrides:**

| Method | Returns | Description |
|--------|---------|-------------|
| `getServiceUserName()` | `String` | The service user mapping name |
| `getResourceResolverFactory()` | `ResourceResolverFactory` | Injected `@Reference` |
| `getLogger()` | `Logger` | SLF4J logger for the subclass |
| `getRequiredResourcePaths()` | `List<String>` | JCR paths to verify during health checks |
| `getDisplayName()` | `String` | Human-readable service name |

**Usage example:**

```java
@Component(service = MyService.class, immediate = true)
public class MyServiceImpl extends BaseServiceResolverService implements MyService {

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    protected String getServiceUserName() {
        return "my-service-user";
    }

    @Override
    protected ResourceResolverFactory getResourceResolverFactory() {
        return resolverFactory;
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    @Override
    protected List<String> getRequiredResourcePaths() {
        return Collections.emptyList();
    }

    @Override
    public String getDisplayName() {
        return "My Service";
    }

    public void doWork() {
        try (ResourceResolver resolver = getServiceResourceResolver()) {
            // Use resolver for JCR operations
        } catch (LoginException e) {
            getLogger().error("Failed to get service resolver", e);
        }
    }
}
```

#### `BaseCacheService`

Abstract class for cache services. Extends `BaseServiceResolverService` and implements `CacheService` + `ManagedCacheService`. Handles enable/disable state, purge tracking, and purge throttling.

**Required overrides:**

| Method | Returns | Description |
|--------|---------|-------------|
| `doPurge(ResourceResolver)` | `void` | Custom purge logic |
| `afterCachePurgeComplete(ResourceResolver)` | `void` | Post-purge hook |
| `getMinimumTimeBetweenCachePurges()` | `long` | Throttle interval in milliseconds |
| `getCacheCreationJobName()` | `String` | Sling Job topic for async cache builds |
| `getJobManager()` | `JobManager` | Injected `@Reference` (nullable) |

#### `JcrFileCacheService`

Extends `BaseCacheService` for caches that store files in the JCR. Provides the `getServiceCacheRootPath()` method to define where cached files are stored (e.g. `/var/cache/my-cache`).

#### `BaseExternalConnectionService`

Abstract class for external connection services. Tracks timestamps for successful and failed connections.

#### `BaseCachePurgeOnResourceChangeEventListener`

Abstract event listener that purges specified `CacheService` instances when JCR resource changes are detected.

### Health Checks

#### `BaseManagedServiceHealthCheck`

Abstract Felix Health Check for verifying that a `ManagedService` is registered and running. Override `getManagedService()` and `getServiceName()`.

### Utility Classes

#### `OsgiServiceUtils`

Static utility methods for OSGi service operations.

| Method | Description |
|--------|-------------|
| `getOpenServiceResourceResolver(serviceName, resolver, factory, service)` | Opens or reuses a service ResourceResolver |
| `getOpenServiceResourceResolverOrNullAndLogExceptions(...)` | Same as above, but returns null on failure |
| `closeServiceResourceResolver(resolver, service)` | Closes a ResourceResolver if it is live |
| `getOsgiServiceOfType(componentContext, type)` | Retrieves the top-ranked registered service of a type |
| `getAllOsgiServicesOfType(componentContext, type)` | Retrieves all registered services of a type |

#### `ResourceCreationUtils`

Static utility methods for creating JCR resources.

| Method | Description |
|--------|-------------|
| `createTextFileResource(content, mimeType, parent, name, resolver)` | Creates an `nt:file` resource (does not commit) |
| `createTextFileResourceAndCommit(content, mimeType, parent, name, resolver)` | Creates an `nt:file` resource and commits |

### Exceptions

| Exception | Thrown When |
|-----------|------------|
| `CacheBuilderException` | Cache build operation fails |
| `CachePurgeException` | Cache purge operation fails |
| `CacheRetrievalException` | Cache retrieval operation fails |

## Dependencies

### Upstream

| Dependency | Maven Coordinates |
|------------|-------------------|
| kestros-structured-sling-models | `io.kestros.commons:kestros-structured-sling-models:[0.2.5,0.2.99]` |
| Apache Sling API | `org.apache.sling:org.apache.sling.api` |
| Apache Felix Health Check API | `org.apache.felix:org.apache.felix.healthcheck.api` |
| Apache Sling Event | `org.apache.sling:org.apache.sling.event` |

### Downstream (modules that depend on this)

This is a foundational dependency. Nearly every Kestros service bundle depends on it, including:

- `kestros-validation-api`
- `kestros-validation-core`
- `kestros-cache-management-foundation`
- `kestros-site-management-core`
- `kestros-sling-ui-libraries`
- `kestros-tasks` (application module)

## Contribution Notes

- **Branch from:** `develop`
- **PR target:** `develop`
- **Branch naming:** `{type}/TASK-NNN-short-description` (e.g. `fix/TASK-042-resolver-null-check`)
- **Commit format:** `[kestros-osgi-service-utils]: <action>, <brief result>`
- **Build verification:** Run `mvn clean package` before submitting; all tests must pass
