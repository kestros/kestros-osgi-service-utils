package io.kestros.commons.osgiserviceutils.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ResourceCreationUtilsTest {

  @Rule
  public SlingContext context = new SlingContext();

  private Resource resource;

  private ResourceResolver resourceResolver;

  @Before
  public void setUp() throws Exception {
    context.addModelsForPackage("io.kestros");
    resourceResolver = spy(context.resourceResolver());
  }

  @Test
  public void testCreateTextFileResource() throws PersistenceException {
    resource = context.create().resource("/resource");
    ResourceCreationUtils.createTextFileResource("Text Content", "text/html", resource,
        "new-text-file", resourceResolver);

    resource = resourceResolver.getResource("/resource/new-text-file");
    assertNotNull(resource);
    assertNotNull(resource.getChild("jcr:content"));
    assertEquals("nt:resource", resource.getChild("jcr:content").getResourceType());
    assertEquals("text/html", resource.getChild("jcr:content").getValueMap().get("jcr:mimeType",
        StringUtils.EMPTY));
    assertEquals("84", resource.getChild("jcr:content").getValueMap().get("jcr:data",
        StringUtils.EMPTY));
    verify(resourceResolver, never()).commit();
  }

  @Test
  public void testCreateTextFileResourceAndCommit() throws PersistenceException {
    resource = context.create().resource("/resource");
    ResourceCreationUtils.createTextFileResourceAndCommit("Text Content", "text/html", resource,
        "new-text-file", resourceResolver);

    resource = resourceResolver.getResource("/resource/new-text-file");
    assertNotNull(resource);
    assertNotNull(resource.getChild("jcr:content"));
    assertEquals("nt:resource", resource.getChild("jcr:content").getResourceType());
    assertEquals("text/html", resource.getChild("jcr:content").getValueMap().get("jcr:mimeType",
        StringUtils.EMPTY));
    assertEquals("84", resource.getChild("jcr:content").getValueMap().get("jcr:data",
        StringUtils.EMPTY));

    verify(resourceResolver, times(1)).commit();
  }

}