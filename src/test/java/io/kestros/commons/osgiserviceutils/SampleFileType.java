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

package io.kestros.commons.osgiserviceutils;

import io.kestros.commons.structuredslingmodels.filetypes.BaseFile;
import io.kestros.commons.structuredslingmodels.filetypes.FileType;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

public enum SampleFileType implements FileType {

  SAMPLE_FILE_TYPE("sample", "text/html", Arrays.asList("text/html"), SampleFile.class);

  private final String name;
  private final String outputContentType;
  private final List<String> readableContentTypes;
  private final String extension;
  private final Class sampleFileClass;

  <T extends BaseFile> SampleFileType(String name, String outputContentType,
      List<String> readableContentTypes, Class<T> sampleFileClass) {
    this.name = name;
    this.outputContentType = outputContentType;
    this.readableContentTypes = readableContentTypes;
    this.extension = "." + name;
    this.sampleFileClass = sampleFileClass;
  }

  @Nonnull
  public String getName() {
    return this.name;
  }

  @Nonnull
  public String getExtension() {
    return this.extension;
  }

  @Override
  public String getOutputContentType() {
    return this.outputContentType;
  }

  @Nonnull
  public List<String> getReadableContentTypes() {
    return this.readableContentTypes;
  }

  @Nonnull
  public <T extends BaseFile> Class<T> getFileModelClass() {
    return this.sampleFileClass;
  }
}