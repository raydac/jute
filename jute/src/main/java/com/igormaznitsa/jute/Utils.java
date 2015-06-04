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
import org.apache.commons.io.FilenameUtils;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public abstract class Utils {

  private static final PeriodFormatter TIME_FORMATTER = new PeriodFormatterBuilder()
          .printZeroAlways()
          .minimumPrintedDigits(2)
          .appendHours().appendSeparator(":")
          .appendMinutes().appendSeparator(":")
          .appendSeconds().appendSeparator(".")
          .minimumPrintedDigits(3)
          .appendMillis().toFormatter();

  private Utils() {
  }

  public static final String lineSeparator = System.getProperty("line.separator", "\r\n");

  public static boolean checkClassAndMethodForPattern(final String juteTest, final String className, final String methofName, final boolean onlyClass) {
    if (className == null || (!onlyClass && methofName == null)) {
      return false;
    }
    if (juteTest == null || juteTest.isEmpty()) {
      return true;
    }
    final String classPattern;
    final String methodPattern;
    final int methodPrefix = juteTest.indexOf('#');
    if (methodPrefix < 0) {
      classPattern = juteTest;
      methodPattern = "*";
    }
    else {
      classPattern = juteTest.substring(0, methodPrefix);
      methodPattern = juteTest.substring(methodPrefix + 1);
    }
    return FilenameUtils.wildcardMatch(className, classPattern) && (onlyClass ? true : FilenameUtils.wildcardMatch(methofName, methodPattern));
  }

  public static String printTimeDelay(final long timeInMilliseconds) {
    final Duration duration = new Duration(timeInMilliseconds);
    final Period period = duration.toPeriod().normalizedStandard(PeriodType.time());
    return TIME_FORMATTER.print(period);
  }

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

  public static String makeStr(final int len, final char ch) {
    final StringBuilder result = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      result.append(ch);
    }
    return result.toString();
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

  public static List<TestContainer> sortDetectedClassMethodsForNameAndOrder(final List<TestContainer> testMethods) {
    Collections.sort(testMethods, new Comparator<TestContainer>() {
      @Override
      public int compare(final TestContainer o1, final TestContainer o2) {
        final int order1 = o1.getOrder();
        final int order2 = o2.getOrder();

        if (order1 < 0 && order2 < 0) {
          return 0;
        }

        final String name1 = o1.getMethodName();
        final String name2 = o2.getMethodName();

        final int result;
        if (order1 == order2) {
          result = name1.compareTo(name2);
        }
        else {
          result = Integer.compare(order1, order2);
        }
        return result;
      }
    });
    return testMethods;
  }

  public static String toStandardJavaClassName(final String rootPath, final String classFilePath) {
    if (rootPath.length() >= classFilePath.length()) {
      throw new Error("Unexpected situation #112321223 (" + rootPath + " >= " + classFilePath + ')');
    }
    String diff = classFilePath.substring(rootPath.length()).replace('\\', '.').replace('/', '.');
    if (diff.toLowerCase(Locale.ENGLISH).endsWith(".class")) {
      diff = diff.substring(0, diff.length() - ".class".length());
    }
    return diff;
  }
}
