package com.coreos.appc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

public abstract class ContainerBuilder {
  /**
   * Logger; caller should replace with a useful destination for their environment.
   */
  public Logger log = NOPLogger.NOP_LOGGER;

  public String baseImage;
  public String cmd;
  public Map<String, String> env;
  public String entryPoint;
  public Set<String> exposesSet;
  public String maintainer;

  public abstract void addFiles(List<ContainerFile> containerFiles) throws IOException;

  public abstract void buildImage(File manifestFile) throws IOException;

  public abstract void writeManifest(File manifestFile, String imageName, String aciVersion) throws IOException;

  public abstract List<ContainerFile> getContainerFiles();
}
