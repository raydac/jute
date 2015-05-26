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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import org.junit.*;

/**
 * Runner to start single test method.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 * @since 1.1.0
 */
public final class JUteSingleTestMethodRunner {

  /**
   * Execute method provided as the first argument in format
   * class_name#method_name
   *
   * @param args command line arguments
   * @throws ClassNotFoundException thrown if test class not found
   */
  public static void main(final String... args) throws ClassNotFoundException {
    if (args == null || args.length == 0) {
      System.err.println("No provided test method name");
      System.exit(999);
    }

    try {
      final String[] classAndMethod = args[0].split("#");
      final Class theKlazz = Class.forName(classAndMethod[0]);

      final Method testMethod = theKlazz.getMethod(classAndMethod[1]);
      final Object theKlazzInstance = theKlazz.newInstance();

      final List<Method> beforeClass = collectBeforeClassMethods(theKlazz);
      final List<Method> afterClass = collectAfterClassMethods(theKlazz);
      final List<Method> beforeTest = collectBeforeMethods(theKlazz);
      final List<Method> afterTest = collectAfterMethods(theKlazz);

      boolean error = false;
      if (!executeMethodList(null, beforeClass, true)) {
        error = true;
      }
      try {
        if (!error) {
          try {
            if (executeMethodList(theKlazzInstance, beforeTest, true)) {
              testMethod.invoke(theKlazzInstance);
            }
            else {
              error = true;
            }
          }
          finally {
            if (!executeMethodList(theKlazzInstance, afterTest, false)) {
              error = true;
            }
          }
        }
      }
      finally {
        if (!executeMethodList(null, afterClass, false) || error) {
          System.exit(1);
        }
      }

      System.exit(0);
    }
    catch (Throwable thr) {
      thr.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static boolean executeMethodList(final Object instance, final List<Method> methods, final boolean breakForError) {
    boolean noerrors = true;
    for (final Method m : methods) {
      try {
        m.invoke(instance);
      }
      catch (Throwable thr) {
        noerrors = false;
        thr.printStackTrace(System.err);
        if (breakForError) {
          break;
        }
      }
    }
    return noerrors;
  }

  private static List<Method> collectBeforeMethods(final Class klazz) {
    if (klazz == java.lang.Object.class) {
      return Collections.<Method>emptyList();
    }
    final List<Method> result = new ArrayList<Method>();
    result.addAll(collectBeforeClassMethods(klazz.getSuperclass()));
    for (final Method m : klazz.getDeclaredMethods()) {
      if (Modifier.isAbstract(m.getModifiers())) {
        continue;
      }
      m.setAccessible(true);
      final Before attr = m.getAnnotation(Before.class);
      if (attr != null) {
        result.add(m);
      }
    }
    return result;
  }

  private static List<Method> collectAfterMethods(final Class klazz) {
    if (klazz == java.lang.Object.class) {
      return Collections.<Method>emptyList();
    }
    final List<Method> result = new ArrayList<Method>();
    result.addAll(collectBeforeClassMethods(klazz.getSuperclass()));
    for (final Method m : klazz.getDeclaredMethods()) {
      if (Modifier.isAbstract(m.getModifiers())) {
        continue;
      }
      m.setAccessible(true);
      final After attr = m.getAnnotation(After.class);
      if (attr != null) {
        result.add(m);
      }
    }
    return result;
  }

  private static List<Method> collectBeforeClassMethods(final Class klazz) {
    if (klazz == java.lang.Object.class) {
      return Collections.<Method>emptyList();
    }
    final List<Method> result = new ArrayList<Method>();
    result.addAll(collectBeforeClassMethods(klazz.getSuperclass()));
    for (final Method m : klazz.getDeclaredMethods()) {
      if (Modifier.isAbstract(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
        continue;
      }
      m.setAccessible(true);
      final BeforeClass attr = m.getAnnotation(BeforeClass.class);
      if (attr != null) {
        result.add(m);
      }
    }
    return result;
  }

  private static List<Method> collectAfterClassMethods(final Class klazz) {
    if (klazz == java.lang.Object.class) {
      return Collections.<Method>emptyList();
    }
    final List<Method> result = new ArrayList<Method>();
    result.addAll(collectBeforeClassMethods(klazz.getSuperclass()));
    for (final Method m : klazz.getDeclaredMethods()) {
      if (Modifier.isAbstract(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
        continue;
      }
      m.setAccessible(true);
      final AfterClass attr = m.getAnnotation(AfterClass.class);
      if (attr != null) {
        result.add(m);
      }
    }
    return result;
  }

}
