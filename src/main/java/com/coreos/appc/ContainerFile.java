package com.coreos.appc;

import java.nio.file.Path;

public class ContainerFile {

  public final Path sourcePath;
  public final String imagePath;

  public ContainerFile(Path sourcePath, String imagePath) {
    this.sourcePath = sourcePath;
    this.imagePath = imagePath;
  }

}
