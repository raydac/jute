# What is it?
It is a small specific Maven plugin (of course it is relative small one because it of course will download and save on your machine a half-of-internet as dependencies) for specific testing tasks.

# What is the specific tasks?
Let's imagine that we want to write JUnit tests for some code which has so bizzare structure and architecture that it is very expensive to dig it and avoid influence of one test to another, may be it is a legacy code or just a system with very very complex behaviour. All your tests work separately but they are red in bunch execution.   

In such situation you can use Jute. the Plugin works during test phase and executes all methods of JUnit tests as external separated processes. It finds all test methods marked as @Test and ignores all test methods marked by @Ignore. You can also exclude (or include) from its processing whole test classes or methods. Also it provides possibility to set different JRE for external processes start. Please remember that it works only with JUnit annotations.

# How to bring Jute in my project?
It is very easy, the plugin has been published in the Maven Central and you can just add its reference in pom.xml
```
<build>
  <plugins>
...
    <plugin>
      <groupId>com.igormaznitsa</groupId>
      <artifactId>jute</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <goals>
            <goal>jute</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
...
  </plugins>
</build>
```
after that JUte will be executing every test phase of your project.

# Hey! I want to use it only for several classes!
No problems, just define name of your class in __includes__ section of the plugin configuration
```
<build>
  <plugins>
...
    <plugin>
      <groupId>com.igormaznitsa</groupId>
      <artifactId>jute</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <goals>
            <goal>jute</goal>
          </goals>
          <configuration>
            <includes>
              <include>/**/MyTestWhichShouldBeStartedInExternalProcess.java</include>
            </includes>
          </configuration>
        </execution>
      </executions>
    </plugin>
...
  </plugins>
</build>
```
In the case onle test methods defined in file named MyTestWhichShouldBeStartedInExternalProcess will be processed by Jute.

# I don't want execute all tests, only a pair!
Ok! In the case you just should define names or a mask for your test method names.
```
<build>
  <plugins>
...
    <plugin>
      <groupId>com.igormaznitsa</groupId>
      <artifactId>jute</artifactId>
      <version>1.0.0</version>
      <executions>
        <execution>
          <goals>
            <goal>jute</goal>
          </goals>
          <configuration>
            <includes>
              <include>/**/MyTestWhichShouldBeStartedInExternalProcess.java</include>
            </includes>
              <includeTests>
                <includeTest>*JuteTest</includeTest>
              </includeTests>
          </configuration>
        </execution>
      </executions>
    </plugin>
...
  </plugins>
</build>
```
After that, only test methods suffixed by JuteTest will be executed by Jute.

# It doesn't process my methods and I have not idea why!
In the case start your maven process with -X (it will turn on debug logging) and add ```<verbose>true</verbose>``` into Jute configuration

# It prints console output only if there is an error! and I want see test output everytime!
No problems! Just add flag to enforce pring console output of external processes into maven log ```<enforcePrintConsole>true</enforcePrintConsole>``` and you will see external process console output every time.
