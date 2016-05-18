package com.adaptris.management.aar;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class PropertiesHelper {
  
  public static Properties loadFromFile(String filePath) throws Exception {
    Properties returnProperties = new Properties();
    File containerProperties = new File(filePath);
    if(containerProperties.exists()) {
      returnProperties.load(new FileInputStream(containerProperties));
    } else
      throw new Exception("Cannot find properties file: " + containerProperties.getAbsolutePath());
    
    return returnProperties;
  }
  
  public static void verifyProperties(Properties properties, String... requiredKeys) throws Exception {
    for(String requiredKey : requiredKeys) {
      if(!properties.containsKey(requiredKey))
        throw new Exception("Properties does not contain the required property: " + requiredKey);
    }
  }

}
