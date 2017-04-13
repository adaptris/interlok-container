package com.adaptris.management.aar;

class SimpleLogger {
  static final boolean DBG = Boolean.getBoolean("adp.bootstrap.debug") || Boolean.getBoolean("interlok.bootstrap.debug");

  static void log(String s) {
    if (DBG) System.err.println("(Container): " + s);
  }

  static void log(String s, Exception e) {
    System.err.println("(Container): " + s);
    e.printStackTrace();
  }
}
