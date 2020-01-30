package io.kestros.commons.osgiserviceutils.utils;


import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

public class OsgiServiceUtilsTest {

  @Rule
  public SlingContext context = new SlingContext();

  private ResourceResolver resourceResolver;

  private ResourceResolverFactory resourceResolverFactory;

  private Object service;

  @Before
  public void setUp() throws Exception {
    resourceResolverFactory = mock(ResourceResolverFactory.class);
    service = new Object();
  }

  @Test
  public void setOpenServiceResourceResolver() throws LoginException {
    resourceResolver = context.resourceResolver();
    assertNotNull(resourceResolver);

    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);

    assertNotNull(resourceResolver);
    assertTrue(resourceResolver.isLive());
  }

  @Test
  public void setOpenServiceResourceResolverWhenResolverHasBeenClosed() throws LoginException {
    resourceResolver = spy(context.resourceResolver());
    doReturn(false).when(resourceResolver).isLive();

    assertFalse(resourceResolver.isLive());

    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(
        context.resourceResolver());
    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);

    assertNotNull(resourceResolver);
    assertTrue(resourceResolver.isLive());
  }

  @Test
  public void setOpenServiceResourceResolverWhenResourceResolverIsNull() throws LoginException {
    assertNull(resourceResolver);
    when(resourceResolverFactory.getServiceResourceResolver(any())).thenReturn(
        context.resourceResolver());
    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);

    assertNotNull(resourceResolver);
    assertTrue(resourceResolver.isLive());
  }

  @Test(expected = LoginException.class)
  public void setOpenServiceResourceResolverWhenLoginException() throws LoginException {
    assertNull(resourceResolver);

    when(resourceResolverFactory.getServiceResourceResolver(any())).thenThrow(LoginException.class);

    resourceResolver = OsgiServiceUtils.getOpenServiceResourceResolver("service", resourceResolver,
        resourceResolverFactory, service);
  }

  @Test
  public void closeServiceResourceResolver() {
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.isLive()).thenReturn(true);

    OsgiServiceUtils.closeServiceResourceResolver(resourceResolver, service);

    verify(resourceResolver, times(1)).close();
  }

  @Test
  public void closeServiceResourceResolverWhenResolverIsNull() {
    OsgiServiceUtils.closeServiceResourceResolver(resourceResolver, service);

    assertNull(resourceResolver);
  }

  @Test
  public void closeServiceResourceResolverWhenResourceResolverIsNotLive() {
    resourceResolver = mock(ResourceResolver.class);
    when(resourceResolver.isLive()).thenReturn(false);

    OsgiServiceUtils.closeServiceResourceResolver(resourceResolver, service);

    verify(resourceResolver, never()).close();
    assertNotNull(resourceResolver);
  }
}