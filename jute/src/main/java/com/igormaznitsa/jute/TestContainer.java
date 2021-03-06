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

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.maven.plugin.logging.Log;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.zeroturnaround.exec.ProcessExecutor;

public final class TestContainer extends AnnotationVisitor {
  public enum TestResult{
    SKIPPED,
    OK,
    TIMEOUT,
    ERROR;
  }
  
  // all fields to be filled by JUteTest annotation values must have the same names!
  private String jvm = "";
  private String in = "";
  private int order = -1;
  private boolean printConsole = false;
  private boolean skip = false;
  private long timeout = 0L;
  private final List<String> jvmOpts = new ArrayList<String>();
  //---------------------------
  private boolean junitTest = false;
  private boolean junitIgnore = false;
  private boolean juteTest = false;
  private final String classFilePath;
  private final String className;
  private final String methodName;
  private final Runnable endCall;

  private String visitingArrayName = null;
  
  private volatile String lastTerminalOut;
  
  public TestContainer(final String classFilePath, final String className, final String testName, final String jvm, final String[] jvmOpts, final String in, final int order, final boolean enforcePrintConsole, final boolean skip, final long timeout) {
    super(Opcodes.ASM5);
    this.classFilePath = classFilePath;
    this.className = className;
    this.methodName = testName;
    this.jvm = jvm;
    this.in = in;
    this.order = order;
    this.printConsole = enforcePrintConsole;
    this.skip = skip;
    this.timeout = timeout;
    if (jvmOpts != null) {
      for (final String s : jvmOpts) {
        this.jvmOpts.add(s);
      }
    }
    this.endCall = null;
  }

  public TestContainer(final String classFilePath, final String className, final String testName, final TestContainer base, final Runnable endCall) {
    super(Opcodes.ASM5);
    this.classFilePath = classFilePath;
    this.className = className;
    this.methodName = testName;
    if (base != null) {
      this.jvm = base.jvm;
      this.in = base.in;
      this.order = base.order;
      this.printConsole = base.printConsole;
      this.skip = base.skip;
      this.timeout = base.timeout;
      this.jvmOpts.addAll(base.jvmOpts);
      this.junitIgnore = base.junitIgnore;
      this.juteTest = base.juteTest;
    }
    this.endCall = endCall;
  }

  public boolean isIgnored() {
    return this.skip || (this.junitTest && this.junitIgnore);
  }

  public String getClassFilePath() {
    return this.classFilePath;
  }

  public String getClassName() {
    return this.className;
  }

  public String[] getJvmOpts() {
    return this.jvmOpts.toArray(new String[this.jvmOpts.size()]);
  }

  public String getMethodName() {
    return this.methodName;
  }

  public String getJVM() {
    return this.jvm;
  }

  public String getIN() {
    return this.in;
  }

  public int getOrder() {
    return this.order;
  }

  public boolean isPrintConsole() {
    return this.printConsole;
  }

  public boolean isSkip() {
    return this.skip;
  }

  public long getTimeout() {
    return this.timeout;
  }

  public boolean isJUnitTest() {
    return this.junitTest;
  }

  public boolean isJUnitIgnore() {
    return this.junitIgnore;
  }

  public boolean isJUteTest() {
    return this.juteTest;
  }

  public void setJuteTest(final boolean value) {
    this.juteTest = value;
  }

  public void setJUnitTest(final boolean value) {
    this.junitTest = value;
  }

  public void setJUnitIgnore(final boolean value) {
    this.junitIgnore = value;
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    this.visitingArrayName = name;
    return this;
  }

  @Override
  public void visit(final String name, final Object value) {
    final String annoName = name == null ? this.visitingArrayName : name;

    for (final Field f : TestContainer.class.getDeclaredFields()) {
      if (f.getName().equals(annoName)) {
        try {
          if (f.getType().isAssignableFrom(List.class)) {
            ((List) f.get(this)).add(value);
          }
          else {
            f.set(this, value);
          }
        }
        catch (Exception ex) {
          throw new Error("Can't set value of JUte annotation '" + name + '\'', ex);
        }
      }
    }
  }

  @Override
  public void visitEnd() {
    if (this.endCall != null) {
      this.endCall.run();
    }
  }

  public TestResult executeTest(final Log log, final boolean startOnlyJUteMarkedTests, final int maxTestNameLength, final String classPath, final Properties javaProperties, final Properties env) throws IOException, InterruptedException {
    final boolean shouldBeSkipped;
    if (startOnlyJUteMarkedTests){
      if (!this.isJUteTest()){
        shouldBeSkipped = true;
      }else{
        shouldBeSkipped = this.skip;
      }
    }else{
      shouldBeSkipped = (this.junitTest && this.junitIgnore) || (this.juteTest && this.skip);
    }

    if (shouldBeSkipped) {
      return TestResult.SKIPPED;
    }
    
    final List<String> arguments = new ArrayList<String>();
    arguments.add(this.jvm);

    if (!this.jvmOpts.isEmpty()) {
      for (final String opt : this.jvmOpts) {
        arguments.add(opt);
      }
    }

    if (javaProperties != null && !javaProperties.isEmpty()) {
      for (final Map.Entry<Object, Object> entry : javaProperties.entrySet()) {
        final String key = (String) entry.getKey();
        final String value = (String) entry.getValue();
        arguments.add("-D" + key + "=" + value);
      }
    }

    arguments.add("-classpath");
    arguments.add(classPath);

    final boolean junitRunner;
    
    if (this.isJUnitTest() && !this.isJUnitIgnore()){
      junitRunner = true;
      arguments.add(JuteMojo.JUNIT_SINGLE_RUNNER_CLASS);
    }else{
      junitRunner = false;
      arguments.add(JuteMojo.JUTE_SINGLE_RUNNER_CLASS);
    }
    
    log.debug("Test "+this+" will be started by "+(junitRunner ? "JUnit runner" : "JUte runner"));
    
    arguments.add(this.toString());

    final StringBuilder buffer = new StringBuilder();
    for (final String s : arguments) {
      if (buffer.length() > 0) {
        buffer.append(' ');
      }
      buffer.append(s);
    }
    log.debug(buffer.toString());

    final ProcessExecutor exec = new ProcessExecutor(arguments.toArray(new String[arguments.size()]));

    if (env != null && !env.isEmpty()) {
      for (final Map.Entry<Object, Object> entry : env.entrySet()) {
        exec.environment((String) entry.getKey(), (String) entry.getValue());
      }
    }

    if (this.in != null) {
      exec.redirectInput(new ByteArrayInputStream(this.in.getBytes(Charset.defaultCharset())));
    }

    final ByteArrayOutputStream consoleBuffer = new ByteArrayOutputStream();
    final ByteArrayOutputStream consoleErrBuffer = new ByteArrayOutputStream();

    final ProcessExecutor executor = exec.destroyOnExit().redirectError(consoleErrBuffer).redirectOutput(consoleBuffer);
    int result;
    if (this.timeout > 0L) {
      try {
        result = executor.timeout(this.timeout, TimeUnit.MILLISECONDS).execute().getExitValue();
      }
      catch (TimeoutException ex) {
        this.lastTerminalOut = prepareTerminalLog(consoleBuffer, consoleErrBuffer);
        return TestResult.TIMEOUT;
      }
    }
    else {
      result = executor.executeNoTimeout().getExitValue();
    }

    this.lastTerminalOut = prepareTerminalLog(consoleBuffer, consoleErrBuffer);

    return result == 0 ? TestResult.OK : TestResult.ERROR;
  }

  private static String collectConsoleData(final ByteArrayOutputStream out, final ByteArrayOutputStream err) {
    final StringBuilder record = new StringBuilder();
    
    final String outStr = new String(out.toByteArray(), Charset.defaultCharset());
    final String errStr = new String(err.toByteArray(), Charset.defaultCharset());
    
    record.append((char)0x2562+"Output"+(char) 0x255F).append(Utils.lineSeparator).append(outStr).append(Utils.lineSeparator);
    if (!errStr.isEmpty()){
      record.append((char) 0x2563 +"Error"+ (char) 0x2560).append(Utils.lineSeparator).append(errStr);
    }
    return record.toString();
  }

  private static String prepareTerminalLog(final ByteArrayOutputStream out, final ByteArrayOutputStream err) {
    final StringBuilder record = new StringBuilder();
    record.append(collectConsoleData(out, err));
    return record.toString();
  }

  @Override
  public String toString() {
    return this.className + '#' + this.methodName;
  }

  public String getLastTerminalOut() {
    return this.lastTerminalOut;
  }
  
  public void dispose(){
    this.lastTerminalOut = null;
  }
  
}
