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

    <!-- disable surefire -- >
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>2.7</version>
      <configuration>
        <skipTests>true</skipTests>
      </configuration>
    </plugin>
    <!-- enable scalatest -- >
    <plugin>
      <groupId>org.scalatest</groupId>
      <artifactId>scalatest-maven-plugin</artifactId>
      <version>2.0.2</version>
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

Ignore When ScalaTest Not Available
-----------------------------------

This plugin expects the you to include ScalaTest dependency used by the your project.  By default, if you do not include a ScalaTest dependency, 
this plugin will fail the build, if you would like to ignore or skip running this plugin when ScalaTest is not in the classpath, you can set 
**noScalaTestIgnore** to **true** in configuration like this: 

```
...
<configuration>
  <noScalaTestIgnore>true</noScalaTestIgnore>
</configuration>
...
```

Deploying to Sonatype
---------------------

Add the following into settings.xml (please replace username and password): 

```
<servers>
  <server>
    <id>sonatype-nexus-staging</id>
    <username>xxx</username>
    <password>yyy</password>
  </server>
</servers>
```

Then run the following command: 

```
> mvn clean deploy -Dmaven.test.skip=true
```