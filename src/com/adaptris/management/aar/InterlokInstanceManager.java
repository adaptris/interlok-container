package com.adaptris.management.aar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InterlokInstanceManager {
  
  private ScheduledExecutorService scheduler;
  
  private ScheduledFuture<?> instanceHandle;
  
  private InterlokInstance interlokInstance;
  
  public InterlokInstanceManager(String instanceName) {
    interlokInstance = new InterlokInstance();
    this.interlokInstance.setInstanceName(instanceName);
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
    
  }

}
