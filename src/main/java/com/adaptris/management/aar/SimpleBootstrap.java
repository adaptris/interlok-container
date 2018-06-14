package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.ARCHIVE_PATH_KEY;
import static com.adaptris.management.aar.Constants.DEFAULT_BOOTSTRAP_RESOURCE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimpleBootstrap {
  
  private static Properties containerProperties = new Properties();

  public static void main(String[] args) throws Exception {
    new SimpleBootstrap().startContainer(PropertiesHelper.verifyProperties(parseArguments(args), ARCHIVE_PATH_KEY));
  }
  
  protected void startContainer(Properties p) {
    try {
      containerProperties = p;
      List<InterlokInstance> instanceList = new ArrayList<>();
      File aarDirectory = new File(containerProperties.getProperty(ARCHIVE_PATH_KEY));
      if(!aarDirectory.exists())
        throw new Exception("Archive directory does not exist: " + aarDirectory.getAbsolutePath());
      else {
        for(String aarInstance : aarDirectory.list()) {
          instanceList.add(new ArchiveInstanceRunner(aarInstance, containerProperties).startInstance());
        }
      }
      ArchiveWatcher archiveWatcher = new ArchiveWatcher();
      archiveWatcher.setDirectoryToWatch(aarDirectory.getCanonicalPath());
      archiveWatcher.setInstanceList(instanceList);
      archiveWatcher.start();
      SimpleLogger.log("Interlok container started - awaiting archives...");
    } catch (Exception e) {
      SimpleLogger.log("Failed to start container", e);
    }
  }

  /**
   * Get the Container Properties file from the arguments.
   * 
   * @param args
   * @return properties.
   * @throws Exception
   */
  protected static Properties parseArguments(String[] args) throws Exception {
    if (args.length > 0) {
      return PropertiesHelper.load(args[0], DEFAULT_BOOTSTRAP_RESOURCE);
    }
    // No args; assume default.
    return PropertiesHelper.load(DEFAULT_BOOTSTRAP_RESOURCE);
  }

  static Properties containerProperties() {
    return containerProperties;
  }
}
