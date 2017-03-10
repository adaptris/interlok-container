package com.adaptris.management.aar;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchiveWatcher {

  private List<InterlokInstance> instanceList;
  
  private String directoryToWatch;
  
  private Map<String, InterlokInstanceManager> scheduledStartups;
  
  private transient boolean stopped = false;
  private transient Thread monitorThread;
  
  public ArchiveWatcher() {
    instanceList = new ArrayList<InterlokInstance>();
    scheduledStartups = new HashMap<String, InterlokInstanceManager>();
  }

  public void start() {
    stopped = false;
    loadAlreadyStartedInstances();
    monitorThread = createThread();
    monitorThread.start();
  }

  private void loadAlreadyStartedInstances() {
    for(InterlokInstance instance : this.getInstanceList()) {
      scheduledStartups.put(instance.getInstanceName(), new InterlokInstanceManager(instance));
    }
  }

  public void stop() {
    stopped = true;
    if(monitorThread != null)
      monitorThread.interrupt();
  }

  private Thread createThread() {
    return new Thread("ArchiveDirectoryWatcherThread") {
      
      @SuppressWarnings("unchecked")
      public void run() {
        try {
          WatchService watcher = FileSystems.getDefault().newWatchService();
          File directory = new File(getDirectoryToWatch());
          Path dir = Paths.get(directory.getCanonicalPath());
          
          dir.register(watcher, 
              StandardWatchEventKinds.ENTRY_CREATE, 
              StandardWatchEventKinds.ENTRY_MODIFY, 
              StandardWatchEventKinds.ENTRY_DELETE);
          
          while(!stopped) {            
            WatchKey key;
            try {
              key = watcher.take();
            } catch (InterruptedException x) {
              return;
            }
            
            for (WatchEvent<?> event : key.pollEvents()) {
              WatchEvent.Kind<?> kind = event.kind();
      
              WatchEvent<Path> ev = (WatchEvent<Path>)event;
              Path filename = ev.context();
              
              if(kind == StandardWatchEventKinds.ENTRY_CREATE) {     
                SimpleLogger.log("New instance: " + filename.toString() + " detected.");
                InterlokInstance interlokInstance = this.scheduleNewLaunch(new File(directory, filename.toString()));
                getInstanceList().add(interlokInstance);
                                
              } else if(kind == StandardWatchEventKinds.ENTRY_DELETE) {
                SimpleLogger.log("Removing instance - " + filename.toString());
                this.scheduleShutdown(new File(directory, filename.toString()));
                
              } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                SimpleLogger.log("Modifying instance - " + filename.toString());
                if(scheduledStartups.containsKey(filename.toString()))
                  scheduledStartups.get(filename.toString()).delayStartup();
              } else
                continue;
            }
            
            boolean valid = key.reset();
            if (!valid)
              break;
            
          }
        }
        catch (Exception x) {
          x.printStackTrace();
        }
      }

      private void scheduleShutdown(File file) {
        scheduledStartups.get(file.getName()).shutdown();
      }

      private InterlokInstance scheduleNewLaunch(File file) {
        InterlokInstanceManager launchScheduler = new InterlokInstanceManager(file.getName());
        InterlokInstance interlokInstance = launchScheduler.scheduleStartup();
        scheduledStartups.put(file.getName(), launchScheduler);
        
        return interlokInstance;
      }

    };
  }
  
  public String getDirectoryToWatch() {
    return directoryToWatch;
  }

  public void setDirectoryToWatch(String directoryToWatch) {
    this.directoryToWatch = directoryToWatch;
  }

  public List<InterlokInstance> getInstanceList() {
    return instanceList;
  }

  public void setInstanceList(List<InterlokInstance> instanceList) {
    this.instanceList = instanceList;
  }
  
}
