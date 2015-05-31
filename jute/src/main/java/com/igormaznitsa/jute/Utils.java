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
package com.igormaznitsa.jute;

import java.util.*;

public abstract class Utils {

  private Utils() {
  }

  public static final String lineSeparator = System.getProperty("line.separator", "\r\n");

  public static int getMaxLineWidth(final List<String> str) {
    int max = 0;
    if (str != null && !str.isEmpty()) {
      for (final String s : str) {
        if (s != null && s.length() > max) {
          max = s.length();
        }
      }
    }
    return max;
  }

  public static List<String> splitToLines(final String str) {
    if (str == null || str.length() == 0) {
      return Collections.<String>emptyList();
    }

    final String[] strarray = str.split("\\n");
    for (int i = 0; i < strarray.length; i++) {
      strarray[i] = strarray[i].replace("\t", "    ");
    }

    return Arrays.asList(strarray);
  }

  public static int calcNumberOfItems(final Map<TestClassProcessor, List<TestContainer>> map) {
    int counter = 0;
    for (final Map.Entry<TestClassProcessor, List<TestContainer>> e : map.entrySet()) {
      counter += e.getValue().size();
    }
    return counter;
  }


}
