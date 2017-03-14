/*
 * Copyright 2015 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adaptris.management.aar;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;

class ClasspathInitialiser {


  private static final String CLASSPATH_KEY = "java.class.path";
  private transient Collection<File> currentClasspath;
  private transient static ClasspathInitialiser INSTANCE = null;

  private ClasspathInitialiser() {
    currentClasspath = getCurrentClassPath();
  }

  public static ClasspathInitialiser load(String directory) {
    if (INSTANCE == null) {
      INSTANCE = new ClasspathInitialiser();
      try {
        INSTANCE.add(new File("./config"));
        for (File f : INSTANCE.getJars(directory)) {
          INSTANCE.add(f);
        }
        INSTANCE.setSystemClasspath();
      } catch (Exception e) {
        SimpleLogger.log("Failed to initialise, forced exit()", e);
        System.exit(1);
      }
    }
    return INSTANCE;
  }

  private String getLoadedClasspath() {
    StringBuffer sb = new StringBuffer();
    for (File element : currentClasspath) {
      sb.append(element);
      sb.append(File.pathSeparator);
    }
    return sb.toString();
  }

  private void setSystemClasspath() {
    System.setProperty(CLASSPATH_KEY, getLoadedClasspath());
  }

  private void add(File file) throws Exception {
    URLClassLoader urlLoader = getUrlClassLoader();
    Method method = getAddMethod();
    if (urlLoader != null) {
      if (!currentClasspath.contains(file.getCanonicalPath())) {
        SimpleLogger.log("(Info) ClasspathInitialiser.load: " + file.getCanonicalPath());
        method.invoke(urlLoader, new Object[] {file.toURI().toURL()});
        currentClasspath.add(file);
      }
    }
  }

  private Method getAddMethod() throws Exception {
    Method addUrlMethod = null;
    addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] {java.net.URL.class});

    addUrlMethod.setAccessible(true);
    return addUrlMethod;
  }


  private URLClassLoader getUrlClassLoader() {

    ClassLoader urlLoader = ContainerBootstrap.class.getClassLoader();
    return urlLoader instanceof URLClassLoader ? (URLClassLoader) urlLoader : null;
  }

  private Collection<File> getCurrentClassPath() {
    ArrayList<File> result = new ArrayList<>();
    StringTokenizer st = new StringTokenizer(System.getProperty(CLASSPATH_KEY), File.pathSeparator);
    while (st.hasMoreElements()) {
      try {
        File file = new File((String) st.nextElement());
        if (!result.contains(file.getCanonicalPath())) {
          result.add(file);
          SimpleLogger.log("(Info) StandardBootstrap.getCurrentClassPath: " + file.getCanonicalPath());
        }
      } catch (Exception e) {
        ;
      }
    }
    return result;
  }

  private Collection<File> getJars(String dir) {

    ArrayList<File> jars = new ArrayList<>();
    File file = new File(dir);
    if (file.exists()) {
      jars.addAll(Arrays.asList(file.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String fileName) {
          boolean result = false;
          if (fileName.endsWith(".jar") || fileName.endsWith(".zip"))
            result = true;
          return result;
        }
      })));
    }
    return jars;
  }
}
