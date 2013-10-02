package org.scalatest.tools.maven;

import org.codehaus.doxia.sink.Sink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.scalatest.tools.maven.MojoUtils.*;

/**
 * A reporting mojo to capture the ScalaTest output as a file that integrates into the Maven site of a project.
 *
 * @author Jon-Anders Teigen
 * @author Sean Griffin
 * @author Mike Pilquist
 * @author Bill Venners
 * @phase site
 * @goal reporter
 */
public class ReporterMojo extends AbstractScalaTestMojo implements MavenReport {

    /**
     * Directory where reports will go.
     *
     * @parameter expression="${project.reporting.outputDirectory}"
     * @required
     * @readonly
     */
    private File reportingOutputDirectory;

    /**
     * Consists of an optional configuration parameters for the file reporter.
     * For more info on configuring reporters, see the ScalaTest documentation.
     * @parameter expression="${fileReporterOptions}"
     */
    private String fileReporterOptions;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // no op, Maven doesn't even call this method but I have to implement it because it's on the interface.
    }

    public void generate(Sink sink, Locale locale) throws MavenReportException {
        try {
            runScalaTest(configuration());
        }
        catch (MojoFailureException e) {
            throw new MavenReportException("Failure executing ScalaTest", e);
        }
        catch (RuntimeException e) {
            throw new MavenReportException("Failure executing ScalaTest", e);
        }

        // ScalaTest outputs plain text but the Mojo requires HTML output so we'll create a bare-bones HTML doc and
        // embed the ScalaTest output inside a <pre> in that doc.

        try {
            sink.head();
            sink.title();
            sink.text(getLocalizedString(locale, "reporter.mojo.outputTitle"));
            sink.title_();
            sink.head_();

            sink.body();
            sink.sectionTitle1();
            sink.text(getLocalizedString(locale, "reporter.mojo.outputTitle"));
            sink.sectionTitle1_();
            sink.verbatim(false);
            sink.text(getScalaTestOutputFromFile());
            sink.verbatim_();
            sink.body_();

            sink.flush();
            sink.close();
        }
        catch (IOException ioe) {
            throw new MavenReportException("Failure generating ScalaTest report", ioe);
        }
    }

    private String[] configuration() {
        return concat(
                sharedConfiguration(),
                fileReporterConfig()
        );
    }

    private List<String> fileReporterConfig() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        if (fileReporterOptions != null) {
            return reporterArg("-f", fileReporterOptions + " tempScalaTestOutput.txt", fileRelativeTo(tmpDir));
        }
        return reporterArg("-f", "tempScalaTestOutput.txt", fileRelativeTo(tmpDir));
    }

    private String getScalaTestOutputFromFile() throws IOException {
        StringWriter fileContents = new StringWriter(1024);
        PrintWriter writer = new PrintWriter(fileContents, true);

        // ScalaTest's FileReporter uses default character encoding so that's what we'll use here, too.
        File outputFile = new File(System.getProperty("java.io.tmpdir"), "tempScalaTestOutput.txt");
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));

        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
            return fileContents.toString();
        }
        finally {
            try {
                reader.close();
                outputFile.delete();
            }
            catch (IOException ignored) {}
        }
    }

    public String getOutputName() {
        return "scalatest-output";
    }

    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    public String getName(Locale locale) {
        return getLocalizedString(locale, "reporter.mojo.name");
    }

    public String getDescription(Locale locale) {
        return getLocalizedString(locale, "reporter.mojo.description");
    }

    public void setReportOutputDirectory(File outputDirectory) {
        reportingOutputDirectory = outputDirectory;
    }

    public File getReportOutputDirectory() {
        return reportingOutputDirectory;
    }

    public boolean isExternalReport() {
        return false;
    }

    public boolean canGenerateReport() {
        return true;
    }

    private String getLocalizedString(Locale locale, String resourceKey) {
        return ResourceBundle.getBundle("mojoResources", locale, getClass().getClassLoader()).getString(resourceKey);
    }
}
