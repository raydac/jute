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
package com.igormaznitsa.jute.annotations;

import java.lang.annotation.*;

/**
 * Annotation shows to JUte that method or all methods of the class should be in scope of JUte.
 * @since 1.0.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface JUteTest {
  /**
   * Java interpreter for the test. Can be path or command.
   * @return empty string if external parameter should be used
   */
  String jvm() default "";
  /**
   * Array contains options for JVM to be used for test start.
   * @return array of jvm options as strings
   */
  String[] jvmOpts() default {};
  /**
   * String to be provided to started process through System.in.
   * @return empty string if external parameter should be used
   */
  String in() default "";
  /**
   * Order of test. Tests with similar order parameters will be started in parallel.
   * @return negative number if default order should be used
   */
  int order() default -1;
  /**
   * Enforce output of process consoles into log.
   * @return false by default if external parameter should be used
   */
  boolean enforceOut() default false;
  /**
   * Skip the test.
   * @return true if the test must be skipped
   */
  boolean skip() default false;
  /**
   * Timeout for the test process.
   * @return timeout in milliseconds for started process, zero or negative number if external parameter should be used
   */
  long timeout() default 0L;
}
