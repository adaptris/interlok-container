package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.INTERLOK_INSTANCE_ID;
import static com.adaptris.management.aar.Constants.JMX_SERVICE_URL;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class InterlokInstanceManager {
  
  private ScheduledExecutorService scheduler;
  
  private ScheduledFuture<?> instanceHandle;
  
  private InterlokInstance interlokInstance;
  
  public InterlokInstanceManager(String instanceName) {
    interlokInstance = new InterlokInstance();
    this.interlokInstance.setInstanceName(instanceName);
    
    scheduler = Executors.newScheduledThreadPool(1);
  }
  
  public InterlokInstanceManager(InterlokInstance interlokInstance) {
    this.interlokInstance = interlokInstance;
    
    scheduler = Executors.newScheduledThreadPool(1);
  }
  
  public InterlokInstance scheduleStartup() {
    final Runnable instanceRunnable = new Runnable() {
      @Override
      public void run() {
        System.out.println("About to start new Interlok instance: " + interlokInstance.getInstanceName());
        try {
          interlokInstance = new ArchiveInstanceRunner(interlokInstance, ContainerBootstrap.getContainerProperties()).startInstance();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    
    this.instanceHandle = this.scheduler.schedule(instanceRunnable, 10, TimeUnit.SECONDS);
    
    return interlokInstance;
  }
  
  public boolean delayStartup() {
    System.out.println("Delaying startup of instance: " + interlokInstance.getInstanceName() + ".  Waiting for file copy.");
    boolean cancelled = this.instanceHandle.cancel(true);
    this.scheduleStartup();
    
    return cancelled;
  }
  
  public void shutdown() {
    JMXConnector jmxc = null;
    try {
      String jmxServiceUrl = this.interlokInstance.getInstanceProperties().getProperty(JMX_SERVICE_URL);
      if(jmxServiceUrl != null) {
        JMXServiceURL url = new JMXServiceURL(jmxServiceUrl); 
        jmxc = JMXConnectorFactory.connect(url, null); 
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection(); 
    
        ObjectName mbeanName = new ObjectName("com.adaptris:type=Adapter,id=" + this.interlokInstance.getInstanceProperties().getProperty(INTERLOK_INSTANCE_ID));
        mbsc.invoke(mbeanName, "requestClose", new Object[]{}, new String[]{});
        
        this.instanceHandle.cancel(true);
      } else
        System.out.println("Cannot shutdown instance; no jmxserviceurl property found.");
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      try {
        jmxc.close();
      } catch (Exception e) {
        // silently
      }
    }
  }

}
