package com.adaptris.management.aar;


import java.net.URL;
import java.net.URLClassLoader;

public class ChildFirstClassloader extends URLClassLoader {

  public ChildFirstClassloader(URL[] urls) {
    super(urls);
  }

  public ChildFirstClassloader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  public void addURL(URL url) {
    super.addURL(url);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public Class loadClass(String name) throws ClassNotFoundException {
    return loadClass(name, false);
  }

  /**
   * We override the parent-first behavior established by
   * java.lang.Classloader.
   * 
   * The implementation is surprisingly straightforward.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {

    // First, check if the class has already been loaded
    Class c = findLoadedClass(name);

    // if not loaded, search the local (child) resources
    if (c == null) {
      try {
        c = findClass(name);
      } catch (ClassNotFoundException cnfe) {
        // ignore
      }
    }

    // if we could not find it, delegate to parent
    // Note that we don't attempt to catch any ClassNotFoundException
    if (c == null) {
      if (getParent() != null) {
        c = getParent().loadClass(name);
      } else {
        c = getSystemClassLoader().loadClass(name);
      }
    }

    if (resolve) {
      resolveClass(c);
    }

    return c;
  }

}
