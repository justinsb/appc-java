package com.coreos.appc;

import java.io.File;

public class ContainerFile {

  public final File sourcePath;
  public final String imagePath;

  public ContainerFile(File sourcePath, String imagePath) {
    this.sourcePath = sourcePath;
    this.imagePath = imagePath;
  }

}
