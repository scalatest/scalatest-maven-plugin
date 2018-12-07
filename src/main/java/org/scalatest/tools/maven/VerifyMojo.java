package org.scalatest.tools.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * A verification that all the tests passed.
 * @phase verify
 * @goal verify
 */
public class VerifyMojo extends AbstractScalaTestMojo {

  /**
   * The summary file to read integration test results from.
   * @parameter default-value="${project.build.directory}/surefire-reports/scalatest-summary.txt" property="scalatest.reportsDirectory"
   */
  private File summaryFile;

  public void execute() throws MojoExecutionException {
      try {
        File reportFile = new File(summaryFile.getAbsolutePath());
        // FileReader reads text files in the default encoding.
        FileReader fileReader =
            new FileReader(reportFile);

        // Always wrap FileReader in BufferedReader.
        BufferedReader bufferedReader =
            new BufferedReader(fileReader);
        if(bufferedReader.readLine().contentEquals("failure")) {
          bufferedReader.close();
          throw new MojoExecutionException("There are test failures");
        } else {
          bufferedReader.close();
          throw new MojoExecutionException("File has invalid content");
        }
      }
      catch(FileNotFoundException ex) {
        throw new MojoExecutionException("Cannot find file: "+summaryFile.getAbsolutePath());
      }
      catch(IOException ex) {
        throw new MojoExecutionException("IOException: "+ ex.toString());
      }
    //}
  }
}
