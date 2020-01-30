package io.kestros.commons.osgiserviceutils.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import static org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class ResourceCreationUtils {

  private ResourceCreationUtils() {
    // Do nothing
  }

  /**
   * Creates text nt:file resource with specified mimeType. New file is NOT committed during this
   * method.
   *
   * @param content Content of text file.
   * @param mimeType File mimeType.
   * @param parentResource Resource to create new file as a child of.
   * @param name Name of new resource.
   * @param resourceResolver ResourceResolver used to create new file.
   * @throws PersistenceException New file could not be created/persisted by resourceResolver.
   */
  public static void createTextFileResource(final String content, final String mimeType,
      final Resource parentResource, final String name, final ResourceResolver resourceResolver)
      throws PersistenceException {
    final Map<String, Object> properties = new HashMap<>();
    properties.put(JCR_PRIMARYTYPE, "nt:file");

    final Map<String, Object> jcrContentProperties = new HashMap<>();
    jcrContentProperties.put(JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
    jcrContentProperties.put(JCR_DATA, new ByteArrayInputStream(content.getBytes(UTF_8)));
    jcrContentProperties.put(JCR_MIMETYPE, mimeType);

    final Resource fileResource = resourceResolver.create(parentResource, name, properties);
    resourceResolver.create(fileResource, JCR_CONTENT, jcrContentProperties);
  }

  /**
   * Creates text nt:file resource with specified mimeType. New file is committed during this
   * method.
   *
   * @param content Content of text file.
   * @param mimeType File mimeType.
   * @param parentResource Resource to create new file as a child of.
   * @param name Name of new resource.
   * @param resourceResolver ResourceResolver used to create new file.
   * @throws PersistenceException New file could not be created/persisted by resourceResolver.
   */
  public static void createTextFileResourceAndCommit(final String content, final String mimeType,
      final Resource parentResource, final String name, final ResourceResolver resourceResolver)
      throws PersistenceException {
    createTextFileResource(content, mimeType, parentResource, name, resourceResolver);
    resourceResolver.commit();
  }

}
