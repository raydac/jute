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

import org.junit.Test;
import static org.junit.Assert.*;

public class UtilsTest {

  @Test
  public void testPrintTimeDelay(){
    assertEquals("34:17:36.789",Utils.printTimeDelay(123456789L));
  }
  
  @Test
  public void testToStandardJavaClassName(){
    assertEquals("some.package.Klazz$Hello",Utils.toStandardJavaClassName("/root/","/root/some/package/Klazz$Hello.class"));
    assertEquals("some.package.Klazz$Hello",Utils.toStandardJavaClassName("/root/","/root/some/package/Klazz$Hello"));
  }

  @Test
  public void testCheckClassAndMethodForPattern(){
    assertFalse(Utils.checkClassAndMethodForPattern(null, null, null, false));
    assertFalse(Utils.checkClassAndMethodForPattern(null, null, null, true));
    assertFalse(Utils.checkClassAndMethodForPattern(null, null, "", false));
    assertFalse(Utils.checkClassAndMethodForPattern(null, "", null, false));
    assertFalse(Utils.checkClassAndMethodForPattern("", null, null, false));
    
    assertTrue(Utils.checkClassAndMethodForPattern(null, "", null, true));
    assertTrue(Utils.checkClassAndMethodForPattern(null, "", "", false));
    assertTrue(Utils.checkClassAndMethodForPattern(null, "", "", false));

    assertTrue(Utils.checkClassAndMethodForPattern("com.package.Klazz", "com.package.Klazz", "Some", false));
    assertTrue(Utils.checkClassAndMethodForPattern("com.package.Klazz#*", "com.package.Klazz", "Some", false));
    assertTrue(Utils.checkClassAndMethodForPattern("*", "com.package.Klazz", "Some", false));
    assertTrue(Utils.checkClassAndMethodForPattern("*#*", "com.package.Klazz", "Some", false));
    assertTrue(Utils.checkClassAndMethodForPattern("*#So*", "com.package.Klazz", "Some", false));
    
    assertFalse(Utils.checkClassAndMethodForPattern("com.package.Klazzz", "com.package.Klazz", "Some", false));
    assertFalse(Utils.checkClassAndMethodForPattern("com.package.Klazz#SomE", "com.package.Klazz", "Some", false));
    assertFalse(Utils.checkClassAndMethodForPattern("com.package.Klazz#Som??", "com.package.Klazz", "Some", false));
  }
  
}
