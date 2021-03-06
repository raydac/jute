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

import com.igormaznitsa.jute.TestContainer.TestResult;
import com.igormaznitsa.jute.runners.JUnitSingleTestMethodRunner;
import com.igormaznitsa.jute.runners.JUteSingleTestMethodRunner;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.*;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.*;
import org.apache.maven.artifact.Artifact;
import org.springframework.util.AntPathMatcher;

/**
 * Maven plugin allows to execute JUnit tests separately in external processes
 * per each test.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
@Mojo(name = "jute", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class JuteMojo extends AbstractMojo {

  private final ThreadPoolExecutor CACHED_EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

  private static final String TERMINAL_SECTION_START = "$$$89234098234-923598oiojadsfsldkfqwoiueq4190284";
  private static final String TERMINAL_SECTION_END = "$&^*@UYYI(*&(*@$(I@(*#@(**^&*&#$IUWYRWIHDKY(@#";
  private static final String SYNC_TEST_RESULT_PREFIX = "$$$*>";
  private static final String ASYNC_TEST_RESULT_PREFIX = ">$$$*>";
  private static final String[] EMPTY_STR = new String[0];

  static final String ANNO_TEST = "Lorg/junit/Test;";
  static final String ANNO_IGNORE = "Lorg/junit/Ignore;";
  static final String ANNO_JUTE = "Lcom/igormaznitsa/jute/annotations/JUteTest;";
  static final String JUNIT_SINGLE_RUNNER_CLASS = JUnitSingleTestMethodRunner.class.getName();
  static final String JUTE_SINGLE_RUNNER_CLASS = JUteSingleTestMethodRunner.class.getName();

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * List of test method names to be included into testing. Wildcard pattern can
   * be used. By default all non-ignored test methods are included.
   * <pre>
   * &lt;includeTests&gt;
   *   &lt;includeTest&gt;&#42Integration??Test&lt;/includeTest&gt;
   *   &lt;includeTest&gt;SomeMethodTest&lt;/includeTest&gt;
   * &lt;/includeTests&gt;
   * </pre>
   */
  @Parameter(name = "includeTests")
  private String[] includeTests;

  /**
   * List of test method names to be excluded from testing. Wildcard pattern can
   * be used. By default there is not any excluded test, if the test is
   * non-ignored one.
   * <pre>
   * &lt;excludeTests&gt;
   *   &lt;excludeTest&gt;&#42Integration??Test&lt;/excludeTest&gt;
   *   &lt;excludeTest&gt;SomeMethodTest&lt;/excludeTest&gt;
   * &lt;/excludeTests&gt;
   * </pre>
   */
  @Parameter(name = "excludeTests")
  private String[] excludeTests;

  /**
   * List of java test files to be included into testing. Ant path pattern can
   * be used. By default all classes are included.
   * <pre>
   * &lt;includes&gt;
   *   &lt;include&gt;/&#42;&#42;/Test&#42;.java&lt;/include&gt;
   *   &lt;include&gt;/TestIT&#42;.java&lt;/include&gt;
   * &lt;/includes&gt;
   * </pre>
   */
  @Parameter(name = "includes")
  private String[] includes;

  /**
   * List of java test files to be excluded from testing. Ant path pattern can
   * be used. By default there is not excluded any test.
   * <pre>
   * &lt;excludes&gt;
   *   &lt;exclude&gt;/&#42;&#42;/Test&#42;.java&lt;/exclude&gt;
   *   &lt;exclude&gt;/TestIT&#42;.java&lt;/exclude&gt;
   * &lt;/excludes&gt;
   * </pre>
   */
  @Parameter(name = "excludes")
  private String[] excludes;

  /**
   * Path to java interpreter or JRE folder or even command to be used to start
   * tests. By default will be used Java interpreter detected through the
   * 'java.home' system property
   */
  @Parameter(name = "java")
  private String java;

  /**
   * Show extra information about process.
   */
  @Parameter(name = "verbose", defaultValue = "false")
  private boolean verbose;

  /**
   * Timeout for testing process in milliseconds. If zero or less then execution
   * without timeout.
   */
  @Parameter(name = "timeout", defaultValue = "0")
  private long timeout;

  /**
   * Map of environment variables to be provided to staring processes. They will
   * be accessible through {@link java.lang.System#getenv(java.lang.String) }
   * <pre>
   * &lt;property&gt;
   *   &lt;name&gt;someKey&lt;/name&gt;
   *   &lt;value&gt;someValue&lt;/value&gt;
   * &lt;/property&gt;
   * </pre>
   */
  @Parameter(name = "env")
  private Properties env;

  /**
   * Map of Java system properties which will be provided to started process.
   * They will be accessible through
   * {@link java.lang.System#getProperty(java.lang.String)}
   * <pre>
   * &lt;property&gt;
   *   &lt;name&gt;someKey&lt;/name&gt;
   *   &lt;value&gt;someValue&lt;/value&gt;
   * &lt;/property&gt;
   * </pre>
   */
  @Parameter(name = "javaProperties")
  private Properties javaProperties;

  /**
   * Enforce output of test process terminal streams. By default console text of
   * process is printed only if there is an error, the parameter makes plugin
   * print console data every time.
   */
  @Parameter(name = "enforcePrintConsole")
  private boolean enforcePrintConsole;

  /**
   * Start only tests marked by @com.igormaznitsa,jute.annotations.JUTeTest
   * (provided by jute-annotations artifact), both @Test and @Ignore JUnit
   * annotations will be ignored by JUte.
   */
  @Parameter(name = "onlyAnnotated", defaultValue = "false")
  private boolean onlyAnnotated;

  /**
   * Parameter allows to skip tests. It provides value of 'skipTests' property.
   */
  @Parameter(name = "skipTests", property = "skipTests", defaultValue = "false")
  private boolean skipTests;

  /**
   * Global parameter to skip all tests entirely. It provides value of
   * 'maven.test.skip' property.
   */
  @Parameter(name = "skip", property = "maven.test.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Parameter provides value of the 'jute.test' property and if defined then
   * the wildcarded value will be filtering test(s). Mainly this parameter for
   * start test methods separately through CLI. It works similar to 'test'
   * property for Surefire.
   */
  @Parameter(name = "juteTest", property = "jute.test", defaultValue = "")
  private String juteTest;

  /**
   * Provides extra options for JVM.
   * <pre>
   * &lt;jvmOptions&gt;
   *   &lt;jvmOption&gt;-Xss256k&lt;/jvmOption&gt;
   *   &lt;jvmOption&gt;-Xmx100m&lt;/jvmOption&gt;
   * &lt;/jvmOptions&gt;
   * </pre>
   */
  @Parameter(name = "jvmOptions")
  private String[] jvmOptions;

  /**
   * Text to be sent to started processes through System.in as bytes decoded in
   * default char-set.
   */
  @Parameter(name = "in")
  private String in;

  /**
   * The value contains folder of compiled test classes.
   */
  @Parameter(name = "testClassesDirectory", defaultValue = "${project.build.testOutputDirectory}")
  private File testClassesDirectory;

  /**
   * The value contains folder of compiled project classes.
   */
  @Parameter(name = "classesDirectory", defaultValue = "${project.build.outputDirectory}")
  private File classesDirectory;

  public File getTestClassesDirectory() {
    return this.testClassesDirectory;
  }

  public File getClassesDirectory() {
    return this.classesDirectory;
  }

  public String getIn() {
    return this.in;
  }

  public String getJUteTest() {
    return this.juteTest;
  }

  public boolean isSkip() {
    return this.skip;
  }

  public boolean isSkipTests() {
    return this.skipTests;
  }

  public boolean isOnlyAnnotated() {
    return this.onlyAnnotated;
  }

  public String[] getJvmOptions() {
    return this.jvmOptions == null ? null : this.jvmOptions.clone();
  }

  public String[] getIncludeTests() {
    return this.includeTests == null ? null : this.includeTests.clone();
  }

  public String[] getExcludeTests() {
    return this.excludeTests == null ? null : this.excludeTests.clone();
  }

  public String[] getIncludes() {
    return this.includes == null ? null : this.includes.clone();
  }

  public String[] getExcludes() {
    return this.excludes == null ? null : this.excludes.clone();
  }

  public Properties getJavaProperties() {
    return this.javaProperties;
  }

  public String getJava() {
    return this.java;
  }

  public boolean isVerbose() {
    return this.verbose;
  }

  public long getTimeout() {
    return this.timeout;
  }

  public boolean isEnforcePrintConsole() {
    return this.enforcePrintConsole;
  }

  public Properties getEnv() {
    return this.env;
  }

  private static List<String> collectAllPotentialTestClassPaths(final Log log, final boolean verbose, final File rootFolder, final String[] includes, final String[] excludes) {
    final List<String> result = new ArrayList<String>();

    final Iterator<File> iterator = FileUtils.iterateFiles(rootFolder, new IOFileFilter() {
      private final AntPathMatcher matcher = new AntPathMatcher();

      @Override
      public boolean accept(final File file) {
        if (file.isDirectory()) {
          return false;
        }

        final String path = file.getAbsolutePath();
        boolean include = false;

        if (path.endsWith(".class")) {
          if (includes.length != 0) {
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

          if (include && excludes.length != 0) {
            for (final String pattern : excludes) {
              if (matcher.match(pattern, path)) {
                include = false;
                break;
              }
            }
          }
        }

        if (!include && verbose) {
          log.info("File " + path + " excluded");
        }

        return include;
      }

      @Override
      public boolean accept(final File dir, final String name) {
        final String path = name;
        boolean include = false;
        if (includes.length != 0) {
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

        if (include && excludes.length != 0) {
          for (final String pattern : excludes) {
            if (matcher.match(pattern, path)) {
              include = false;
              break;
            }
          }
        }

        if (!include && verbose) {
          log.info("Folder " + name + " excluded");
        }

        return include;
      }
    }, DirectoryFileFilter.DIRECTORY);

    while (iterator.hasNext()) {
      final String detectedFile = iterator.next().getAbsolutePath();
      if (verbose) {
        log.info("Found potential test class : " + detectedFile);
      }
      result.add(detectedFile);
    }

    if (result.isEmpty()) {
      log.warn("No test files found in " + rootFolder.getAbsolutePath());
    }

    return result;
  }

  private static String[] normalizeStringArray(final String[] array) {
    String[] result;
    if (array == null) {
      result = EMPTY_STR;
    }
    else {
      result = new String[array.length];
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
    }
    return result;
  }

  private static String getPathInsideJDK() {
    return SystemUtils.IS_OS_WINDOWS ? "\\bin\\java.exe" : "/bin/java";
  }

  private static File getFilePathToJVMInterpreter(final String jvmInterpreter) {
    File result;
    if (jvmInterpreter == null) {
      result = new File(new File(System.getProperty("java.home")), getPathInsideJDK());
    }
    else {
      try {
        result = new File(jvmInterpreter);
        if (!result.isFile()) {
          if (result.isDirectory()) {
            result = new File(result, getPathInsideJDK());
            if (!result.isFile()) {
              result = null;
            }
          }
          else {
            result = null;
          }
        }
      }
      catch (Exception ex) {
        result = null;
      }
    }
    return result;
  }

  private Collection<File> getClassPathAsFiles() {
    final List<File> result = new ArrayList<File>();
    if (this.testClassesDirectory != null) {
      result.add(this.testClassesDirectory);
    }
    if (this.classesDirectory != null) {
      result.add(this.classesDirectory);
    }

    for (final Artifact a : project.getArtifacts()) {
      if (a.getArtifactHandler().isAddedToClasspath()) {
        File file = a.getFile();
        if (file != null) {
          result.add(file);
        }
      }
    }
    return result;
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
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (isSkipExecution()) {
      getLog().info("Tests are skipped.");
      return;
    }

    final File testFolder = this.testClassesDirectory;
    if (!testFolder.isDirectory()) {
      getLog().info("No test folder");
      return;
    }
    getLog().info("Project test folder : " + testFolder.getAbsolutePath());

    final File pathToMojoJar;
    try {
      pathToMojoJar = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
    }
    catch (URISyntaxException ex) {
      throw new MojoExecutionException("Can't get path to the Mojo jar", ex);
    }

    final File javaInterpreter = getFilePathToJVMInterpreter(this.java);
    final String testClassPath = makeClassPath(pathToMojoJar, getClassPathAsFiles());

    final TestContainer baseTestConfig = new TestContainer(null, null, null, javaInterpreter == null ? this.java : javaInterpreter.getAbsolutePath(), this.jvmOptions, this.in, -1, this.enforcePrintConsole, false, this.timeout);

    final List<String> collectedTestFilePaths = collectAllPotentialTestClassPaths(getLog(), this.verbose, testFolder, normalizeStringArray(this.includes), normalizeStringArray(this.excludes));
    final Map<TestClassProcessor, List<TestContainer>> extractedTestMethods = new HashMap<TestClassProcessor, List<TestContainer>>();
    try {
      fillListByTestMethods(baseTestConfig, testFolder, collectedTestFilePaths, extractedTestMethods);
    }
    catch (IOException ex) {
      throw new MojoExecutionException("Can't scan test classes", ex);
    }

    final long startTime = System.currentTimeMillis();

    getLog().info("Global Java options: " + (this.jvmOptions == null ? "<not provided>" : Arrays.toString(this.jvmOptions)));
    if (javaInterpreter == null) {
      getLog().info("JVM interpreter as command: " + this.java);
    }
    else {
      getLog().info("Global JVM interpreter as path: " + javaInterpreter.getAbsolutePath());
    }

    if (this.juteTest != null && !this.juteTest.isEmpty()) {
      getLog().info("NB! Defined juteTest : " + this.juteTest);
    }

    getLog().info("Test class path: " + testClassPath);
    getLog().info(this.timeout <= 0L ? "No Timeout" : "Timeout is " + this.timeout + " ms");
    getLog().info("Detected " + Utils.calcNumberOfItems(extractedTestMethods) + " potential test method(s)");
    getLog().info("");

    final AtomicInteger startedCounter = new AtomicInteger();
    final AtomicInteger errorCounter = new AtomicInteger();
    final AtomicInteger skippedCounter = new AtomicInteger();

    int maxTestNameLength = 0;
    for (final Map.Entry<TestClassProcessor, List<TestContainer>> e : extractedTestMethods.entrySet()) {
      for (final TestContainer test : e.getValue()) {
        if (maxTestNameLength < test.getMethodName().length()) {
          maxTestNameLength = test.getMethodName().length();
        }
      }
    }

    for (final Map.Entry<TestClassProcessor, List<TestContainer>> e : extractedTestMethods.entrySet()) {
      if (e.getValue().isEmpty()) {
        continue;
      }

      getLog().info(e.getKey().getClassName());
      getLog().info(" " + (char) 0x2502);

      int nextTestIndex = 0;

      final List<String> logStrings = new ArrayList<String>();

      while (!Thread.currentThread().isInterrupted() && nextTestIndex < e.getValue().size()) {
        try {
          logStrings.clear();
          final int prevStartIndex = nextTestIndex;
          final int numberOfExecuted = executeNextTestsFromList(logStrings, maxTestNameLength, testClassPath, e.getValue(), prevStartIndex, startedCounter, errorCounter, skippedCounter);
          getLog().debug("Executed " + numberOfExecuted + " test(s)");
          printExecutionResultIntoLog(nextTestIndex + numberOfExecuted >= e.getValue().size(), logStrings);
          nextTestIndex += numberOfExecuted;
        }
        catch (Throwable ex) {
          throw new MojoExecutionException("Critical error during a test method execution", ex);
        }
      }
      getLog().info("");
    }

    final long delay = System.currentTimeMillis() - startTime;
    getLog().info(String.format("Tests run: %d, Errors: %d, Skipped: %d, Total time: %s", startedCounter.get(), errorCounter.get(), skippedCounter.get(), Utils.printTimeDelay(delay)));

    if (errorCounter.get() != 0) {
      throw new MojoFailureException("Detected failed tests, see session log");
    }
  }

  private static String extractTestNameFromLogString(final String text) {
    if (text.startsWith(SYNC_TEST_RESULT_PREFIX)) {
      return text.substring(SYNC_TEST_RESULT_PREFIX.length());
    }
    if (text.startsWith(ASYNC_TEST_RESULT_PREFIX)) {
      return text.substring(ASYNC_TEST_RESULT_PREFIX.length());
    }
    return text;
  }

  private void printExecutionResultIntoLog(final boolean endTestBunch, final List<String> result) {
    int numberOfTestsInLog = 0;
    for (final String s : result) {
      numberOfTestsInLog += (s.startsWith(SYNC_TEST_RESULT_PREFIX) || s.startsWith(ASYNC_TEST_RESULT_PREFIX)) ? 1 : 0;
    }

    int testIndex = 0;
    int line = 0;

    while (line < result.size()) {
      final boolean lastTest = testIndex == numberOfTestsInLog - 1;
      final String str = result.get(line++);

      if (str.startsWith(SYNC_TEST_RESULT_PREFIX) || str.startsWith(ASYNC_TEST_RESULT_PREFIX)) {
        final boolean syncTask = str.startsWith(SYNC_TEST_RESULT_PREFIX);
        testIndex++;
        final String substr = extractTestNameFromLogString(str);

        final String prefix;
        if (lastTest && endTestBunch) {
          if (syncTask) {
            prefix = " " + (char) 0x2514 + (char) 0x2504;
          }
          else {
            prefix = " " + (char) 0x2558 + (char) 0x2550;
          }
        }
        else {
          if (syncTask) {
            prefix = " " + (char) 0x251C + (char) 0x2504;
          }
          else {
            prefix = " " + (char) 0x255E + (char) 0x2550;
          }
        }
        getLog().info(prefix + substr);
      }
      else if (str.equals(TERMINAL_SECTION_START)) {
        final List<String> terminal = new ArrayList<String>();
        int maxWidth = 0;
        while (true) {
          final String termStr = result.get(line++);
          if (termStr.equals(TERMINAL_SECTION_END)) {
            break;
          }
          terminal.add(termStr);
          maxWidth = Math.max(termStr.length(), maxWidth);
        }
        maxWidth += 10;

        getLog().info((char) 0x250F + Utils.makeStr(maxWidth, (char) 0x2501) + (char) 0x2513);
        boolean error = false;
        for (final String s : terminal) {
          if (error) {
            getLog().error(" " + s);
          }
          else {
            getLog().info(" " + s);
            if (s.contains((char) 0x2563 + "Error" + (char) 0x2560)) {
              error = true;
            }
          }
        }
        getLog().info((char) 0x2517 + Utils.makeStr(maxWidth, (char) 0x2501) + (char) 0x251B);
      }
      else {
        getLog().warn("Unexpected log string: " + str);
      }
    }
  }

  private static List<String> makeTestResultReference(final boolean syncTest, final TestContainer test, final long durationInMilliseconds, final int maxTestNameLength, final TestResult testResult, final String terminal) {
    final List<String> result = new ArrayList<String>();
    final StringBuilder buffer = new StringBuilder();
    buffer.append(syncTest ? SYNC_TEST_RESULT_PREFIX : ASYNC_TEST_RESULT_PREFIX).append(test.getMethodName());
    final int len = maxTestNameLength + 5;
    buffer.append(Utils.makeStr(len - test.getMethodName().length(), '.'));
    buffer.append(testResult.name());
    if (testResult != TestResult.SKIPPED && durationInMilliseconds >= 0L) {
      buffer.append(' ').append('(').append(Utils.printTimeDelay(durationInMilliseconds)).append(')');
    }

    result.add(buffer.toString());
    buffer.setLength(0);

    if (terminal != null) {
      final String[] splittedTerminal = terminal.split("\\n");
      result.add(TERMINAL_SECTION_START);
      for (final String s : splittedTerminal) {
        result.add(s);
      }
      result.add(TERMINAL_SECTION_END);
    }

    return result;
  }

  private int executeNextTestsFromList(final List<String> logStrings, final int maxTestNameLength, final String testClassPath, final List<TestContainer> testContainers, final int startIndex, final AtomicInteger startedCounter, final AtomicInteger errorCounter, final AtomicInteger skippedCounter) throws Exception {
    final List<TestContainer> toExecute = new ArrayList<TestContainer>();

    int detectedOrder = -1;

    for (int i = startIndex; i < testContainers.size(); i++) {
      final TestContainer c = testContainers.get(i);
      if (detectedOrder < 0) {
        if (c.getOrder() < 0) {
          toExecute.add(c);
        }
        else {
          if (toExecute.isEmpty()) {
            detectedOrder = c.getOrder();
            toExecute.add(c);
          }
          else {
            break;
          }
        }
      }
      else {
        if (c.getOrder() == detectedOrder) {
          toExecute.add(c);
        }
        else {
          break;
        }
      }
    }
    final CountDownLatch counterDown;

    if (detectedOrder >= 0 && toExecute.size() > 1) {
      counterDown = new CountDownLatch(toExecute.size());
    }
    else {
      counterDown = null;
    }

    final List<Throwable> thrownErrors = Collections.synchronizedList(new ArrayList<Throwable>());

    for (final TestContainer container : toExecute) {
      final Runnable run = new Runnable() {
        @Override
        public void run() {
          final long startTime = System.currentTimeMillis();
          try {
            getLog().debug("Start execution: " + container.toString());
            startedCounter.incrementAndGet();
            final TestResult result = container.executeTest(getLog(), onlyAnnotated, maxTestNameLength, testClassPath, javaProperties, env);
            final long endTime = System.currentTimeMillis();
            switch (result) {
              case ERROR:
              case TIMEOUT: {
                errorCounter.incrementAndGet();
              }
              break;
              case SKIPPED: {
                skippedCounter.incrementAndGet();
              }
              break;
            }

            if (logStrings != null) {
              final boolean printConsoleLog = result != TestResult.OK || container.isPrintConsole();
              synchronized (logStrings) {
                logStrings.addAll(makeTestResultReference(counterDown == null, container, endTime - startTime, maxTestNameLength, result, (printConsoleLog ? container.getLastTerminalOut() : null)));
              }
            }
          }
          catch (Throwable thr) {
            getLog().debug("Error during execution " + container.toString(), thr);
            thrownErrors.add(thr);
          }
          finally {
            getLog().debug("End execution: " + container.toString());
            if (counterDown != null) {
              counterDown.countDown();
            }
          }
        }
      };
      if (counterDown == null) {
        getLog().debug("Sync.execution: " + container.toString());
        run.run();
      }
      else {
        getLog().debug("Async.execution: " + container.toString());
        CACHED_EXECUTOR.execute(run);
      }
    }
    if (counterDown != null) {
      try {
        counterDown.await();
      }
      catch (InterruptedException ex) {
        getLog().error(ex);
      }
    }
    if (!thrownErrors.isEmpty()) {
      for (final Throwable thr : thrownErrors) {
        getLog().error(thr);
      }
    }

    return toExecute.size();
  }

  private boolean isSkipExecution() {
    return this.isSkip() || this.isSkipTests();
  }

  private void fillListByTestMethods(final TestContainer base, final File testRoot, final List<String> testClassFilePaths, final Map<TestClassProcessor, List<TestContainer>> detectedTestMap) throws IOException {
    final String testRootPath = FilenameUtils.normalize(testRoot.getAbsolutePath() + File.separatorChar);

    for (final String classFilePath : testClassFilePaths) {
      final String normalizedClassFilePath = FilenameUtils.normalize(classFilePath);
      final String standardJavaClassName = Utils.toStandardJavaClassName(testRootPath, normalizedClassFilePath);

      getLog().debug("Process class file : " + normalizedClassFilePath + '[' + standardJavaClassName + ']');

      if (!Utils.checkClassAndMethodForPattern(this.juteTest, standardJavaClassName, "", true)) {
        getLog().debug("Excluded for 'juteTest' (" + this.juteTest + ')');
        continue;
      }

      final InputStream classInStream = new FileInputStream(normalizedClassFilePath);
      try {
        final ClassReader classReader = new ClassReader(classInStream);
        final List<TestContainer> listOfDetectedMethods = new ArrayList<TestContainer>();

        final TestClassProcessor tcv = new TestClassProcessor(this.onlyAnnotated, this.juteTest, normalizedClassFilePath, base, this.getLog(), listOfDetectedMethods, normalizeStringArray(this.includeTests), normalizeStringArray(this.excludeTests));
        classReader.accept(tcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        getLog().debug("Class file " + normalizedClassFilePath + " has " + listOfDetectedMethods.size() + " detected methods");

        detectedTestMap.put(tcv, Utils.sortDetectedClassMethodsForNameAndOrder(listOfDetectedMethods));
      }
      finally {
        IOUtils.closeQuietly(classInStream);
      }
    }
  }

}
