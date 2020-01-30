package io.kestros.commons.osgiserviceutils.services;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true,
           service = SampleServiceResolverService.class)
public class SampleServiceResolverService extends BaseServiceResolverService {

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @Override
  protected String getServiceUserName() {
    return "user-name";
  }

  @Override
  protected ResourceResolverFactory getResourceResolverFactory() {
    return resourceResolverFactory;
  }

}
