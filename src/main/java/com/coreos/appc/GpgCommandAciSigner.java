package com.coreos.appc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

public class GpgCommandAciSigner extends AciSigner {

  @Override
  public byte[] sign(File file) throws InterruptedException {
    List<String> args = Lists.newArrayList();
    args.add("gpg");
    args.add("--armor");
    args.add("--output");
    args.add("-");
    args.add("--detach-sign");
    args.add(file.getAbsolutePath());

    ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);

    Process process;
    try {
      process = pb.start();
    } catch (IOException e) {
      throw new IllegalArgumentException("Error starting gpg", e);
    }
    StreamReader stdout = new StreamReader(process.getInputStream());
    stdout.start();
    StreamReader stderr = new StreamReader(process.getErrorStream());
    stderr.start();

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IllegalArgumentException("Error signing image; unexpected exit code: " + exitCode);
    }

    byte[] signature = stdout.asBytes();
    if (signature.length == 0) {
      throw new IllegalStateException("Failed to generate signature (no output)");
    }
    return signature;
  }

  private class StreamReader extends Thread {
    private InputStream is;
    private ByteArrayOutputStream os;

    public StreamReader(InputStream is) {
      this.is = is;
      this.os = new ByteArrayOutputStream();
    }

    @Override
    public void run() {
      try {
        ByteStreams.copy(is, os);
      } catch (IOException e) {
        log.warn("Error capturing process output");
      }
    }

    public byte[] asBytes() {
      return os.toByteArray();
    }

    public String asString(Charset charset) {
      try {
        return os.toString(charset.name());
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("Unknown encoding", e);
      }
    }

  }
}
