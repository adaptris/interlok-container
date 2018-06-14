package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.ARCHIVE_PATH_KEY;
import static com.adaptris.management.aar.Constants.GLOBAL_LIB_PATH_KEY;

import java.io.File;
import java.util.Properties;

@Deprecated
public class ContainerBootstrap extends SimpleBootstrap {
  
  public static void main(String[] args) throws Exception {
    System.err.println("ContainerBootstrap is deprecated, and will be removed once Java9 is formally supported");
    Properties p = parseArguments(args);
    PropertiesHelper.verifyProperties(p, GLOBAL_LIB_PATH_KEY, ARCHIVE_PATH_KEY);
    ClasspathInitialiser.init(p.getProperty(GLOBAL_LIB_PATH_KEY).split(File.pathSeparator));
    new SimpleBootstrap().startContainer(p);
  }

}
