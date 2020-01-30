package io.kestros.commons.osgiserviceutils;

import static io.kestros.commons.osgiserviceutils.SampleFileType.SAMPLE_FILE_TYPE;

import io.kestros.commons.structuredslingmodels.filetypes.BaseFile;
import io.kestros.commons.structuredslingmodels.filetypes.FileType;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = Resource.class,
       resourceType = "nt:file")
public class SampleFile extends BaseFile {

  @Override
  public FileType getFileType() {
    return SAMPLE_FILE_TYPE;
  }
}
