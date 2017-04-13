package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.ARCHIVE_PATH_KEY;
import static com.adaptris.management.aar.Constants.GLOBAL_LIB_PATH_KEY;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.management.MBeanServer;

public class ContainerBootstrap {
  
  private static Properties containerProperties;
  
  public static void main(String[] args) {
    new ContainerBootstrap().doContainer(args);
  }
  
  void doContainer(String[] args) {
    try {
      containerProperties = parseArguments(args);
      PropertiesHelper.verifyProperties(containerProperties, GLOBAL_LIB_PATH_KEY, ARCHIVE_PATH_KEY);
      ClasspathInitialiser.init(containerProperties.getProperty(GLOBAL_LIB_PATH_KEY).split(File.pathSeparator));
      MBeanServer s = ManagementFactory.getPlatformMBeanServer();

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
      SimpleLogger.log(this.usage(), e);
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

  static Properties getContainerProperties() {
    return containerProperties;
  }
  
}
