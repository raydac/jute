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

import java.util.List;
import java.util.regex.Pattern;
import org.apache.maven.it.Verifier;
import static org.junit.Assert.*;
import org.junit.Test;

public class IntegrationTest extends AbstractJUteITTest{
  @Test
  public void testJUnitMarkedTests_JUteDefaultConfig() throws Exception {
    final Verifier verifier = verify("junitMarkedDefaultCfg", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertPattern("^Test_Method1$", junitSection);
    assertNoPattern("^Test_Method2$", junitSection);
    assertPattern("^Test_Method3$", junitSection);
    
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test1\\.+OK", juteSection);
    assertNoPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test2\\.+OK", juteSection);
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test3\\.+OK", juteSection);
    assertNoPattern("^\\>\\>\\>Console.*$", juteSection);
    assertNoPattern("^\\>\\>\\>Error.*$", juteSection);
  }

  @Test
  public void testJUteMarkedTests_JUteDefaultConfig() throws Exception {
    final Verifier verifier = verify("juteMarkedDefaultCfg", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertNoPattern("^Test_Method1$", junitSection);
    assertNoPattern("^Test_Method2$", junitSection);
    assertNoPattern("^Test_Method3$", junitSection);
    
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test1\\.+OK", juteSection);
    assertNoPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test2\\.+OK", juteSection);
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test3\\.+OK", juteSection);
    assertNoPattern("^\\>\\>\\>Console.*$", juteSection);
    assertNoPattern("^\\>\\>\\>Error.*$", juteSection);
  }

  @Test
  public void testExcludes() throws Exception {
    final Verifier verifier = verify("excludedFileCfg", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertNoPattern("Method", junitSection);
  
    assertNoPattern("DefaultTest#Method", juteSection);
    
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest2\\#test1\\.+OK", juteSection);
    assertNoPattern("\\[INFO\\]\\ssome\\.DefaultTest2\\#test2\\.+OK", juteSection);
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest2\\#test3\\.+OK", juteSection);
    assertNoPattern("^\\>\\>\\>Console.*$", juteSection);
    assertNoPattern("^\\>\\>\\>Error.*$", juteSection);
  }

  @Test
  public void testIncludes() throws Exception {
    final Verifier verifier = verify("includedFileCfg", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertNoPattern("Method", junitSection);
  
    assertNoPattern("DefaultTest#Method", juteSection);
    
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest2\\#test1\\.+OK", juteSection);
    assertNoPattern("\\[INFO\\]\\ssome\\.DefaultTest2\\#test2\\.+OK", juteSection);
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest2\\#test3\\.+OK", juteSection);
    assertNoPattern("^\\>\\>\\>Console.*$", juteSection);
    assertNoPattern("^\\>\\>\\>Error.*$", juteSection);
  }

  @Test
  public void testTerminalIn() throws Exception {
    final Verifier verifier = verify("terminalIn", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertNoPattern("Method", junitSection);
    
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test1\\.+OK", juteSection);
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test2\\.+OK", juteSection);
  }

  @Test
  public void testJvmOptions() throws Exception {
    final Verifier verifier = verify("jvmOptions", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertNoPattern("Method", junitSection);
    
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test1\\.+OK", juteSection);
    assertPattern("\\[INFO\\]\\ssome\\.DefaultTest\\#test2\\.+OK", juteSection);
  }
}
