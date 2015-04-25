package com.coreos.appc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

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
  public void push(AciImageInfo imageInfo, File image, byte[] signature, Logger log) {
    String imageKey = buildKey(imageInfo, "aci");

    {
      log.info("Uploading image to {}/{}", bucketName, imageKey);
      PutObjectRequest request = new PutObjectRequest(bucketName, imageKey, image);
      if (makePublic) {
        request.withCannedAcl(CannedAccessControlList.PublicRead);
      }
      s3.putObject(request);
    }

    String signatureKey = buildKey(imageInfo, "aci.asc");
    if (signature != null) {
      InputStream contents = new ByteArrayInputStream(signature);
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(signature.length);
      PutObjectRequest request = new PutObjectRequest(bucketName, signatureKey, contents, metadata);
      if (makePublic) {
        request.withCannedAcl(CannedAccessControlList.PublicRead);
      }
      log.info("Uploading signature to {}/{}", bucketName, signatureKey);
      s3.putObject(request);
    }

    if (updateLatest) {
      String tag = "latest";
      log.info("Setting up redirect for {}", tag);

      String tagKey = buildKey(imageInfo, "aci", tag);

      String redirectTo = "/" + imageKey;
      createRedirect(tagKey, redirectTo);
    }

    if (updateLatest && signature != null) {
      String tag = "latest";
      log.info("Setting up signature redirect for {}", tag);

      String tagSignatureKey = buildKey(imageInfo, "aci.asc", tag);
      //
      // InputStream contents = new ByteArrayInputStream(signature);
      // ObjectMetadata metadata = new ObjectMetadata();
      // metadata.setContentLength(signature.length);
      // PutObjectRequest request = new PutObjectRequest(bucketName, tagSignatureKey, contents,
      // metadata);
      // if (makePublic) {
      // request.withCannedAcl(CannedAccessControlList.PublicRead);
      // }
      // log.info("Uploading signature to {}/{}", bucketName, tagSignatureKey);
      // s3.putObject(request);

      String redirectTo = "/" + signatureKey;
      createRedirect(tagSignatureKey, redirectTo);
    }
  }

  private void createRedirect(String fromKey, String redirectTo) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(0);
    metadata.setHeader("x-amz-website-redirect-location", redirectTo);
    PutObjectRequest request = new PutObjectRequest(bucketName, fromKey, new ByteArrayInputStream(
        new byte[0]), metadata);
    if (makePublic) {
      request.withCannedAcl(CannedAccessControlList.PublicRead);
    }
    s3.putObject(request);
  }

  private String buildKey(AciImageInfo imageInfo, String extension) {

    String version = imageInfo.version;
    if (Strings.isNullOrEmpty(version)) {
      version = AciImageInfo.DEFAULT_VERSION;
    }

    return buildKey(imageInfo, extension, version);
  }

  private String buildKey(AciImageInfo imageInfo, String extension, String version) {
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

    String os = imageInfo.os;
    if (Strings.isNullOrEmpty(os)) {
      os = AciImageInfo.DEFAULT_OS;
    }

    String arch = imageInfo.arch;
    if (Strings.isNullOrEmpty(arch)) {
      arch = AciImageInfo.DEFAULT_ARCH;
    }

    // key += name + "-" + version + "-" + os + "-" + arch + "." + ext;

    String key = baseKey + os + "/" + arch + "/" + name + "-" + version + "." + extension;

    return key;
  }
}