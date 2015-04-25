package com.coreos.appc;

import java.io.File;

import org.slf4j.Logger;

public abstract class AciRepository {
  public abstract void push(AciImageInfo imageInfo, File image, byte[] signature, Logger log);
}
