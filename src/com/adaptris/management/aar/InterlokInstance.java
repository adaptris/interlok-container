package com.adaptris.management.aar;

import java.util.Properties;

public class InterlokInstance {

  private String instanceName;
  
  private Properties instanceProperties;
  
  public InterlokInstance() {
  }
  
  public InterlokInstance(String instanceName, Properties instanceProperties) {
    this.setInstanceName(instanceName);
    this.setInstanceProperties(instanceProperties);
  }

  public String getInstanceName() {
    return instanceName;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

  public Properties getInstanceProperties() {
    return instanceProperties;
  }

  public void setInstanceProperties(Properties instanceProperties) {
    this.instanceProperties = instanceProperties;
  }
  
}
