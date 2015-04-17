package com.coreos.appc;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.slf4j.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.base.Strings;

public class S3AciRepository extends AciRepository {
  final AmazonS3 s3;
  final String bucketName;
  final String prefix;

  public boolean updateLatest = true;
  public boolean makePublic = true;

  public S3AciRepository(AmazonS3 s3, String bucketName, String prefix) {
    this.s3 = s3;
    this.bucketName = bucketName;
    this.prefix = prefix;
  }

  @Override
  public void push(AciImageInfo imageInfo, File image, Logger log) {
    String baseKey = prefix;
    if (Strings.isNullOrEmpty(baseKey)) {
      baseKey = "";
    } else if (!baseKey.endsWith("/")) {
      baseKey += "/";
    }

    String name = imageInfo.name;
    if (Strings.isNullOrEmpty(name)) {
      throw new IllegalArgumentException("name is required");
    }

    String version = imageInfo.version;
    if (Strings.isNullOrEmpty(version)) {
      version = AciImageInfo.DEFAULT_VERSION;
    }

    String os = imageInfo.os;
    if (Strings.isNullOrEmpty(os)) {
      os = AciImageInfo.DEFAULT_OS;
    }

    String arch = imageInfo.arch;
    if (Strings.isNullOrEmpty(arch)) {
      arch = AciImageInfo.DEFAULT_ARCH;
    }

    String ext = "aci";

    // key += name + "-" + version + "-" + os + "-" + arch + "." + ext;

    String key = baseKey + os + "/" + arch + "/" + bucketName + "/" + name + "-" + version + "."
        + ext;

    PutObjectRequest request = new PutObjectRequest(bucketName, key, image);
    if (makePublic) {
      request.withCannedAcl(CannedAccessControlList.PublicRead);
    }
    log.info("Uploading image to {}/{}", bucketName, key);
    s3.putObject(request);

    if (updateLatest) {
      String tag = "latest";

      log.info("Setting up redirect for {}", tag);
      String tagKey = baseKey + os + "/" + arch + "/" + bucketName + "/" + name + "-" + tag + "."
          + ext;

      String redirectTo = "/" + key;

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(0);
      metadata.setHeader("x-amz-website-redirect-location", redirectTo);
      request = new PutObjectRequest(bucketName, tagKey, new ByteArrayInputStream(new byte[0]),
          metadata);
      if (makePublic) {
        request.withCannedAcl(CannedAccessControlList.PublicRead);
      }
      s3.putObject(request);
    }
  }
}