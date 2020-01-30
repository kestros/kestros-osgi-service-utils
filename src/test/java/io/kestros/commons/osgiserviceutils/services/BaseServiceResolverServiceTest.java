package io.kestros.commons.osgiserviceutils.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BaseServiceResolverServiceTest {

  @Rule
  public SlingContext context = new SlingContext();

  private SampleServiceResolverService serviceResolverService;

  private ResourceResolverFactory resourceResolverFactory;

  private ResourceResolver resourceResolver;

  @Before
  public void setUp() throws Exception {
    context.addModelsForPackage("io.kestros");

    resourceResolverFactory = mock(ResourceResolverFactory.class);
    resourceResolver = spy(context.resourceResolver());

    serviceResolverService = spy(new SampleServiceResolverService());

    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(resourceResolver);
    doReturn(resourceResolverFactory).when(serviceResolverService).getResourceResolverFactory();
  }

  @Test
  public void testGetServiceUserName() {
    assertEquals("user-name", serviceResolverService.getServiceUserName());
  }

  @Test
  public void testActivate() throws LoginException {
    serviceResolverService.activate(context.componentContext());
    verify(resourceResolverFactory, times(1)).getServiceResourceResolver(any());
    verify(serviceResolverService, times(1)).getServiceUserName();
    verify(serviceResolverService, times(1)).getServiceResourceResolver();
  }

  @Test
  public void testDeactivate() {
    serviceResolverService.activate(context.componentContext());
    serviceResolverService.deactivate();
    verify(resourceResolver, times(1)).close();
  }

  @Test
  public void testGetResourceResolverFactory() {
    assertEquals(resourceResolverFactory, serviceResolverService.getResourceResolverFactory());
  }

  @Test
  public void testGetServiceResourceResolver() {
    serviceResolverService.activate(context.componentContext());
    assertEquals(resourceResolver, serviceResolverService.getServiceResourceResolver());
  }
}