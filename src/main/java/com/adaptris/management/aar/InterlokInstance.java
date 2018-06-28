package com.adaptris.management.aar;

import java.util.Properties;

class InterlokInstance {

  private static final Properties DEFAULTS;

  static {
    DEFAULTS = new Properties();
    DEFAULTS.setProperty(Constants.INSTANCE_MAIN_CLASS, "com.adaptris.core.management.SimpleBootstrap");
    DEFAULTS.setProperty(Constants.INSTANCE_INVOKED_METHODS, "logVersionInformation,standardBoot");
  }

  private String instanceName;

  private Properties instanceProperties = new Properties(DEFAULTS);

  InterlokInstance() {
    instanceProperties = new Properties(DEFAULTS);
  }
  
  InterlokInstance(String instanceName, Properties instanceProperties) {
    this.setInstanceName(instanceName);
    this.setInstanceProperties(instanceProperties);
  }

  String getInstanceName() {
    return instanceName;
  }

  void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  Properties getInstanceProperties() {
    return instanceProperties;
  }

  void setInstanceProperties(Properties instanceProperties) {
    this.instanceProperties.putAll(instanceProperties);
  }
  
  String instanceMainClass() {
    return instanceProperties.getProperty(Constants.INSTANCE_MAIN_CLASS);
  }

  String[] instanceInvokedMethods() {
    return instanceProperties.getProperty(Constants.INSTANCE_INVOKED_METHODS).split(",");
  }
}
