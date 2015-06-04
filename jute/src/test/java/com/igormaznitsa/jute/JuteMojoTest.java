package com.igormaznitsa.jute;

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


import java.io.File;
import java.util.*;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Assert;

public class JuteMojoTest extends AbstractMojoTestCase {
  
  private JuteMojo init(final File config) throws Exception {
    final JuteMojo mojo = (JuteMojo) this.configureMojo(new JuteMojo(), "jute", config);
    assertNotNull(mojo);
    return mojo;
  }


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    System.clearProperty("jute.test");
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    System.clearProperty("jute.test");
  }

  public void testDefaultConfig() throws Exception {
    final JuteMojo myMojo = init(getTestFile("src/test/resources/com/igormaznitsa/jute/testcfgs/testDefaultConfig.xml"));
    assertNotNull(myMojo);

    assertEquals(false,myMojo.isVerbose());
    assertEquals(0L,myMojo.getTimeout());
    assertNull(myMojo.getEnv());
    assertFalse(myMojo.isOnlyAnnotated());
    assertNull(myMojo.getExcludeTests());
    assertNull(myMojo.getExcludes());
    assertNull(myMojo.getIncludeTests());
    assertNull(myMojo.getIncludes());
    assertNull(myMojo.getJava());
    assertNull(myMojo.getIn());
    assertFalse(myMojo.isSkip());
    assertFalse(myMojo.isSkipTests());
    assertFalse(myMojo.isEnforcePrintConsole());
    assertNull(myMojo.getJUteTest());
  }

  public void testNonDefaultConfig() throws Exception {
    System.setProperty("jute.test","some.package.DefaultTest#Method");
    
    final JuteMojo myMojo = init(getTestFile("src/test/resources/com/igormaznitsa/jute/testcfgs/testNonDefaultConfig.xml"));
    assertNotNull(myMojo);

    assertEquals(true,myMojo.isVerbose());
    assertEquals(1000L,myMojo.getTimeout());

    final Properties env = myMojo.getEnv();
    assertEquals(2,env.size());
    assertEquals("value1",env.get("key1"));
    assertEquals("value2",env.get("key2"));

    final Properties jprops = myMojo.getJavaProperties();
    assertEquals(1,jprops.size());
    assertEquals("javaValue1",jprops.get("javaKey1"));

    Assert.assertEquals(new String[]{"-Xss100k","-Xmx100m"},myMojo.getJvmOptions());

    assertTrue(myMojo.isOnlyAnnotated());
    
    assertTrue(Arrays.deepEquals(new String[]{"excludedTest1","excludedTest2"}, myMojo.getExcludeTests()));
    assertTrue(Arrays.deepEquals(new String[]{"excluded1","excluded2"}, myMojo.getExcludes()));
    assertTrue(Arrays.deepEquals(new String[]{"includedTest1","includedTest2"}, myMojo.getIncludeTests()));
    assertTrue(Arrays.deepEquals(new String[]{"included1","included2"}, myMojo.getIncludes()));
    assertEquals("fake/jre/bin/java",myMojo.getJava());
    assertTrue(myMojo.isEnforcePrintConsole());
    assertEquals("Some test text",myMojo.getIn());
    assertEquals("some.package.DefaultTest#Method", myMojo.getJUteTest());
  }
}
