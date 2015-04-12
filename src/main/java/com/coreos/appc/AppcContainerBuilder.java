package com.coreos.appc;

import static com.google.common.base.CharMatcher.WHITESPACE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class AppcContainerBuilder extends ContainerBuilder {

  final List<ContainerFile> addFiles = Lists.newArrayList();

  final File aciFile;

  public boolean compress = true;

  public AppcContainerBuilder(File aciFile) {
    this.aciFile = aciFile;
  }

  @Override
  public void addFiles(List<ContainerFile> containerFiles) throws IOException {
    addFiles.addAll(containerFiles);
  }

  @Override
  public void buildImage(String imageName) throws Exception {
    log.info("Building image " + imageName);

    try (AciFileWriter aciFileWriter = new AciFileWriter(aciFile, compress)) {
      AciManifest manifest = createAciManifest(imageName);

      aciFileWriter.addManifest(manifest);

      for (ContainerFile containerFile : addFiles) {
        final Path sourcePath = containerFile.sourcePath;

        String imagePath = containerFile.imagePath;
        log.info(String.format("Writing %s -> %s", sourcePath, imagePath));

        if (imagePath.contains("/")) {
          String dir = imagePath.substring(0, imagePath.lastIndexOf('/'));
          aciFileWriter.mkdirs(dir);
        }

        aciFileWriter.addFile(sourcePath.toFile(), imagePath);

        // // ensure all directories exist because copy operation will fail if they don't
        // Files.createDirectories(destPath.getParent());
        // Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        // Files.setLastModifiedTime(destPath, FileTime.fromMillis(0));
        // // file location relative to docker directory, used later to generate Dockerfile
        // final Path relativePath = Paths.get(containerFile.imagePath);
        // copiedPaths.add(relativePath.toString());
      }
      log.info("Built " + imageName);
    }
  }

  private AciManifest createAciManifest(String imageName) throws IOException {
    AciManifest manifest = new AciManifest();

    manifest.acKind = "ImageManifest";
    manifest.acVersion = "0.5.1";
    manifest.name = imageName;
    if (baseImage != null) {
      manifest.addDependency(baseImage);
    }

    if (maintainer != null) {
      manifest.annotations().put(AciManifest.Annotations.AUTHOR, maintainer);
    }

    List<String> exec = Lists.newArrayList();
    if (entryPoint != null) {
      exec.addAll(tokenize(entryPoint));
    }
    if (cmd != null) {
      exec.addAll(tokenize(cmd));
    }
    manifest.app().exec = exec;

    manifest.app().user = "0";
    manifest.app().group = "0";

    if (env != null) {
      final List<String> sortedKeys = Ordering.natural().sortedCopy(env.keySet());
      for (String key : sortedKeys) {
        final String value = env.get(key);
        manifest.app().addEnvironment(key, value);
      }
    }

    if (exposesSet != null && exposesSet.size() > 0) {
      // The values will be sorted with no duplicated since exposesSet is a TreeSet
      for (String exposes : exposesSet) {
        manifest.app().addPort(exposes);
      }
    }

    return manifest;
  }

  private static List<String> tokenize(String cmd) {
    // CMD needs to be a list of arguments if ENTRYPOINT is set.
    if (cmd.startsWith("[") && cmd.endsWith("]")) {
      // cmd seems to be an argument list
      String parse = cmd.substring(1, cmd.length() - 1);
      List<String> args = Lists.newArrayList();
      for (String arg : Splitter.on(",").omitEmptyStrings().split(parse)) {
        arg = arg.trim();
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
          arg = arg.substring(1, arg.length() - 1);
        }
        args.add(arg);
      }

      return args;
    } else {
      final List<String> args = ImmutableList.copyOf(Splitter.on(WHITESPACE).omitEmptyStrings()
          .split(cmd));
      return args;
    }
  }
}
