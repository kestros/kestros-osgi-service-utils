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