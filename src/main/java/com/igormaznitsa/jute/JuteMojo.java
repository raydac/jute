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

import com.igormaznitsa.jute.junit.SingleTestMethodRunner;
import com.jcabi.aether.Classpath;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.joda.time.*;
import org.joda.time.format.*;
import org.objectweb.asm.*;
import org.springframework.util.AntPathMatcher;
import org.zeroturnaround.exec.ProcessExecutor;

/**
 * Maven plugin allows to execute JUnit tests separately in external processes
 * per each test.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
@Mojo(name = "jute", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class JuteMojo extends AbstractMojo {

  private static final String JUNIT_SINGLE_RUNNER_CLASS = SingleTestMethodRunner.class.getName();

  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;
  @Parameter(defaultValue = "${session}", readonly = true)
  protected MavenSession session;

  /**
   * List of test method names to be included into testing. Wildcard pattern can
   * be used.
   */
  @Parameter(name = "includeTests")
  protected String[] includeTests;
  /**
   * List of test method names to be excluded from testing. Wildcard pattern can
   * be used.
   */
  @Parameter(name = "excludeTests")
  protected String[] excludeTests;
  /**
   * List of java test files to be included into testing. Ant path pattern can
   * be used.
   */
  @Parameter(name = "includes")
  protected String[] includes;
  /**
   * List of java test files to be excluded from testing. Ant path pattern can
   * be uses.
   */
  @Parameter(name = "excludes")
  protected String[] excludes;
  /**
   * Path to java interpreter either JRE folder or executable file.
   */
  @Parameter(name = "java")
  protected String java;
  /**
   * Show extra information during processing.
   */
  @Parameter(name = "verbose", defaultValue = "false")
  protected boolean verbose;
  /**
   * Timeout for testing process in milliseconds.
   */
  @Parameter(name = "timeout", defaultValue = "0")
  protected long timeout;

  /**
   * Map of environment variables to be provided to staring processes.
   */
  @Parameter(name = "env")
  protected Map<String, String> env;

  private static List<String> prepareListOfFiles(final File rootFolder, final String[] includes, final String[] excludes) {
    final List<String> result = new ArrayList<String>();

    final Iterator<File> iterator = FileUtils.iterateFiles(rootFolder, new IOFileFilter() {
      private final AntPathMatcher matcher = new AntPathMatcher();

      @Override
      public boolean accept(final File file) {
        final String path = file.getAbsolutePath();
        boolean include = false;

        if (path.endsWith(".class")) {
          if (includes != null) {
            for (final String patteen : includes) {
              if (matcher.match(patteen, path)) {
                include = true;
                break;
              }
            }
          }
          else {
            include = true;
          }

          if (include && excludes != null) {
            for (final String pattern : excludes) {
              if (matcher.match(pattern, path)) {
                include = false;
                break;
              }
            }
          }
        }
        return include;
      }

      @Override
      public boolean accept(final File dir, final String name) {
        final String path = name;
        boolean include = false;
        if (includes != null) {
          for (final String pattern : includes) {
            if (matcher.match(pattern, path)) {
              include = true;
              break;
            }
          }
        }
        else {
          include = true;
        }

        if (include && excludes != null) {
          for (final String pattern : excludes) {
            if (matcher.match(pattern, path)) {
              include = false;
              break;
            }
          }
        }

        return include;
      }
    }, DirectoryFileFilter.DIRECTORY);

    while (iterator.hasNext()) {
      result.add(iterator.next().getAbsolutePath());
    }

    return result;
  }

  private static String[] normalizeStringArray(final String[] array) {
    if (array == null) {
      return new String[0];
    }
    final String[] result = new String[array.length];
    for (int i = 0; i < array.length; i++) {
      final String s = array[i];
      if (s.toLowerCase(Locale.ENGLISH).endsWith(".java")) {
        final String withoutExt = s.substring(0, s.lastIndexOf('.'));
        result[i] = withoutExt + ".class";
      }
      else {
        result[i] = s;
      }
    }
    return result;
  }

  private File findInterpreter() {
    final String name = SystemUtils.IS_OS_WINDOWS ? "\\bin\\java.exe" : "/bin/java";

    File result;
    if (this.java == null) {
      result = new File(new File(System.getProperty("java.home")), name);
    }
    else {
      result = new File(this.java);
      if (result.isDirectory()) {
        result = new File(result, name);
      }
    }
    return result.isFile() ? result : null;
  }

  private String makeClassPath(final File mojoJarPath, final Collection<File> files) {
    final StringBuilder result = new StringBuilder();

    for (final File f : files) {
      if (result.length() > 0) {
        result.append(File.pathSeparatorChar);
      }
      result.append(f.getAbsoluteFile());
    }

    if (result.length() > 0) {
      result.append(File.pathSeparatorChar);
    }
    result.append(mojoJarPath.getAbsolutePath());

    return result.toString();
  }

  @Override
  public void execute() throws MojoExecutionException {
    final File pathToMojoJar;
    try {
      pathToMojoJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    }
    catch (URISyntaxException ex) {
      throw new MojoExecutionException("Can't get path to the Mojo jar", ex);
    }

    final File javaInterpreter = findInterpreter();
    if (javaInterpreter == null) {
      throw new MojoExecutionException("Can't find java interpreter, check the 'java' parameter");
    }

    final String testClassPath = makeClassPath(pathToMojoJar, new Classpath(project, new File(session.getLocalRepository().getBasedir()), "test"));
    final List<String> potentialTestFiles = prepareListOfFiles(new File(project.getBuild().getTestOutputDirectory()), normalizeStringArray(this.includes), normalizeStringArray(this.excludes));
    final List<String> extractedNonIgnoredtestMethods = new ArrayList<String>();
    try {
      extractAllNonIgnoredTestMethods(potentialTestFiles, extractedNonIgnoredtestMethods);
    }
    catch (IOException ex) {
      throw new MojoExecutionException("Can't scan classes", ex);
    }

    final long startTime = System.currentTimeMillis();

    if (this.verbose) {
      getLog().info("Java interpreter path: " + javaInterpreter.getAbsolutePath());
      getLog().info("Test class path: " + testClassPath);
      getLog().info("Detected " + extractedNonIgnoredtestMethods.size() + " test method(s)");
    }

    int startedCounter = 0;
    int errorCounter = 0;

    int maxTestNameLength = 0;
    for (final String methodName : extractedNonIgnoredtestMethods) {
      if (maxTestNameLength < methodName.length()) {
        maxTestNameLength = methodName.length();
      }
    }

    for (final String methodName : extractedNonIgnoredtestMethods) {
      try {
        startedCounter++;
        if (!executeTest(javaInterpreter.getAbsolutePath(), maxTestNameLength, testClassPath, methodName)) {
          errorCounter++;
        }
      }
      catch (Throwable ex) {
        throw new MojoExecutionException("Error during test method execution '" + methodName + '\'', ex);
      }
    }

    final Duration duration = new Duration(startTime, System.currentTimeMillis());
    final Period period = duration.toPeriod().normalizedStandard(PeriodType.time());
    final PeriodFormatter format = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(2)
            .appendHours().appendSeparator(":")
            .appendMinutes().appendSeparator(":")
            .appendSeconds().appendSeparator(".")
            .appendMillis().toFormatter();
    getLog().info(String.format("Tests run: %d, Errors: %d, Total time: %s", startedCounter, errorCounter, format.print(period)));

    if (errorCounter != 0) {
      throw new MojoExecutionException("Detected failed tests, see session log");
    }
  }

  private boolean executeTest(final String javaPath, final int maxTestNameLength, final String classPath, final String methodToExecute) throws IOException, InterruptedException {
    final ProcessExecutor exec = new ProcessExecutor(javaPath, "-classpath", classPath, JUNIT_SINGLE_RUNNER_CLASS, methodToExecute);

    if (this.env != null && !this.env.isEmpty()) {
      for (final Map.Entry<String, String> r : this.env.entrySet()) {
        exec.environment(r.getKey(), r.getValue());
      }
    }

    final ByteArrayOutputStream consoleBuffer = new ByteArrayOutputStream();
    final ByteArrayOutputStream consoleErrBuffer = new ByteArrayOutputStream();

    final StringBuilder record = new StringBuilder(methodToExecute);
    for (int i = record.length(); i < maxTestNameLength + 10; i++) {
      record.append('.');
    }

    final ProcessExecutor executor = exec.destroyOnExit().redirectError(consoleErrBuffer).redirectOutput(consoleBuffer);
    int result = 0;
    boolean statusPrinted = false;
    if (this.timeout > 0L) {
      try {
        result = executor.timeout(this.timeout, TimeUnit.MILLISECONDS).execute().getExitValue();
      }
      catch (TimeoutException ex) {
        result = 999;
        record.append("TIMEOUT");
        statusPrinted = true;
      }
    }
    else {
      result = executor.executeNoTimeout().getExitValue();
    }

    if (result == 0) {
      if (!statusPrinted) {
        record.append("OK");
      }
      getLog().info(record.toString());
      return true;
    }
    else {
      if (!statusPrinted) {
        record.append("ERROR");
      }
      record.append(System.lineSeparator()).append("-----------------------").append(System.lineSeparator());
      record.append(">>>Console").append(System.lineSeparator()).append(new String(consoleBuffer.toByteArray(), Charset.defaultCharset())).append(System.lineSeparator());
      record.append(">>>Errors").append(System.lineSeparator()).append(new String(consoleErrBuffer.toByteArray(), Charset.defaultCharset())).append(System.lineSeparator());
      record.append("-----------------------");
      getLog().error(record.toString());
      return false;
    }
  }

  private void extractAllNonIgnoredTestMethods(final List<String> testClassFilePaths, final List<String> result) throws IOException {
    for (final String s : testClassFilePaths) {
      extractTestMethodsToList(s, result);
    }
  }

  private void extractTestMethodsToList(final String pathToClass, final List<String> methodList) throws IOException {

    final InputStream in = new FileInputStream(pathToClass);
    try {
      final ClassReader classReader = new ClassReader(in);
      classReader.accept(new ClassVisitorImpl(this.getLog(), this.verbose, methodList, this.includeTests, this.excludeTests), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

  private static final class ClassVisitorImpl extends ClassVisitor {

    private static final String ANNO_TEST = "Lorg/junit/Test;";
    private static final String ANNO_IGNORE = "Lorg/junit/Ignore;";
    private final List<String> methodList;
    private final String[] includedTests;
    private final String[] excludedTests;
    private String className;
    private boolean classIgnored;

    private final Log logger;
    private final boolean verbose;

    ClassVisitorImpl(final Log logger, final boolean verbose, final List<String> resultMethodList, final String[] includedTests, final String[] excludedTests) {
      super(Opcodes.ASM5);
      this.methodList = resultMethodList;
      this.includedTests = includedTests;
      this.excludedTests = excludedTests;
      this.logger = logger;
      this.verbose = verbose;
    }

    private String makeTestName(final String methodName) {
      return this.className + '#' + methodName;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
      if (this.classIgnored || !desc.equals("()V")) {
        return null;
      }

      boolean canBeProcessed = true;

      if (this.includedTests != null) {
        canBeProcessed = false;
        for (final String wildcard : this.includedTests) {
          if (FilenameUtils.wildcardMatch(name, wildcard)) {
            canBeProcessed = true;
            break;
          }
        }

        if (!canBeProcessed) {
          this.logger.info("Test " + makeTestName(name) + " is ignored because is not presented in include test list");
        }
      }

      if (canBeProcessed && this.excludedTests != null) {
        for (final String wildcard : this.excludedTests) {
          if (FilenameUtils.wildcardMatch(name, wildcard)) {
            canBeProcessed = false;
            break;
          }
        }

        if (!canBeProcessed) {
          this.logger.info("Test " + makeTestName(name) + " is ignored because is presented in exclude test list");
        }
      }

      if (!canBeProcessed) {
        return null;
      }
      else {
        return new MethodVisitor(Opcodes.ASM5) {
          boolean isTest;
          boolean isIgnored;

          @Override
          public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            if (desc.equals(ANNO_TEST)) {
              this.isTest = true;
            }
            else if (desc.equals(ANNO_IGNORE)) {
              this.isIgnored = true;
            }

            return null;
          }

          @Override
          public void visitEnd() {
            final String method = makeTestName(name);
            if (this.isTest && !this.isIgnored) {
              methodList.add(method);
            }
            else {
              if (verbose && this.isIgnored) {
                logger.info("Test " + method + " is ignored for @Ignore annotation");
              }
            }
          }
        };
      }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.className = name.replace('/', '.');
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
      if (ANNO_IGNORE.equals(desc)) {
        this.classIgnored = true;
        if (this.verbose) {
          this.logger.info("Class " + className + " is ignored for @Ignore annotation");
        }
      }
      return null;
    }
  }
}
