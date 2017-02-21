package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.ARCHIVE_PATH_KEY;
import static com.adaptris.management.aar.Constants.GLOBAL_LIB_PATH_KEY;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ContainerBootstrap {
  
  private static Properties containerProperties;
  
  public static void main(String[] args) {
    new ContainerBootstrap().doContainer(args);
  }
  
  public void doContainer(String[] args) {
    try {
      containerProperties = parseArguments(args);
      PropertiesHelper.verifyProperties(containerProperties, GLOBAL_LIB_PATH_KEY, ARCHIVE_PATH_KEY);
      
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
      
      System.out.println("Interlok container started - awaiting archives...");
      
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(this.usage());
    }
  }

  /**
   * Get the Container Properties file from the arguments.
   * @param args
   * @return
   * @throws Exception
   */
  private Properties parseArguments(String[] args) throws Exception {
    if(args.length == 1) {
      return PropertiesHelper.loadFromFile(args[0]);
    } else
      throw new Exception("Incorrect number of arguments to ContainerBootstrap");
  }

  private String usage() {
    return "Only a single argument is required; the path to the container properties file.";
  }

  public static Properties getContainerProperties() {
    return containerProperties;
  }

  public static void setContainerProperties(Properties containerProperties) {
    ContainerBootstrap.containerProperties = containerProperties;
  }

  
}
