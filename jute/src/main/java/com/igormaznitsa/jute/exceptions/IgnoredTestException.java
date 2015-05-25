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
package com.igormaznitsa.jute.exceptions;

import com.igormaznitsa.jute.TestContainer;

public class IgnoredTestException extends Exception {
  private static final long serialVersionUID = -9073177039882736578L;
  
  private final TestContainer container;
  
  public IgnoredTestException(final TestContainer test){
    this.container = test;
  }
  
  public TestContainer getTest(){
    return this.container;
  }
}
