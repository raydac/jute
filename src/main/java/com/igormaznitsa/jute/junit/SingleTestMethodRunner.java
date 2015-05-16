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
package com.igormaznitsa.jute.junit;

import org.junit.runner.*;
import org.junit.runner.notification.Failure;

/**
 * Runner to start single test method with JUnit.
 * 
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
public final class SingleTestMethodRunner {

  /**
   * Execute method provided as the first argument in format class_name#method_name
   * @param args command line arguments
   * @throws ClassNotFoundException thrown if test class not found
   */
  public static void main(final String... args) throws ClassNotFoundException {
    if (args == null || args.length == 0){
      System.err.println("No provided test method name");
      System.exit(999);
    }
    
    Result result = null;
    try {
      final String[] classAndMethod = args[0].split("#");
      final Request request = Request.method(Class.forName(classAndMethod[0]), classAndMethod[1]);
      result = new JUnitCore().run(request);
    }
    catch (Throwable thr) {
      thr.printStackTrace(System.err);
    }

    if (result == null) {
      System.exit(2);
    }
    else if (result.wasSuccessful()) {
      System.exit(0);
    }
    else {
      for (final Failure f : result.getFailures()) {
        System.err.println(f.getMessage());
        System.err.println(f.getTrace());
      }
      System.exit(1);
    }
  }
}
