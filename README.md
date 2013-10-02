scalatest-maven-plugin
======================

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
      <artifactId>maven-scalatest-plugin</artifactId>
      <version>1.0-M2</version>
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
