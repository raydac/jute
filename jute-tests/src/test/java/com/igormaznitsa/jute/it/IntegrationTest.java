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
    
    assertPattern(Pattern.compile("^Test_Method1$"), junitSection);
    assertNoPattern(Pattern.compile("^Test_Method2$"), junitSection);
    assertPattern(Pattern.compile("^Test_Method3$"), junitSection);
    
    assertPattern(Pattern.compile("\\[INFO\\]\\ssome\\.DefaultTest\\#test1\\.+OK"), juteSection);
    assertNoPattern(Pattern.compile("\\[INFO\\]\\ssome\\.DefaultTest\\#test2\\.+OK"), juteSection);
    assertPattern(Pattern.compile("\\[INFO\\]\\ssome\\.DefaultTest\\#test3\\.+OK"), juteSection);
    assertNoPattern(Pattern.compile("^\\>\\>\\>Console.*$"), juteSection);
    assertNoPattern(Pattern.compile("^\\>\\>\\>Error.*$"), juteSection);
  }

  @Test
  public void testJUteMarkedTests_JUteDefaultConfig() throws Exception {
    final Verifier verifier = verify("juteMarkedDefaultCfg", false);
    
    final List<String> juteSection = extractJuteSection(verifier);
    final List<String> junitSection = extractJUnitSection(verifier);
    
    assertNoPattern(Pattern.compile("^Test_Method1$"), junitSection);
    assertNoPattern(Pattern.compile("^Test_Method2$"), junitSection);
    assertNoPattern(Pattern.compile("^Test_Method3$"), junitSection);
    
    assertPattern(Pattern.compile("\\[INFO\\]\\ssome\\.DefaultTest\\#test1\\.+OK"), juteSection);
    assertNoPattern(Pattern.compile("\\[INFO\\]\\ssome\\.DefaultTest\\#test2\\.+OK"), juteSection);
    assertPattern(Pattern.compile("\\[INFO\\]\\ssome\\.DefaultTest\\#test3\\.+OK"), juteSection);
    assertNoPattern(Pattern.compile("^\\>\\>\\>Console.*$"), juteSection);
    assertNoPattern(Pattern.compile("^\\>\\>\\>Error.*$"), juteSection);
  }
}
