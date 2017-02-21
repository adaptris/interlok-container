package com.adaptris.management.aar;

import static com.adaptris.management.aar.Constants.ARCHIVE_PATH_KEY;
import static com.adaptris.management.aar.Constants.GLOBAL_LIB_PATH_KEY;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;

public class ArchiveInstanceRunner extends Thread {
  
  private static Object instanceRunnerMonitor = new Object();
  
  private static final String DEFAULT_INVOKED_METHODS = "logVersionInformation,standardBoot";
  
  private static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";
  
  private Properties containerProperties;
    
  private InterlokInstance interlokInstance;
  
  private URLClassLoader classLoader;
  
  public ArchiveInstanceRunner(String instanceName, Properties containerProperties) {
    interlokInstance = new InterlokInstance();
    interlokInstance.setInstanceName(instanceName);
    this.containerProperties = containerProperties;
  }
  
  public ArchiveInstanceRunner(InterlokInstance interlokInstance, Properties containerProperties) {
    this.interlokInstance = interlokInstance;
    this.containerProperties = containerProperties;
  }
  
  public InterlokInstance startInstance() throws Exception {
    File aarDirectory = new File(containerProperties.getProperty(ARCHIVE_PATH_KEY));
    interlokInstance.setInstanceProperties(PropertiesHelper.loadFromFile(new File(new File(aarDirectory, interlokInstance.getInstanceName()), BOOTSTRAP_PROPERTIES).getAbsolutePath()));
    
    this.start();
    return interlokInstance;
  }
  
  public void run() {
    try {
      synchronized(instanceRunnerMonitor) {
        Thread.currentThread().setName(interlokInstance.getInstanceName());
        
        File aarDirectory = new File(containerProperties.getProperty(ARCHIVE_PATH_KEY));
        PropertiesHelper.verifyProperties(interlokInstance.getInstanceProperties(), Constants.INSTANCE_MAIN_CLASS);
        
        URL[] classpathJars = this.getDirectoryJars(
            new File(containerProperties.getProperty(GLOBAL_LIB_PATH_KEY)).getAbsolutePath(),
            new File(new File(aarDirectory, interlokInstance.getInstanceName()), "lib").getAbsolutePath()
        );
              
        classLoader = new URLClassLoader(classpathJars, this.getClass().getClassLoader());
        Class<?> standardBootstrapClass = classLoader.loadClass(interlokInstance.getInstanceProperties().getProperty(Constants.INSTANCE_MAIN_CLASS));
        Constructor<?> bootstrapConstructor = standardBootstrapClass.getConstructor(String[].class);
        
        File aarInstDir = new File(aarDirectory, interlokInstance.getInstanceName());
        File bootstrapPropertiesFile = new File(aarInstDir, BOOTSTRAP_PROPERTIES);
        Object standardBootstrapInst = bootstrapConstructor.newInstance(new Object[] {new String[] {bootstrapPropertiesFile.getAbsolutePath()}});
        
        Thread.currentThread().setContextClassLoader(classLoader);
        
        invokeInstanceBootstrap(interlokInstance.getInstanceProperties(), standardBootstrapClass, standardBootstrapInst);
        
        registerShutdownHook();
      }
      
    } catch (Exception e) {
      System.out.println("Failed to start archive instance.");
      e.printStackTrace();
    }
  }

  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
          if(classLoader != null) {
//            try {
              // Don't want to do this without knowing all instances have closed first.
              // Otherwise the shutdown handler in each instance may not be able to load required inner classes because the class loader is now closed.
              // We obviously don;t know which shutdown hook will run first; this one or the one for the instance.
//              classLoader.close();
//            } catch (IOException e) {
//              // silent.
//            }
          }
      }
  });
    
  }

  private void invokeInstanceBootstrap(Properties instanceProperties, Class<?> standardBootstrapClass, Object standardBootstrapInst) throws Exception {
    String[] methods = (instanceProperties.containsKey(Constants.INSTANCE_INVOKED_METHODS) ? instanceProperties.getProperty(Constants.INSTANCE_INVOKED_METHODS) : DEFAULT_INVOKED_METHODS).split(",");
    
    for(String method : methods) {
      Class<?> invokableStandardBootstrapClass = standardBootstrapClass;
      
      boolean done = false;
      while(!done) {
        try {
          if(invokableStandardBootstrapClass != null) {
            Method invokableMethod = invokableStandardBootstrapClass.getDeclaredMethod(method, new Class[0]);
            invokableMethod.setAccessible(true);
            invokableMethod.invoke(standardBootstrapInst, new Object[0]);
            done = true;
          }
          else
            throw new Exception("Cannot execute method: " + method);
        } catch (NoSuchMethodException ex) {
          invokableStandardBootstrapClass = invokableStandardBootstrapClass.getSuperclass();
        }
      }
    }
  }
  
  private URL[] getDirectoryJars(String... libDirectories) throws Exception {
    ArrayList<URL> urls = new ArrayList<URL>();

    for(String libDirectory : libDirectories) {
      File dir = new File(libDirectory);
      if (dir.exists()) {
        String[] fileNames = dir.list(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String fileName) {
            boolean result = false;
            if (fileName.endsWith(".jar") || fileName.endsWith(".zip")) 
              result = true;
            return result;
          }
        });
  
        if (fileNames == null)
          System.out.println("No jars found in [" + dir.getCanonicalPath() + "]");
        else {  
          for (int i = 0; i < fileNames.length; i++) {
            File file = new File(dir.getPath() + File.separator + fileNames[i]);
            urls.add(file.toURI().toURL());
          }
        }
      }
    }

    return urls.toArray(new URL[urls.size()]);
  }

}
