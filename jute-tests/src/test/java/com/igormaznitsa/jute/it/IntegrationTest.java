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

import java.util.*;
import org.apache.maven.it.Verifier;
import org.junit.Test;
import static org.junit.Assert.*;

public class IntegrationTest extends AbstractJUteITTest {
  @Test
  public void testNoTests() throws Exception {
    final Verifier verifier = verify("noTests", false);

    final List<String> junitSection = extractJUnitSection(verifier);
    final List<String> juteSection = extractJuteSection(verifier);

    assertPattern("Tests run: 0",junitSection);
    
    assertNoPattern("DefaultTest", juteSection);
    assertNoPattern("Test1", juteSection);
    assertNoPattern("Test2", juteSection);
    assertPattern("0 potential test",juteSection);
  }

  @Test
  public void testJUteTestParameter() throws Exception {
    final Verifier verifier = verify("juteTestParameter", false, Collections.singletonMap("jute.test", "*.DefaultTestSec#testSecG"));

    final List<String> junitSection = extractJUnitSection(verifier);
    final List<String> juteSection = extractJuteSection(verifier);

    assertEmpty(junitSection);
    
    assertPattern("1 potential test",juteSection);
    assertPattern("testSecG\\.+OK",juteSection);
  }

  @Test
  public void testJUteTestParameter_MarkedJUnitTest() throws Exception {
    final Verifier verifier = verify("juteTestParameter_JUnitMarked", false, Collections.singletonMap("jute.test", "*.DefaultTestSec#testSecG"));

    final List<String> junitSection = extractJUnitSection(verifier);
    final List<String> juteSection = extractJuteSection(verifier);

    assertEmpty(junitSection);
    
    assertPattern("1 potential test",juteSection);
    assertPattern("testSecG\\.+OK",juteSection);
  }


  @Test
  public void testJUnitMarkedTests_JUteDefaultConfig() throws Exception {
    final Verifier verifier = verify("junitMarkedDefaultCfg", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);

    assertPattern("^Test_Method1$", junitSection);
    assertNoPattern("^Test_Method2$", junitSection);
    assertPattern("^Test_Method3$", junitSection);

    assertPattern("test1\\.+OK", juteSection);
    assertNoPattern("test2\\.+OK", juteSection);
    assertPattern("test3\\.+OK", juteSection);
    assertNoPattern("\\[ERROR\\]", juteSection);
  }

  @Test
  public void testAlphabetOrdered() throws Exception {
    final Verifier verifier = verify("alphabetOrdered", false);

    final List<String> junitSection = extractJUnitSection(verifier);
    final List<String> juteSection = extractJuteSection(verifier);

    assertEmpty(junitSection);
    assertNoPattern("\\[ERROR\\]", juteSection);
    assertPatternOrder(juteSection, "\\u2504testA1\\.+OK", "\\u2504testA2\\.+OK", "\\u2504testB\\.+OK", "\\u2504testC\\.+OK", "\\u2504testD\\.+OK", "\\u2504testE\\.+OK", "\\u2504testF\\.+OK", "\\u2504testG\\.+OK");
  }

  @Test
  public void testTimeoutExecution() throws Exception {
    final Verifier verifier = verifyWithExpectedError("timeoutExecution", false);

    final List<String> junitSection = extractJUnitSection(verifier);
    final List<String> juteSection = extractJuteSection(verifier);

    assertEmpty(junitSection);
    assertPattern("\\u2504testB\\.+TIMEOUT", juteSection);
    assertPatternOrder(juteSection, "Output","TestB","Error");
    assertPattern("\\u2504testA\\.+OK", juteSection);
    assertPattern("\\u2504testC\\.+OK", juteSection);
  }
  
  @Test
  public void testParallelExecution() throws Exception {
    final Verifier verifier = verify("parallelExecution", false);

    final List<String> junitSection = extractJUnitSection(verifier);
    final List<String> juteSection = extractJuteSection(verifier);

    assertEmpty(junitSection);
    assertNoPattern("\\[ERROR\\]", juteSection);

    assertPatternOrder(juteSection, "\\u2504testA\\.+OK", "\\u2504testB\\.+OK");

    final List<String> parallelList = new ArrayList<String>();
    boolean started = false;
    boolean testListStarted = false;
    for (final String s : juteSection) {
      if (testListStarted) {
        if (s.trim().equals("[INFO]")) break;
        if (s.contains("\u2550")) {
          parallelList.add(s);
          started = true;
        }
        else {
          assertFalse("Parallel tests have some between them [" + s + ']', started);
        }
      }else{
        testListStarted = s.contains("\u2502");
      }
    }

    assertEquals(3, parallelList.size());
    boolean testC = false;
    boolean testD = false;
    boolean testE = false;
    for (final String s : parallelList) {
      if (s.contains("testE")) {
        testE = true;
      }
      if (s.contains("testD")) {
        testD = true;
      }
      if (s.contains("testC")) {
        testC = true;
      }
    }
    assertTrue(testC && testD && testE);

  }

  @Test
  public void testJUteMarkedTests_JUteDefaultConfig() throws Exception {
    final Verifier verifier = verify("juteMarkedDefaultCfg", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);

    assertNoPattern("^Test_Method1$", junitSection);
    assertNoPattern("^Test_Method2$", junitSection);
    assertNoPattern("^Test_Method3$", junitSection);

    assertPattern("test1\\.+OK", juteSection);
    assertPattern("test2\\.+SKIPPED", juteSection);
    assertPattern("test3\\.+OK", juteSection);
    assertNoPattern("\\[ERROR\\]", juteSection);
  }

  @Test
  public void testJUteMarkedTests_BeforeAfter() throws Exception {
    final Verifier verifier = verifyWithExpectedError("juteMarked_BeforeAfter", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertPattern("Tests run: 0,",junitSection);
    assertPatternOrder(juteSection, "AbsBeforeClass", "BeforeClass", "AbsBeforeTest", "BeforeTest", "TEST", "AbsAfterTest", "AfterTest", "AbsAfterClass", "AfterClass");
    assertPatternOrder(juteSection, "AbsBeforeClass", "BeforeClass", "AbsBeforeTest", "BeforeTest", "FAILEDTEST", "AbsAfterTest", "AfterTest", "AbsAfterClass", "AfterClass");
    assertPattern("FAIL_TEXT",juteSection);
  }

  @Test
  public void testExcludes() throws Exception {
    final Verifier verifier = verify("excludedFileCfg", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);

    assertNoPattern("Method", junitSection);

    assertNoPattern("DefaultTest(?!2)", juteSection);

    assertPattern("test1\\.+OK", juteSection);
    assertPattern("test2\\.+SKIPPED", juteSection);
    assertPattern("test3\\.+OK", juteSection);
    assertNoPattern("\\[ERROR\\]", juteSection);
  }

  @Test
  public void testIncludes() throws Exception {
    final Verifier verifier = verify("includedFileCfg", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);

    assertNoPattern("Method", junitSection);

    assertNoPattern("DefaultTest(?!2)", juteSection);

    assertPattern("test1\\.+OK", juteSection);
    assertNoPattern("test2\\.+OK", juteSection);
    assertPattern("test3\\.+OK", juteSection);
    assertNoPattern("\\[ERROR\\]", juteSection);
  }

  @Test
  public void testTerminalIn() throws Exception {
    final Verifier verifier = verify("terminalIn", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);

    assertNoPattern("Method", junitSection);

    assertNoPattern("\\[ERROR\\]", juteSection);
    assertPattern("test1\\.+OK", juteSection);
    assertPattern("test2\\.+OK", juteSection);
  }

  @Test
  public void testJvmOptions() throws Exception {
    final Verifier verifier = verify("jvmOptions", false);

    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);

    assertNoPattern("Method", junitSection);

    assertNoPattern("\\[ERROR\\]", juteSection);
    assertPattern("test1\\.+OK", juteSection);
    assertPattern("test2\\.+OK", juteSection);
  }
}
