package org.scalatest.tools.maven;

import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.scalatest.tools.maven.MojoUtils.*;

/**
 * Provides a bridge between Maven and the command-line form of ScalaTest's Runner.
 * Many of the configuration options available on this goal
 * are directly reflected in the Runner ScalaDoc on http://www.scalatest.org.
 *
 * @author Sean Griffin
 * @author Mike Pilquist
 * @author Jon-Anders Teigen
 * @author Bill Venners
 * @phase test
 * @goal test
 * @threadSafe
 */
public class TestMojo extends AbstractScalaTestMojo {

    /**
     * Output directory in which ScalaTest file reports should be written to.  Passed to ScalaTest via the -f argument.
     * @parameter default-value="${project.build.directory}/scalatest-reports" property="scalatest.reportsDirectory"
     * @required
     */
    File reportsDirectory;

    /**
     * Set to true to skip execution of tests.
     * @parameter property="skipTests"
     */
    boolean skipTests;

    /**
     * Set to true to avoid failing the build when tests fail
     * @parameter property="maven.test.failure.ignore"
     */
    boolean testFailureIgnore;

    /**
     * Comma separated list of filereporters. A filereporter consists of an optional
     * configuration and a mandatory filename, separated by a whitespace. E.g <code>all.txt,XE ignored_and_pending.txt</code>
     * For more info on configuring reporters, see the scalatest documentation.
     * @parameter property="filereports"
     */
    String filereports;

    /**
     * Comma separated list of htmlreporters.  An htmlreporter
     * consists of a mandatory directory and an optional css file
     * name, separated by whitespace. E.g:
     * <code>
     *   &lt;htmlreporters&gt;
     *     target/htmldir,
     *     target/myhtmldir src/my.css
     *   &lt;/htmlreporters&gt;
     * </code>
     * For more info on configuring reporters, see the scalatest documentation.
     * @parameter property="htmlreporters"
     */
    String htmlreporters;

    /**
     * Comma separated list of reporters. A reporter consist of an optional configuration
     * and a mandatory reporter classname, separated by whitespace. The reporter classname
     * must be the fully qualified name of a class extending <code>org.scalatest.Reporter</code>
     * E.g <code>C my.SuccessReporter,my.EverythingReporter</code>
     * For more info on configuring reporters, see the ScalaTest documentation.
     * @parameter property="reporters"
     */
    String reporters;

    /**
     * Comma separated list of junitxml. A junitxml consists of an optional configuration
     * and a mandatory directory for the xml files, separated by whitespace.
     * For more info on configuring reporters, see the scalatest documentation.
     * @parameter property="junitxml"
     */
    String junitxml;

    /**
     * Configuration for logging to stdout. (This logger is always enabled)
     * For more info on configuring reporters, see the scalatest documentation.
     * @parameter property="stdout"
     */
    String stdout;

    /**
     * Configuration for logging to stderr. It is disabled by default, but will be enabled
     * when configured. Empty configuration just means enable.
     * For more info on configuring reporters, see the scalatest documentation.
     * @parameter property="stderr"
     */
    String stderr;

    /**
     * Set this to "true" to redirect the unit test standard output to a file
     * (found in reportsDirectory/scalatest-output.txt by default).
     * <p>
     * NOTE: Works only if forkMode="once"
     *
     * @parameter property="maven.test.redirectTestOutputToFile" default-value="false"
     */
    boolean redirectTestOutputToFile;

    /**
     * Name of the file where the test output is redirected if enabled
     *
     * @parameter default-value="scalatest-output.txt"
     */
    String testOutputFileName;

    public void execute() throws MojoFailureException {
        getLog().info("ScalaTest report directory: " + reportsDirectory);

        if (skipTests) {
            getLog().info("Tests are skipped.");
        } else {
            if (!runScalaTest(configuration()) && !testFailureIgnore) {
                throw new MojoFailureException("There are test failures");
            }
        }
    }

    String[] configuration() {
        return concat(
                sharedConfiguration(),
                stdout(),
                stderr(),
                filereports(),
                htmlreporters(),
                reporters(),
                junitxml()
        );
    }

    // These private methods create the relevant portion of the command line
    // to pass to Runner based on the corresponding Maven configuration parameter.
    private List<String> stdout() {
        final String stdoutProcessed = maybeRemoveAnsiCodes(stdout);
        return unmodifiableList(singletonList(stdoutProcessed == null ? "-o" : "-o" + stdoutProcessed));
    }

    private List<String> stderr() {
        return stderr == null ? Collections.<String>emptyList() : unmodifiableList(singletonList("-e" + stderr));
    }

    private String maybeRemoveAnsiCodes(String streamParams) {
        return redirectTestOutputToFile
                ? maybeAppendLetter(streamParams, "W")
                : streamParams;
    }

    private String maybeAppendLetter(String string, String letter) {
        if (string == null) {
            return letter;
        }

        if (string.contains(letter)) {
            return string;
        }

        return string + letter;
    }

    private List<String> filereports() {
        return unmodifiableList(reporterArg("-f", filereports, fileRelativeTo(reportsDirectory)));
    }

    private List<String> htmlreporters() {
        List<String> r = new ArrayList<String>();

        for (String arg : splitOnComma(htmlreporters)) {
            String[] split = arg.split("\\s+");

            if (split.length > 0) {
                r.add("-h");
                r.add(split[0]);

                if (split.length > 1) {
                    r.add("-Y");
                    r.add(split[1]);
                }
            }
        }
        return unmodifiableList(r);
    }

    private List<String> reporters() {
        return reporterArg("-C", reporters, passThrough);
    }

    private List<String> junitxml(){
        return reporterArg("-u", junitxml, dirRelativeTo(reportsDirectory));
    }

    protected Writer getOutputWriter() throws MojoFailureException {
        return redirectTestOutputToFile ? newFileWriter() : super.getOutputWriter();
    }

    private Writer newFileWriter() throws MojoFailureException {
        final File outputFile = new File(reportsDirectory, testOutputFileName);

        if (!reportsDirectory.exists() && !reportsDirectory.mkdirs()) {
            throw new MojoFailureException("Unable to create directory path: " + reportsDirectory);
        }

        try {
            return new FileWriter(outputFile);
        } catch (IOException e) {
            throw new MojoFailureException("Unable to access the output file: '" + outputFile + "'", e);
        }
    }
}
