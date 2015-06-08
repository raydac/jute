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
package com.igormaznitsa.jute.it;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.maven.it.VerificationException;
import static org.junit.Assert.*;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public abstract class AbstractJUteITTest {

  protected static final String PROJECT_VERSION = System.getProperty("maven.jute.version", "unknown");

  protected File getTestFolder(final String name) throws IOException {
    final File result = ResourceExtractor.simpleExtractResources(this.getClass(), name);
    assertTrue(result.isDirectory());
    return result;
  }

  protected Verifier verify(final String testName, final boolean debug) throws Exception {
    final Verifier ver = new Verifier(getTestFolder(testName).getAbsolutePath(), debug);
    ver.executeGoals(Arrays.asList("compile", "test"), Collections.singletonMap("maven.jute.version", PROJECT_VERSION));
    return ver;
  }

  protected Verifier verify(final String testName, final boolean debug, final Map<String,String> properties) throws Exception {
    final Verifier ver = new Verifier(getTestFolder(testName).getAbsolutePath(), debug);
    for(final Map.Entry<String,String> p : properties.entrySet()){
      ver.getSystemProperties().setProperty(p.getKey(), p.getValue());
    }
    ver.executeGoals(Arrays.asList("compile", "test"), Collections.singletonMap("maven.jute.version", PROJECT_VERSION));
    return ver;
  }

  protected Verifier verifyWithExpectedError(final String testName, final boolean debug) throws Exception {
    final Verifier ver = new Verifier(getTestFolder(testName).getAbsolutePath(), debug);
    try {
      ver.executeGoals(Arrays.asList("compile", "test"), Collections.singletonMap("maven.jute.version", PROJECT_VERSION));
    }
    catch (VerificationException ex) {
      return ver;
    }
    fail("There was not verify error");
    return null;
  }

  protected static String list2Str(final List<String> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    final StringBuilder buffer = new StringBuilder();
    for (final String s : list) {
      buffer.append(s).append('\n');
    }
    return buffer.toString();
  }

  protected static void assertEmpty(final List<String> log) {
    assertEquals("Must be single line", 1, log.size());
    assertEquals("[INFO] ", log.get(0));
  }

  protected static List<String> extractJuteSection(final Verifier ver) throws IOException {
    final List<String> log = ver.loadLines(ver.getLogFileName(), Charset.defaultCharset().name());
    final List<String> result = new ArrayList<String>();

    boolean juteSection = false;

    for (final String s : log) {
      if (juteSection) {
        if (s.startsWith("[INFO] Tests run:") || s.startsWith("[INFO] --- ")) {
          juteSection = false;
          if (s.contains("Tests")){
            result.add(s);
          }
        }
        else {
          result.add(s);
        }
      }
      else {
        if (s.contains("--- jute:" + PROJECT_VERSION + ":jute")) {
          juteSection = true;
        }
      }
    }
    return result;
  }

  protected static List<String> extractJUnitSection(final Verifier ver) throws IOException {
    final List<String> log = ver.loadLines(ver.getLogFileName(), Charset.defaultCharset().name());
    final List<String> result = new ArrayList<String>();

    boolean junitSection = false;

    for (final String s : log) {
      if (junitSection) {
        if (s.startsWith("Tests run:") || s.startsWith("[INFO] --- ")) {
          if (s.startsWith("Tests run")){
            result.add(s);
          }
          junitSection = false;
        }
        else {
          result.add(s);
        }
      }
      else {
        if (s.contains("--- maven-surefire-plugin:")) {
          junitSection = true;
        }
      }
    }
    return result;
  }

  protected void assertPatternOrder(final List<String> text, final String... regex) {
    int index = 0;
    Pattern nextPattern = Pattern.compile(regex[index++]);
    for (final String s : text) {
      if (nextPattern.matcher(s).find()) {
        if (index >= regex.length) {
          nextPattern = null;
          break;
        }
        nextPattern = Pattern.compile(regex[index++]);
      }
    }
    assertNull("Error order for " + nextPattern, nextPattern);
  }

  protected static void assertPattern(final String regex, final List<String> list) {
    final Pattern pattern = Pattern.compile(regex);
    for (final String s : list) {
      if (pattern.matcher(s).find()) {
        return;
      }
    }
    for (final String line : list) {
      System.err.println(line);
    }
    System.err.flush();
    fail("Not found pattern: " + pattern.pattern());
  }

  protected static void assertNoPattern(final String regex, final List<String> list) {
    final Pattern pattern = Pattern.compile(regex);
    for (final String s : list) {
      if (pattern.matcher(s).find()) {
        for (final String line : list) {
          System.err.println(line);
        }
        fail("Detected pattern : " + pattern.pattern());
      }
    }
  }

  private static String glue(final List<String> str) {
    final StringBuilder result = new StringBuilder();
    for (final String s : str) {
      result.append(s).append(System.lineSeparator());
    }
    return result.toString();
  }
}
