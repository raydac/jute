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
import java.util.List;

/**
 * Common runner base class.
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
abstract class AbstractRunner {

  public static Class<? extends Annotation> findAnnotationClass(final String className) {
    try {
      Class<?> result = Class.forName(className);
      if (!Annotation.class.isAssignableFrom(result)) {
        result = null;
      }
      return (Class<? extends Annotation>) result;
    }
    catch (ClassNotFoundException ex) {
      return null;
    }
  }

  public static Class<?> findClass(final String className) {
    try {
      return Class.forName(className);
    }
    catch (ClassNotFoundException ex) {
      return null;
    }
  }

  public static boolean execBoolean(final Object obj, final String method) {
    try {
      return (Boolean) obj.getClass().getMethod(method).invoke(obj);
    }
    catch (Throwable thr) {
      throw new Error("Error execBoolean(" + method + ')');
    }
  }

  public static String execString(final Object obj, final String method) {
    try {
      return (String) obj.getClass().getMethod(method).invoke(obj);
    }
    catch (Throwable thr) {
      throw new Error("Error execString(" + method + ')');
    }
  }

  public static List<?> execList(final Object obj, final String method) {
    try {
      return (List<?>) obj.getClass().getMethod(method).invoke(obj);
    }
    catch (Throwable thr) {
      throw new Error("Error execList(" + method + ')');
    }
  }

}
