package com.coreos.appc;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class AciManifest {
  public String acKind;
  public String acVersion;
  public String name;
  public Map<String, String> labels;
  public App app;
  public List<Dependency> dependencies;
  public List<String> pathWhitelist;
  public Map<String, String> annotations;

  public static class App {
    public List<String> exec = Lists.newArrayList();
    public String user;
    public String group;
    public List<EventHandler> eventHandlers;
    public String workingDirectory;
    public List<Environment> environment;
    public List<Isolator> isolators;
    public List<MountPoint> mountPoints;
    public List<Port> ports;

    public void addEnvironment(String name, String value) {
      Environment env = new Environment();
      env.name = name;
      env.value = value;

      if (environment == null) {
        environment = Lists.newArrayList();
      }
      environment.add(env);
    }

    public void addPort(String port) {
      Port p = new Port();
      p.name = port;
      p.port = port;
      // TODO(justinsb): Is this optional?
      p.protocol = "tcp";

      if (ports == null) {
        ports = Lists.newArrayList();
      }
      ports.add(p);
    }
  }

  public static class EventHandler {
    public String name;
    public List<String> exec = Lists.newArrayList();
  }

  public static class Environment {
    public String name;
    public String value;
  }

  public static class Isolator {
    public String name;
    public JsonObject value;
  }

  public static class MountPoint {
    public String name;
    public String path;
    public boolean readOnly;
  }

  public static class Port {
    public String name;
    public String port;
    public String protocol;
    public boolean socketActivated;
  }

  public static class Dependency {
    public String app;
    public String imageId;
    public Map<String, String> labels;
  }

  public static class Annotations {
    public static final String AUTHOR = "author";
  }

  public void write(Appendable writer) {
    Gson gson = new Gson();
    gson.toJson(this, writer);
  }

  public void addDependency(String baseImage) {
    if (dependencies == null) {
      dependencies = Lists.newArrayList();
    }
    Dependency dependency = new Dependency();
    dependency.app = baseImage;
    dependencies.add(dependency);
  }

  public Map<String, String> annotations() {
    if (annotations == null) {
      annotations = Maps.newHashMap();
    }
    return annotations;
  }

  public App app() {
    if (app == null) {
      app = new App();
    }
    return app;
  }
}
