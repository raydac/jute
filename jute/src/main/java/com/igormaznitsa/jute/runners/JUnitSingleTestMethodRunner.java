/* 
 * Copyright 2015 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.jute.runners;

import java.lang.reflect.InvocationTargetException;

/**
 * Runner to start single test method with JUnit.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
public final class JUnitSingleTestMethodRunner extends AbstractRunner {

  /**
   * Execute method provided as the first argument in format
   * class_name#method_name
   *
   * @param args command line arguments
   */
  public static void main(final String... args) {
    final Class<?> CLASS_JUNIT_REQUEST = findClass("org.junit.runner.Request");
    final Class<?> CLASS_JUNIT_CORE = findClass("org.junit.runner.JUnitCore");
    final Class<?> CLASS_JUNIT_RESULT = findClass("org.junit.runner.Result");
    final Class<?> CLASS_JUNIT_FAILURE = findClass("org.junit.runner.notification.Failure");

    if (CLASS_JUNIT_CORE == null || CLASS_JUNIT_FAILURE == null || CLASS_JUNIT_REQUEST == null || CLASS_JUNIT_RESULT == null) {
      System.err.println("Can't find needed JUnit classes, either JUnit is not provided or incompatible version!");
      System.exit(1);
    }

    if (args == null || args.length == 0) {
      System.err.println("No provided test method name");
      System.exit(999);
    }
    Object result = null;
    try {
      final String[] classAndMethod = args[0].split("#");
      final Object requestObject = CLASS_JUNIT_REQUEST.getMethod("method", Class.class, String.class).invoke(null, Class.forName(classAndMethod[0]), classAndMethod[1]);
      result = CLASS_JUNIT_CORE.getMethod("run", CLASS_JUNIT_REQUEST).invoke(CLASS_JUNIT_CORE.newInstance(), requestObject);
    }
    catch (Throwable thr) {
      thr.printStackTrace();
      if (thr instanceof InvocationTargetException) {
        final Throwable cause = thr.getCause();
        if (cause == null) {
          thr.printStackTrace(System.err);
        }
        else {
          cause.printStackTrace(System.err);
        }
      }
      else {
        thr.printStackTrace(System.err);
      }
    }

    if (result == null) {
      System.exit(2);
    }
    else if (execBoolean(result, "wasSuccessful")) {
      System.exit(0);
    }
    else {
      for (final Object f : execList(result, "getFailures")) {
        System.err.println(execString(f, "getMessage"));
        System.err.println(execString(f, "getTrace"));
      }
      System.exit(1);
    }
  }
}
