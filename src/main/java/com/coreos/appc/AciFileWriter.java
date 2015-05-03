package com.coreos.appc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

public class AciFileWriter {
  final TreeMap<String, TarEntry> entries = Maps.newTreeMap();

  static abstract class TarEntry {
    final String tarPath;

    public TarEntry(String tarPath) {
      this.tarPath = tarPath;
    }

    public abstract void write(ArchiveOutputStream tarOutputStream) throws IOException;

    public boolean isDirectory() {
      return false;
    }
  }

  static class DirectoryTarEntry extends TarEntry {

    public DirectoryTarEntry(String tarPath) {
      super(tarPath);
    }

    public void write(ArchiveOutputStream tarOutputStream) throws IOException {
      TarArchiveEntry entry = new TarArchiveEntry(tarPath);

      tarOutputStream.putArchiveEntry(entry);
      tarOutputStream.closeArchiveEntry();
    }

    public boolean isDirectory() {
      return true;
    }
  }

  static class FileTarEntry extends TarEntry {

    final File src;

    public FileTarEntry(String tarPath, File src) {
      super(tarPath);
      this.src = src;
    }

    public void write(ArchiveOutputStream tarOutputStream) throws IOException {
      TarArchiveEntry entry = new TarArchiveEntry(src);
      setMetadata(entry);

      tarOutputStream.putArchiveEntry(entry);
      try (FileInputStream fis = new FileInputStream(src)) {
        IOUtils.copy(fis, tarOutputStream);
      }
      tarOutputStream.closeArchiveEntry();
    }

    protected void setMetadata(TarArchiveEntry entry) {
      entry.setName(tarPath);
    }
  }

  private void mkdir(String tarPath) throws IOException {
    if (!tarPath.endsWith("/")) {
      tarPath += "/";
    }
    if (entries.containsKey(tarPath)) {
      throw new IllegalStateException();
    }
    entries.put(tarPath, new DirectoryTarEntry(tarPath));
  }

  public boolean hasDirectory(String imagePath) throws IOException {
    if (!imagePath.endsWith("/")) {
      imagePath += "/";
    }
    TarEntry tarEntry = entries.get(imagePath);
    if (tarEntry == null)
      return false;
    return tarEntry.isDirectory();
  }

  public void mkdirs(String imagePath) throws IOException {
    String tarPath = toTarPath(imagePath);

    String path = "";
    for (String token : Splitter.on('/').split(tarPath)) {
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

    if (entries.containsKey(tarPath)) {
      throw new IllegalStateException();
    }
    entries.put(tarPath, new FileTarEntry(tarPath, src));
  }

  public void addManifest(File manifestFile) throws IOException {
    String tarPath = "manifest";

    FileTarEntry fileTarEntry = new FileTarEntry(tarPath, manifestFile) {
      @Override
      protected void setMetadata(TarArchiveEntry entry) {
        super.setMetadata(entry);
        entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
        entry.setUserId(0);
        entry.setGroupId(0);
      }
    };

    if (entries.containsKey(tarPath)) {
      throw new IllegalStateException();
    }
    entries.put(tarPath, fileTarEntry);
  }

  public void write(File outputFile, boolean compress) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      write(fos, compress);
    }
  }

  void write(OutputStream outputStream, boolean compress) throws IOException {
    if (compress) {
      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
        write(gzipOutputStream);
      }
    } else {
      write(outputStream);
    }
  }

  void write(OutputStream outputStream) throws IOException {
    try (ArchiveOutputStream tarOutputStream = new ArchiveStreamFactory().createArchiveOutputStream(
        ArchiveStreamFactory.TAR, outputStream)) {
      write(tarOutputStream);
    } catch (ArchiveException e) {
      throw new IOException("Error creating output stream", e);
    }
  }

  public void write(ArchiveOutputStream tarOutputStream) throws IOException {
    for (TarEntry entry : entries.values()) {
      entry.write(tarOutputStream);
    }
  }
}
