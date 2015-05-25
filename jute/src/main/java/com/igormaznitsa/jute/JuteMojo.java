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

import com.igormaznitsa.jute.exceptions.IgnoredTestException;
import com.igormaznitsa.jute.runners.JUnitSingleTestMethodRunner;
import com.igormaznitsa.jute.runners.JUteSingleTestMethodRunner;
import com.jcabi.aether.Classpath;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
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

/**
 * Maven plugin allows to execute JUnit tests separately in external processes
 * per each test.
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
@Mojo(name = "jute", defaultPhase = LifecyclePhase.TEST, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class JuteMojo extends AbstractMojo {

  private static final String [] EMPTY_STR = new String[0];
  
  static final String ANNO_TEST = "Lorg/junit/Test;";
  static final String ANNO_IGNORE = "Lorg/junit/Ignore;";
  static final String ANNO_JUTE = "Lcom/igormaznitsa/jute/annotations/JUteTest;";
  static final String JUNIT_SINGLE_RUNNER_CLASS = JUnitSingleTestMethodRunner.class.getName();
  static final String JUTE_SINGLE_RUNNER_CLASS = JUteSingleTestMethodRunner.class.getName();

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

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
   * Path to JRE folder to be used for tests, also path to executable file can
   * be provided. By default the Maven JRE will be used. Value must contain path
   * to JRE folder or to Java executable file, don't provide path to commands.
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
   * Start only tests marked by @com.igormaznitsa,jute.annotations.JUTeTest annotation
   * and ignore JUnit tests.
   */
  @Parameter(name = "onlyAnnotated",defaultValue = "false")
  private boolean onlyAnnotated;
  
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

  public String getIn() {
    return this.in;
  }

  public boolean isOnlyAnnotated(){
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
    String[] result = null;
    if (array == null){
      result = EMPTY_STR;
    } else {
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

  private static File getFilePathToJVMInterpreter(final String jvmInterpreter) {
    File result;
    if (jvmInterpreter == null) {
      final String name = SystemUtils.IS_OS_WINDOWS ? "\\bin\\java.exe" : "/bin/java";
      result = new File(new File(System.getProperty("java.home")), name);
    }
    else {
      try {
        result = new File(jvmInterpreter);
        if (!result.isFile()) {
          result = null;
        }
      }
      catch (Exception ex) {
        result = null;
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
  public void execute() throws MojoExecutionException {
    final File testFolder = new File(this.project.getBuild().getTestOutputDirectory());
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
    final String testClassPath = makeClassPath(pathToMojoJar, new Classpath(project, new File(session.getLocalRepository().getBasedir()), "test"));

    final TestContainer baseTestConfig = new TestContainer(null, null, null, javaInterpreter == null ? this.java : javaInterpreter.getAbsolutePath(), this.jvmOptions, this.in, -1, this.enforcePrintConsole, false, this.timeout);

    final List<String> collectedTestFilePaths = collectAllPotentialTestClassPaths(getLog(), this.verbose, testFolder, normalizeStringArray(this.includes), normalizeStringArray(this.excludes));
    final List<TestContainer> extractedTestMethods = new ArrayList<TestContainer>();
    try {
      fillListByTestMethods(baseTestConfig, collectedTestFilePaths, extractedTestMethods);
    }
    catch (IOException ex) {
      throw new MojoExecutionException("Can't scan test classes", ex);
    }

    final long startTime = System.currentTimeMillis();

    if (this.verbose) {
      getLog().info("Global Java options: " + (this.jvmOptions == null ? "<not provided>" : Arrays.toString(this.jvmOptions)));
      if (javaInterpreter == null) {
        getLog().info("Java interpreter command: " + this.java);
      }
      else {
        getLog().info("Java interpreter path: " + javaInterpreter.getAbsolutePath());
      }
      getLog().info("Test class path: " + testClassPath);
      getLog().info("Detected " + extractedTestMethods.size() + " test method(s)");
      getLog().info(this.timeout < 0L ? "No Timeout" : "Timeout is " + this.timeout + " ms");
    }

    int startedCounter = 0;
    int errorCounter = 0;

    int maxTestNameLength = 0;
    for (final TestContainer test : extractedTestMethods) {
      if (maxTestNameLength < test.toString().length()) {
        maxTestNameLength = test.toString().length();
      }
    }

    for (final TestContainer test : extractedTestMethods) {
      try {
        startedCounter++;
        if (!test.executeTest(getLog(), this.onlyAnnotated, maxTestNameLength, testClassPath, this.javaProperties, this.env)) {
          errorCounter++;
        }
      }
      catch(IgnoredTestException ex){
        
      }
      catch (Throwable ex) {
        throw new MojoExecutionException("Error during test method execution '" + test + '\'', ex);
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

  private void fillListByTestMethods(final TestContainer base, final List<String> testClassFilePaths, final List<TestContainer> listOfDetectedMethods) throws IOException {
    for (final String s : testClassFilePaths) {
      final InputStream classInStream = new FileInputStream(s);
      try {
        final ClassReader classReader = new ClassReader(classInStream);
        classReader.accept(new TestClassVisitor(s, base, this.getLog(), this.verbose, listOfDetectedMethods, normalizeStringArray(this.includeTests), normalizeStringArray(this.excludeTests)), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      }
      finally {
        IOUtils.closeQuietly(classInStream);
      }
    }
  }

}
