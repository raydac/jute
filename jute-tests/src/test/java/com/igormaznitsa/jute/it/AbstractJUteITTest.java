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
import static org.junit.Assert.*;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public abstract class AbstractJUteITTest {

  protected static final String PROJECT_VERSION = System.getProperty("maven.project.version", "unknown");

  protected File getTestFolder(final String name) throws IOException {
    final File result = ResourceExtractor.simpleExtractResources(this.getClass(), name);
    assertTrue(result.isDirectory());
    return result;
  }

  protected Verifier verify(final String testName, final boolean debug) throws Exception {
    final Verifier ver = new Verifier(getTestFolder(testName).getAbsolutePath(), debug);
    ver.executeGoals(Arrays.asList("compile", "test"));
    return ver;
  }

  protected static String list2Str(final List<String> list) {
    if (list==null || list.isEmpty()) return "";
    final StringBuilder buffer = new StringBuilder();
    for(final String s : list){
      buffer.append(s).append('\n');
    }
    return buffer.toString();
  }
  
  protected static List<String> extractJuteSection(final Verifier ver) throws IOException {
    final List<String> log = ver.loadLines(ver.getLogFileName(), Charset.defaultCharset().name());
    final List<String> result = new ArrayList<String>();

    boolean juteSection = false;

    for (final String s : log) {
      if (juteSection) {
        if (s.startsWith("[INFO] Tests run:")) {
          juteSection = false;
        }
        result.add(s);
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
        if (s.startsWith("Tests run:")) {
          junitSection = false;
        }
        result.add(s);
      }
      else {
        if (s.contains("--- maven-surefire-plugin:")) {
          junitSection = true;
        }
      }
    }
    return result;
  }
  
  protected static void assertPattern(final String regex, final List<String> list){
    final Pattern pattern = Pattern.compile(regex);
    for(final String s : list){
      if (pattern.matcher(s).find()) return;
    }
    fail("Pattern "+pattern.pattern()+" not found");
  }

  protected static void assertNoPattern(final String regex, final List<String> list){
    final Pattern pattern = Pattern.compile(regex);
    for(final String s : list){
      if (pattern.matcher(s).find()) fail("Detected pattern "+pattern.pattern());
    }
  }
}
