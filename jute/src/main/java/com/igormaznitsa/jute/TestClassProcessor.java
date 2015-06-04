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

import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.*;

final class TestClassProcessor extends ClassVisitor {

  private final List<TestContainer> detectedTestMethodList;
  private final String[] includedTests;
  private final String[] excludedTests;
  private final String classFilePath;
  private String className;
  private boolean classJUnitIgnoreFlag;
  private final Log logger;
  private final boolean verbose;
  private final TestContainer baseParameters;
  private TestContainer clazzParameters;
  private boolean inappropriateClass;

  private final String juteTestParameter;
  
  TestClassProcessor(final String juteTestParameter, final String classFilePath, final TestContainer baseParameters, final Log logger, final boolean verbose, final List<TestContainer> resultList, final String[] includedTestPatterns, final String[] excludedTestPatterns) {
    super(Opcodes.ASM5);
    this.juteTestParameter = juteTestParameter;
    this.detectedTestMethodList = resultList;
    this.includedTests = includedTestPatterns;
    this.excludedTests = excludedTestPatterns;
    this.logger = logger;
    this.verbose = verbose;
    this.baseParameters = baseParameters;
    this.classFilePath = classFilePath;
  }

  private boolean isTestIncluded(final String testName) {
    if (testName.startsWith("<")) {
      return false;
    }
    boolean result = true;
    if (this.includedTests != null && this.includedTests.length != 0) {
      result = false;
      for (final String wildcard : this.includedTests) {
        if (FilenameUtils.wildcardMatch(testName, wildcard)) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  private boolean isTestExcluded(final String testName) {
    if (testName.startsWith("<")) {
      return true;
    }
    boolean result = false;
    if (this.excludedTests != null && this.excludedTests.length != 0) {
      result = false;
      for (final String wildcard : this.excludedTests) {
        if (FilenameUtils.wildcardMatch(testName, wildcard)) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  @Override
  public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
    if (this.inappropriateClass) {
      return null;
    }

    if (((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_STATIC)) != 0) || this.classJUnitIgnoreFlag || !desc.equals("()V") || name.startsWith("<")) {
      return null;
    }
    final boolean foundInExcludedList = isTestIncluded(desc);
    final boolean testExcluded = isTestExcluded(desc);

    final String logTestName = this.className + '#' + name;

    if (!foundInExcludedList) {
      this.logger.info("Test method " + logTestName + " is ignored because not presented in include list");
      return null;
    }

    if (testExcluded) {
      this.logger.info("Test " + logTestName + " is ignored because presented in exclude list");
      return null;
    }

    return new MethodVisitor(Opcodes.ASM5) {
      boolean junitTest;
      boolean juteTest;
      boolean junitIgnore;
      TestContainer detectedMethod;

      @Override
      public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        if (detectedMethod == null) {
          detectedMethod = new TestContainer(classFilePath, className, name, clazzParameters == null ? baseParameters : clazzParameters, null);
        }

        AnnotationVisitor result = null;
        if (desc.equals(JuteMojo.ANNO_TEST)) {
          this.junitTest = true;
        }
        else if (desc.equals(JuteMojo.ANNO_IGNORE)) {
          this.junitIgnore = true;
        }
        else if (desc.equals(JuteMojo.ANNO_JUTE)) {
          this.juteTest = true;
          result = detectedMethod;
        }
        return result;
      }

      @Override
      public void visitEnd() {
        if (detectedMethod == null) {
          detectedMethod = new TestContainer(classFilePath, className, name, clazzParameters == null ? baseParameters : clazzParameters, null);
        }
        
        this.juteTest = this.juteTest || clazzParameters!=null;
        
        if ((this.junitTest || this.juteTest) && Utils.checkClassAndMethodForPattern(juteTestParameter,detectedMethod.getClassName(), detectedMethod.getMethodName(), false)){
          detectedMethod.setJUnitIgnore(this.junitIgnore);
          detectedMethod.setJUnitTest(this.junitTest);
          detectedMethod.setJuteTest(this.juteTest);
          detectedTestMethodList.add(detectedMethod);
        }
      }
    };
  }

  @Override
  public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
    this.className = name.replace('/', '.');
    this.inappropriateClass = (access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_ANNOTATION | Opcodes.ACC_ENUM)) != 0;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
    if (this.inappropriateClass) {
      return null;
    }

    AnnotationVisitor result = null;
    if (JuteMojo.ANNO_IGNORE.equals(desc)) {
      this.classJUnitIgnoreFlag = true;
      if (this.clazzParameters == null) {
        this.clazzParameters = new TestContainer(this.classFilePath, this.className, "", this.baseParameters, null);
      }
      this.clazzParameters.setJUnitIgnore(true);
    }
    else if (JuteMojo.ANNO_JUTE.equals(desc)) {
      if (this.clazzParameters == null) {
        this.clazzParameters = new TestContainer(this.classFilePath, this.className, "", this.baseParameters, null);
      }
      this.clazzParameters.setJuteTest(true);
      result = this.clazzParameters;
    }
    return result;
  }

  public String getClassName(){
    return this.className;
  }
  
  public TestContainer getClassJUteParameters(){
    return this.clazzParameters;
  }
  
  @Override
  public int hashCode(){
    return this.className.hashCode();
  }
  
  @Override
  public boolean equals(final Object obj){
    if (obj == null) return false;
    if (obj == this) return true;
    if (obj instanceof TestClassProcessor){
       return this.className.equals(((TestClassProcessor)obj).className);
    }
    return false;
  }
}
