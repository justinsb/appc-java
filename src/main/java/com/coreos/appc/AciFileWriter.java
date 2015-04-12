package com.coreos.appc;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

public class AciFileWriter implements Closeable {
  final ArchiveOutputStream tarOutputStream;

  final Set<String> directories = Sets.newHashSet();

  public AciFileWriter(File outputFile, boolean compress) throws IOException {
    this(new FileOutputStream(outputFile), compress);
  }

  public AciFileWriter(OutputStream outputStream, boolean compress) throws IOException {
    if (compress) {
      outputStream = new GZIPOutputStream(outputStream);
    }
    ArchiveOutputStream tarOutputStream;
    try {
      tarOutputStream = new ArchiveStreamFactory().createArchiveOutputStream(
          ArchiveStreamFactory.TAR, outputStream);
    } catch (ArchiveException e) {
      throw new IOException("Error creating output stream", e);
    }
    this.tarOutputStream = tarOutputStream;
  }

  @Override
  public void close() throws IOException {
    tarOutputStream.close();
  }

  public void mkdir(String imagePath) throws IOException {
    if (!imagePath.endsWith("/")) {
      imagePath += "/";
    }

    String tarPath = toTarPath(imagePath);

    TarArchiveEntry entry = new TarArchiveEntry(tarPath);

    tarOutputStream.putArchiveEntry(entry);
    tarOutputStream.closeArchiveEntry();
  }

  public boolean hasDirectory(String imagePath) throws IOException {
    if (!imagePath.endsWith("/")) {
      imagePath += "/";
    }
    return directories.contains(imagePath);
  }

  public void mkdirs(String imagePath) throws IOException {
    String path = "";
    for (String token : Splitter.on('/').split(imagePath)) {
      path += token + "/";
      if (!hasDirectory(path)) {
        mkdir(path);
      }
    }
  }

  private static String toTarPath(String imagePath) {
    String tarPath;
    if (imagePath.startsWith("/")) {
      tarPath = "rootfs" + imagePath;
    } else {
      tarPath = "rootfs/" + imagePath;
    }
    return tarPath;
  }

  public void addFile(File src, String imagePath) throws IOException {
    String tarPath = toTarPath(imagePath);

    TarArchiveEntry entry = new TarArchiveEntry(src);
    entry.setName(tarPath);

    tarOutputStream.putArchiveEntry(entry);
    try (FileInputStream fis = new FileInputStream(src)) {
      IOUtils.copy(fis, tarOutputStream);
    }
    tarOutputStream.closeArchiveEntry();
  }

  public void addManifest(AciManifest manifest) throws IOException {
    StringWriter writer = new StringWriter();
    manifest.write(writer);
    byte[] manifestData = writer.toString().getBytes(Charsets.UTF_8);

    TarArchiveEntry entry = new TarArchiveEntry("manifest");
    entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
    entry.setSize(manifestData.length);
    entry.setUserId(0);
    entry.setGroupId(0);

    tarOutputStream.putArchiveEntry(entry);
    tarOutputStream.write(manifestData);
    tarOutputStream.closeArchiveEntry();
  }
}
