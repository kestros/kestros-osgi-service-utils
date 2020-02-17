# Kestros OSGI Service Utils
Foundational and utility logic for building OSGI Services on Kestros/Sling instances.

## Baseline Services
### Service User Resource Resolver Service
### Cache Service
#### Base Cache Service
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