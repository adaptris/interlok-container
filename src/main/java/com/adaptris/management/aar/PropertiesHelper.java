package com.adaptris.management.aar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class PropertiesHelper {

  static Properties load(String resource, String defaultResource) throws IOException {
    try {
      return load(resource);
    }
    catch (IOException e) {
      return load(defaultResource);
    }
  }

  static Properties load(String resource) throws IOException {
    try {
      return viaFile(resource);
    }
    catch (FileNotFoundException e) {
      return viaClassloader(resource);
    }
  }

  static Properties viaFile(String filePath) throws IOException {
    Properties result = new Properties();
    File containerProperties = new File(filePath);
    if (containerProperties.exists()) {
      try (FileInputStream in = new FileInputStream(containerProperties)) {
        result.load(in);
      }
    }
    else {
      throw new FileNotFoundException(filePath);
    }
    return result;
  }

  static Properties viaClassloader(String resource) throws IOException {
    Properties result = new Properties();
    InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    if (in != null) {
      try (InputStream read = in) {
        result.load(read);
      }
    }
    else {
      throw new FileNotFoundException(resource);
    }
    return result;
  }

  static Properties verifyProperties(Properties properties, String... requiredKeys) throws Exception {
    for (String requiredKey : requiredKeys) {
      if (!properties.containsKey(requiredKey))
        throw new Exception("Properties does not contain the required property: " + requiredKey);
    }
    return properties;
  }
}
