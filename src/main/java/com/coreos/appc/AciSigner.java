package com.coreos.appc;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

public abstract class AciSigner {
  public Logger log = NOPLogger.NOP_LOGGER;

  public abstract byte[] sign(File file) throws InterruptedException;

}
