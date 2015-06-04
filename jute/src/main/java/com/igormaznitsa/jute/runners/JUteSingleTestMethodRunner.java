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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Runner to start single test method.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 * @since 1.1.0
 */
public final class JUteSingleTestMethodRunner extends AbstractRunner {

  /**
   * Execute method provided as the first argument in format
   * class_name#method_name
   *
   * @param args command line arguments
   */
  public static void main(final String... args) {
    final Class<? extends Annotation> CLASS_JUNIT_AFTER = findAnnotationClass("org.junit.After");
    final Class<? extends Annotation> CLASS_JUNIT_AFTER_CLASS = findAnnotationClass("org.junit.AfterClass");
    final Class<? extends Annotation> CLASS_JUNIT_BEFORE = findAnnotationClass("org.junit.Before");
    final Class<? extends Annotation> CLASS_JUNIT_BEFORE_CLASS = findAnnotationClass("org.junit.BeforeClass");

    if (args == null || args.length == 0) {
      System.err.println("No provided test method name");
      System.exit(999);
    }

    try {
      final String[] classAndMethod = args[0].split("#");
      final Class theKlazz = Class.forName(classAndMethod[0]);

      final Method testMethod = theKlazz.getMethod(classAndMethod[1]);
      final Object theKlazzInstance = theKlazz.newInstance();

      final List<Method> beforeClass = collectMethodsForFlagAnnotation(theKlazz, CLASS_JUNIT_BEFORE_CLASS);
      final List<Method> afterClass = collectMethodsForFlagAnnotation(theKlazz, CLASS_JUNIT_AFTER_CLASS);
      final List<Method> beforeTest = collectMethodsForFlagAnnotation(theKlazz, CLASS_JUNIT_BEFORE);
      final List<Method> afterTest = collectMethodsForFlagAnnotation(theKlazz, CLASS_JUNIT_AFTER);

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

  private static List<Method> collectMethodsForFlagAnnotation(final Class klazz, final Class<? extends Annotation> flagAnnotation) {
    if (flagAnnotation == null || klazz == java.lang.Object.class) {
      return Collections.<Method>emptyList();
    }
    final List<Method> result = new ArrayList<Method>();
    result.addAll(collectMethodsForFlagAnnotation(klazz.getSuperclass(), flagAnnotation));
    for (final Method m : klazz.getDeclaredMethods()) {
      if (Modifier.isAbstract(m.getModifiers()) || !Modifier.isStatic(m.getModifiers())) {
        continue;
      }
      m.setAccessible(true);
      final Object attr = m.getAnnotation(flagAnnotation);
      if (attr != null) {
        result.add(m);
      }
    }
    return result;
  }
  
}
