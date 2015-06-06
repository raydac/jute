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
 * Flag annotation for JUTe tests. Can be placed as for test method as for whole
 * class.
 *
 * @since 1.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface JUteTest {

  /**
   * Java interpreter to be used for the test execution. Can be a file path or a
   * command. For instance 'java' or '/usr/bin/java'
   *
   * @return either full path to JVM interpreter or a command
   */
  String jvm() default "";

  /**
   * Array contains options for JVM to be used for test start. For instance '-Dsome.property=HelloWorld'
   *
   * @return array of jvm options as strings
   */
  String[] jvmOpts() default {};

  /**
   * String to be provided to started process through System.in.
   *
   * @return string to be read by process through System.in
   */
  String in() default "";

  /**
   * Order of test processing. Test methods with the same non-negative order
   * will be started in parallel.
   *
   * @return order number of the test
   */
  int order() default -1;

  /**
   * Enforce print the test console output even if there was not any error. By default console logs are not printed into log if there was not any error.
   *
   * @return flag to print test console output into log
   */
  boolean printConsole() default false;

  /**
   * Skip the test.
   *
   * @return true if the test must be skipped
   */
  boolean skip() default false;

  /**
   * Timeout for the test process.
   *
   * @return timeout in milliseconds for started process, if it is either zero or
   * non-positive value then ignored.
   */
  long timeout() default 0L;
}
