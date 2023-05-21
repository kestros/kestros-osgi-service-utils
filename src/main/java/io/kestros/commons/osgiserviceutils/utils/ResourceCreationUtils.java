/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.kestros.commons.osgiserviceutils.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;
import static org.apache.jackrabbit.JcrConstants.JCR_MIMETYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Utility methods for creating Resources within the JCR.
 */
public class ResourceCreationUtils {

  private ResourceCreationUtils() {
    // Do nothing
  }

  /**
   * Creates text nt:file resource with specified mimeType. New file is NOT committed during this
   * method.
   *
   * @param content          Content of text file.
   * @param mimeType         File mimeType.
   * @param parentResource   Resource to create new file as a child of.
   * @param name             Name of new resource.
   * @param resourceResolver ResourceResolver used to create new file.
   * @throws PersistenceException New file could not be created/persisted by resourceResolver.
   */
  public static void createTextFileResource(final String content, final String mimeType,
      final Resource parentResource, final String name, final ResourceResolver resourceResolver)
      throws PersistenceException {
    final Map<String, Object> properties = new HashMap<>();
    InputStream inputStream = null;
    try {
      inputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
      properties.put(JCR_PRIMARYTYPE, "nt:file");

      final Map<String, Object> jcrContentProperties = new HashMap<>();
      jcrContentProperties.put(JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
      jcrContentProperties.put(JCR_DATA, inputStream);
      jcrContentProperties.put(JCR_MIMETYPE, mimeType);

      final Resource fileResource = resourceResolver.create(parentResource, name, properties);
      resourceResolver.create(fileResource, JCR_CONTENT, jcrContentProperties);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // D nothing
        }
      }
    }
  }

  /**
   * Creates text nt:file resource with specified mimeType. New file is committed during this
   * method.
   *
   * @param content          Content of text file.
   * @param mimeType         File mimeType.
   * @param parentResource   Resource to create new file as a child of.
   * @param name             Name of new resource.
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
