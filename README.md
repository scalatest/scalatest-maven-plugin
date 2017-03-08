ScalaTest Maven Plugin
======================

Building ScalaTest Maven Plugin
-------------------------------

Maven 3 is used to manage the build process.  To build this plugin, please make sure you have the following installed:-

  * The Git command line tools
  * A recent JDK (the [current Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) is recommended)
  * Maven 3 (http://maven.apache.org/download.html)

You then clone and checkout master trunk:-

    $ git clone git://github.com/scalatest/scalatest-maven-plugin.git
    
    $ cd scalatest-maven-plugin

Finally use the following commands to build for ScalaTest Maven Plugin: 

    $ mvn clean package

The built output will be available in target/.

Using ScalaTest Maven Plugin
----------------------------

To use the ScalaTest Maven plugin, you need to disable SureFire and enable ScalaTest. Here's an example of how to do this in your pom.xml: 

```xml
    <!-- disable surefire -- >
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>2.19.1</version>
      <configuration>
        <skipTests>true</skipTests>
      </configuration>
    </plugin>
    
    <!-- enable scalatest -- >
    <plugin>
      <groupId>org.scalatest</groupId>
      <artifactId>maven-scalatest-plugin</artifactId>
      <version>1.1.1</version>
      <configuration>
        <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
        <junitxml>.</junitxml>
        <filereports>WDF TestSuite.txt</filereports>
      </configuration>
      <executions>
        <execution>
          <id>test</id>
          <goals>
            <goal>test</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    
    <!-- Enable scalatest with suffixes -- >
    <plugin>
    <groupId>org.scalatest</groupId>
    <artifactId>scalatest-maven-plugin</artifactId>
    <version>1.1.1</version>
    <configuration>
        <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
        <junitxml>.</junitxml>
        <filereports>WDF TestSuite.txt</filereports>
    </configuration>
    <executions>
        <execution>
            <id>test</id>
            <goals>
                <goal>test</goal>
            </goals>
            <configuration>
                <suffixes>(?&lt;!Integration)(Test|Spec)</suffixes>
            </configuration>
        </execution>
        <execution>
            <id>integration-test</id>
            <phase>integration-test</phase>
            <goals>
                <goal>test</goal>
            </goals>
            <configuration>
                <suffixes>(?&lt;=Integration)(Test|Spec)</suffixes>
            </configuration>
        </execution>
    </executions>
    </plugin>
```