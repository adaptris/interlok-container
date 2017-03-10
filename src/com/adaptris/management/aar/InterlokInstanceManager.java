package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.INTERLOK_INSTANCE_ID;
import static com.adaptris.management.aar.Constants.JMX_SERVICE_URL;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class InterlokInstanceManager {
  
  private ScheduledExecutorService scheduler;
  
  private ScheduledFuture<?> instanceHandle;
  
  private InterlokInstance interlokInstance;
  
  private boolean isRunning;
  
  public InterlokInstanceManager(String instanceName) {
    interlokInstance = new InterlokInstance();
    this.interlokInstance.setInstanceName(instanceName);
    
    scheduler = Executors.newScheduledThreadPool(1);
    
    isRunning = false;
  }
  
  public InterlokInstanceManager(InterlokInstance interlokInstance) {
    this.interlokInstance = interlokInstance;
    
    scheduler = Executors.newScheduledThreadPool(1);
  }
  
  public InterlokInstance scheduleStartup() {
    final Runnable instanceRunnable = new Runnable() {
      @Override
      public void run() {
        SimpleLogger.log("About to start new Interlok instance: " + interlokInstance.getInstanceName());
        try {
          interlokInstance = new ArchiveInstanceRunner(interlokInstance, ContainerBootstrap.getContainerProperties()).startInstance();
          isRunning = true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
    
    this.instanceHandle = this.scheduler.schedule(instanceRunnable, 10, TimeUnit.SECONDS);
    
    return interlokInstance;
  }
  
  public boolean delayStartup() {
    if(isRunning) {
      SimpleLogger.log("Instance already running: " + interlokInstance.getInstanceName() + ".  Ignoring modification.");
      return true;
    } else {
      SimpleLogger.log("Delaying startup of instance: " + interlokInstance.getInstanceName() + ".  Waiting for file copy.");
      boolean cancelled = cancelPendingStartup();
      scheduleStartup();
      return cancelled;
    }
  }
  
  public void shutdown() {
    JMXConnector jmxc = null;
    try {
      jmxc = connect(this.interlokInstance.getInstanceProperties().getProperty(JMX_SERVICE_URL));
      ObjectName mbeanName = new ObjectName(
          "com.adaptris:type=Adapter,id=" + this.interlokInstance.getInstanceProperties().getProperty(INTERLOK_INSTANCE_ID));
      SimpleLogger.log("Attempting shutdown of " + mbeanName.toString());
      if (jmxc != null) {
        jmxc.getMBeanServerConnection().invoke(mbeanName, "requestClose", new Object[] {}, new String[] {});
      } else {
        MBeanServer s = ManagementFactory.getPlatformMBeanServer();
        s.invoke(mbeanName, "requestClose", new Object[] {}, new String[] {});
      }
      cancelPendingStartup();
    }
    catch (InstanceNotFoundException e) {
      SimpleLogger.log(
          "No Instance matching " + this.interlokInstance.getInstanceProperties().getProperty(INTERLOK_INSTANCE_ID) + " found");
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      closeQuietly(jmxc);
      isRunning = false;
    }
  }

  private boolean cancelPendingStartup() {
    if (instanceHandle != null) {
      return this.instanceHandle.cancel(true);
    }    
    return false;
  }
  
  private JMXConnector connect(String jmxServiceURL) throws Exception {
    if (jmxServiceURL != null) {
      JMXServiceURL url = new JMXServiceURL(jmxServiceURL);
      JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
      return jmxc;
    }
    return null;
  }

  private void closeQuietly(JMXConnector jmxc) {
    try {
      if (jmxc != null) {
        jmxc.close();
      }
    }
    catch (Exception e) {
    }
  }
}
