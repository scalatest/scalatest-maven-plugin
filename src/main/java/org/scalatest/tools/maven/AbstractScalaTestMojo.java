package org.scalatest.tools.maven;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.scalatest.tools.maven.MojoUtils.*;

/**
 * Provides the base for all mojos.
 *
 * @author Jon-Anders Teigen
 * @author Sean Griffin
 * @author Mike Pilquist
 * @author Bill Venners
 *
 * @requiresDependencyResolution test
 */
abstract class AbstractScalaTestMojo extends AbstractMojo {
    /**
     * Injected by Maven so that forked process can be
     * launched from the working directory of current maven project in a multi-module build.  Should not be user facing.
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    MavenProject project;

    /**
     * Injected by Maven so that it can be included in the run path.  Should not be user facing.
     * @parameter property="project.build.testOutputDirectory"
     * @required
     * @readOnly
     */
    File testOutputDirectory;

    /**
     * Injected by Maven so that it can be included in the run path.  Should not be user facing.
     * @parameter property="project.build.outputDirectory"
     * @required
     * @readOnly
     */
    File outputDirectory;

    /**
     * Comma separated list of additional elements to be added
     * to the ScalaTest runpath. <code>${project.build.outputDirectory}</code> and
     * <code>${project.build.testOutputDirectory}</code> are included by default
     * @parameter property="runpath"
     */
    String runpath;

    /**
     * Comma separated list of suites to be executed.
     * @parameter property="suites"
     */
    String suites;

    /**
     * Comma separated list of tests to be executed
     * @parameter property="tests"
     */
    String tests;

    /**
     * Regex of suffixes to filter discovered suites
     * @parameter property="suffixes"
     */
    String suffixes;

    /**
     * Comma separated list of tags to include
     * @parameter property="tagsToInclude"
     */
    String tagsToInclude;

    /**
     * Comma separated list of tags to exclude
     * @parameter property="tagsToExclude"
     */
    String tagsToExclude;

    /**
     * Comma separated list of configuration parameters to pass to ScalaTest.
     * The parameters must be of the format &lt;key&gt;=&lt;value&gt;. E.g <code>foo=bar,monkey=donkey</code>
     * @parameter property="config"
     */
    String config;

    /**
     * Set to true to run suites concurrently
     * @parameter property="parallel"
     */
    boolean parallel;

    /**
     * Set to true to enable suite sorting, this only takes effect if parallel is set to true. 
     * @parameter property="suiteSorting"
     */
    boolean suiteSorting;

    /**
     * Set the thread count for parallel tests execution, this only takes effect if parallel is set to true. 
     * @parameter property="threadCount"
     */
    int threadCount;

    /**
     * Comma separated list of packages containing suites to execute
     * @parameter property="membersOnlySuites"
     */
    String membersOnlySuites;

// TODO: Change this to wildcard and membersOnly
    /**
     * Comma separated list of wildcard suite names to execute
     * @parameter property="wildcardSuites"
     */
    String wildcardSuites;

    /**
     * Comma separated list of testNG xml files to execute
     * @parameter property="testNGXMLFiles"
     */
    String testNGConfigFiles;

    /**
     * Comma separated list of files to store names of failed and
     * canceled tests into.
     * @parameter property="memoryFiles"
     */
    String memoryFiles;

    /**
     * Comma separated list of files to store names of failed and
     * canceled tests into.
     * @parameter property="testsFiles"
     */
    String testsFiles;

    /**
     * Comma separated list of JUnit suites/tests to execute
     * @parameter property="junitClasses"
     */
    String jUnitClasses;

    /**
     * Option to specify the forking mode. Can be "never", "once", or "always".
     * "always" behaves similarly to Surefire's always mode and forks once per
     * explicit suite or JUnit class.
     *
     * @parameter property="forkMode" default-value="once"
     */
    String forkMode;

    /**
     * Option to specify additional JVM options to pass to the forked process.
     *
     * @parameter property="argLine"
     */
    String argLine;

    /**
     * Additional environment variables to pass to the forked process.
     *
     * @parameter
     */
    Map<String, String> environmentVariables;

    /**
     * Additional system properties to pass to the forked process.
     *
     * @parameter
     */
    Map<String, String> systemProperties;

    /**
     * Option to specify whether the forked process should wait at startup for a remote debugger to attach.
     *
     * <p>If set to <code>true</code>, the forked process will suspend at startup and wait for a remote
     * debugger to attach to the configured port.</p>
     *
     * @parameter property="debugForkedProcess" default-value="false"
     */
    boolean debugForkedProcess;

    /**
     * JVM options to pass to the forked process when <code>debugForkedProcess</code> is true.
     *
     * <p>If set to a non-empty value, the standard debug arguments are replaced by the specified arguments.
     * This allows customization of how remote debugging is done, without having to reconfigure the JVM
     * options in <code>argLine</code>.
     *
     * @parameter property="debugArgLine"
     */
    String debugArgLine;

    /**
     * Port to listen on when debugging the forked process.
     *
     * @parameter property="debuggerPort" default-value="5005"
     */
    int debuggerPort = 5005;

    /**
     * Timeout in seconds to allow the forked process to run before killing it and failing the test run.
     *
     * <p>If set to 0, process never times out.</p>
     *
     * @parameter property="timeout" default-value="0"
     */
    int forkedProcessTimeoutInSeconds = 0;

    /**
     * Whether or not to log the command used to launch the forked process.
     *
     * @parameter property="logForkedProcessCommand" default-value="false"
     */
    boolean logForkedProcessCommand;

    /**
     * Span scale factor.
     *
     * @parameter property="spanScaleFactor" default-value="1.0"
     */
    double spanScaleFactor = 1.0;

    /**
     * The current working directory for forked process. Optional. If not specified, basedir will be used.
     *
     * @parameter property="workingDirectory"
     */
    String workingDirectory;

    /**
     * Option to specify an alternative path to JVM (or path to the java executable) to use with
     * the forked process.
     *
     * @parameter property="jvm"
     */
    String jvm;

    // runScalaTest is called by the concrete mojo subclasses  TODO: make it protected and others too
    // Returns true if all tests pass
    boolean runScalaTest(String[] args) throws MojoFailureException {
        getLog().debug(Arrays.toString(args));
        validateForkModeConfiguration();

        if (forkMode.equals("never")) {
            return runWithoutForking(args);
        }
        else if (forkMode.equals("once")) {
            return runForkingOnce(args);
        }
        else if (forkMode.equals("always")) {
            return runForkingAlways(args);
        }
        else {
            getLog().error("Invalid forkMode: \"" + forkMode + "\"; Using once instead.");
            return runForkingOnce(args);
        }
    }

    // Returns true if all tests pass
    protected boolean runWithoutForking(String[] args) {
        try {
            return (Boolean) run().invoke(null, new Object[]{args});
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if(target instanceof RuntimeException){
                throw (RuntimeException)target;
            } else {
                throw new IllegalArgumentException(target);
            }
        }
    }

    // Returns true if all tests pass
    private boolean runForkingOnce(String[] args) throws MojoFailureException {
        return runForkedProcess(args);
    }

    // Returns true if all tests pass
    private boolean runForkingAlways(String[] args) throws MojoFailureException {
        List<String[]> forkedRuns = forkModeAlwaysArgs(args);
        boolean allPassed = true;

        for (String[] forkedRunArgs : forkedRuns) {
            if (!runForkedProcess(forkedRunArgs)) {
                allPassed = false;
            }
        }

        return allPassed;
    }

    protected void validateForkModeConfiguration() throws MojoFailureException {
    }

    private List<String[]> forkModeAlwaysArgs(String[] args) throws MojoFailureException {
        List<String[]> forkedRuns = new ArrayList<String[]>();
        List<String> explicitSuites = splitOnComma(suites);
        List<String> explicitJUnitClasses = splitOnComma(jUnitClasses);

        validateAlwaysSupported(explicitSuites, explicitJUnitClasses);

        String[] sharedArgs = removeAlwaysExecutionUnitArgs(args);

        for (String suite : explicitSuites) {
            List<String> forkedArgs = new ArrayList<String>(Arrays.asList(sharedArgs));
            appendSuiteArgs(forkedArgs, suite);
            forkedRuns.add(forkedArgs.toArray(new String[forkedArgs.size()]));
        }

        for (String jUnitClass : explicitJUnitClasses) {
            List<String> forkedArgs = new ArrayList<String>(Arrays.asList(sharedArgs));
            forkedArgs.add("-j");
            forkedArgs.add(jUnitClass);
            forkedRuns.add(forkedArgs.toArray(new String[forkedArgs.size()]));
        }

        return forkedRuns;
    }

    private void validateAlwaysSupported(List<String> explicitSuites, List<String> explicitJUnitClasses) throws MojoFailureException {
        if (!splitOnComma(tests).isEmpty()
                || !splitOnComma(suffixes).isEmpty()
                || !splitOnComma(membersOnlySuites).isEmpty()
                || !splitOnComma(wildcardSuites).isEmpty()
                || !splitOnComma(testNGConfigFiles).isEmpty()
                || !splitOnComma(testsFiles).isEmpty()) {
            throw new MojoFailureException("forkMode \"always\" supports only explicit suites and JUnit classes in this version.");
        }

        if (explicitSuites.isEmpty() && explicitJUnitClasses.isEmpty()) {
            throw new MojoFailureException("forkMode \"always\" requires at least one explicit suite or JUnit class.");
        }
    }

    private String[] removeAlwaysExecutionUnitArgs(String[] args) {
        List<String> sharedArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("-s".equals(arg)) {
                i = skipSuiteSelectorArgs(args, i);
            }
            else if ("-j".equals(arg)) {
                i++;
            }
            else {
                sharedArgs.add(arg);
            }
        }

        return sharedArgs.toArray(new String[sharedArgs.size()]);
    }

    private int skipSuiteSelectorArgs(String[] args, int suiteArgIndex) {
        int nextIndex = suiteArgIndex + 1; // skip the suite name paired with -s

        if (hasSuiteTestSelector(args, nextIndex)) {
            nextIndex += 2; // also skip the paired -t/-z and test name
        }

        return nextIndex;
    }

    private boolean hasSuiteTestSelector(String[] args, int suiteNameIndex) {
        int testSelectorIndex = suiteNameIndex + 1;
        int testNameIndex = suiteNameIndex + 2;

        return testNameIndex < args.length
                && ("-t".equals(args[testSelectorIndex]) || "-z".equals(args[testSelectorIndex]));
    }

    private void appendSuiteArgs(List<String> args, String suiteConfig) {
        SuiteTestPair pair = new SuiteTestPair(suiteConfig);

        if (pair.suite != null) {
            args.add("-s");
            args.add(pair.suite);

            if (pair.test != null) {
                addTest(args, pair.test);
            }
        }
    }

    // Returns true if all tests pass
    protected boolean runForkedProcess(String[] args) throws MojoFailureException {

        final Commandline cli = new Commandline();
        if ((this.workingDirectory == null || this.workingDirectory.isEmpty())) {
            cli.setWorkingDirectory(project.getBasedir());
        } else {
            cli.setWorkingDirectory(workingDirectory);
        }

        if (this.jvm == null || this.jvm.isEmpty()) {
            cli.setExecutable(getJvm());
        } else {
            cli.setExecutable(this.jvm);
        }

        // Set up environment
        if (environmentVariables != null) {
            for (final Map.Entry<String, String> entry : environmentVariables.entrySet()) {
                cli.addEnvironment(entry.getKey(), entry.getValue());
            }
        }
        cli.addEnvironment("CLASSPATH", buildClassPathEnvironment());

        // Set up system properties
        if (systemProperties != null) {
            for (final Map.Entry<String, String> entry : systemProperties.entrySet()) {
                cli.createArg().setValue(String.format("-D%s=%s", entry.getKey(), entry.getValue()));
            }
        }
        cli.createArg().setValue(String.format("-Dbasedir=%s", project.getBasedir().getAbsolutePath()));

        // Set user specified JVM arguments
        if (argLine != null) {
            cli.createArg().setLine(stripNewLines(argLine));
        }

        // Set debugging JVM arguments if debugging is enabled
        if (debugForkedProcess) {
            cli.createArg().setLine(stripNewLines(forkedProcessDebuggingArguments()));
        }

        // Set ScalaTest arguments
        cli.createArg().setValue("org.scalatest.tools.Runner");
        for (final String arg : args) {
            cli.createArg().setValue(arg);
        }

        // Log command string
        final String commandLogStatement = "Forking ScalaTest via: " + cli;
        if (logForkedProcessCommand) {
            getLog().info(commandLogStatement);
        } else {
            getLog().debug(commandLogStatement);
        }

        try (
            final Writer outputWriter = getOutputWriter()
        ) {
            final StreamConsumer outputConsumer = new WriterStreamConsumer(outputWriter);

            final int result = CommandLineUtils.executeCommandLine(cli, outputConsumer, outputConsumer,
                    forkedProcessTimeoutInSeconds);

            return result == 0;
        }
        catch (final CommandLineTimeOutException e) {
            throw new MojoFailureException(String.format("Timed out after %d seconds waiting for forked process to complete.", forkedProcessTimeoutInSeconds), e);
        }
        catch (final CommandLineException e) {
            throw new MojoFailureException("Exception while executing forked process.", e);
        }
        catch (IOException e) {
            throw new MojoFailureException("Unable to close the output writer ", e);
        }
    }

    protected Writer getOutputWriter() throws MojoFailureException {
        return new PrintWriter(System.out) {
            @Override
            public void close() {
                out = null; // System.out should stay open
            }
        };
    }

    private String buildClassPathEnvironment() {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (String e : testClasspathElements()) {
            if (first) {
                first = false;
            }
            else {
                buf.append(File.pathSeparator);
            }
            buf.append(e);
        }
        return buf.toString();
    }

    private String forkedProcessDebuggingArguments() {
        if (debugArgLine == null) {
            return String.format("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s", debuggerPort);
        } else {
            return debugArgLine;
        }
    }

    protected boolean isScalaTestAvailable() {
        try {
            classLoader().loadClass("org.scalatest.tools.Runner");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }

    // This is just used by runScalaTest to get the method to invoke
    private Method run() {
        try {
            Class<?> runner = classLoader().loadClass("org.scalatest.tools.Runner");
            return runner.getMethod("run", String[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("scalatest is missing from classpath");
        }
    }

    // This is just used by run to get a class loader from which to load ScalaTest
    private ClassLoader classLoader() {
        try {
            List<URL> urls = new ArrayList<URL>();
            for (String element : testClasspathElements()) {
                File file = new File(element);
                if (file.isFile()) {
                    urls.add(file.toURI().toURL());
                }
            }
            URL[] u = urls.toArray(new URL[urls.size()]);
            return new URLClassLoader(u);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    // Have to use the programmatic way of getting the classpath elements
    // instead of the field-level injection since that apparently doesn't work
    // for ReporterMojos in maven-2.2 (it does work in maven-3)
    private List<String> testClasspathElements() {
        try {
            return (List<String>) project.getTestClasspathElements();
        }
        catch (DependencyResolutionRequiredException e) {
            // There's really no known way this exception can happen since
            // the @requiresDependencyResolution at the top of the class
            // defines test-scoped resolution.
            throw new IllegalStateException("Dependency resolution should be test-scoped.", e);
        }
    }

    // This is the configuration parameters shared by all concrete Mojo subclasses
    List<String> sharedConfiguration() {
        return unmodifiableList(
            new ArrayList<String>() {{
            addAll(runpath());
            addAll(config());
            addAll(tagsToInclude());
            addAll(tagsToExclude());
            addAll(parallel());
            addAll(tests());
            addAll(suites());
            addAll(suffixes());
            addAll(membersOnlySuites());
            addAll(wildcardSuites());
            addAll(testNGConfigFiles());
            addAll(memoryFiles());
            addAll(testsFiles());
            addAll(junitClasses());
            addAll(spanScaleFactor());
        }});
    }

    private List<String> config() {
        List<String> c = new ArrayList<String>();
        for(String pair : splitOnComma(config)){
            c.add("-D"+pair);
        }
        return unmodifiableList(c);
    }

    private List<String> runpath() {
        checkRunpathArgument("Output", outputDirectory);
        checkRunpathArgument("Test output", testOutputDirectory);

        String outputPath = outputDirectory.getAbsolutePath();
        if(outputPath.contains(" ")) {
            outputPath = outputPath.replaceAll(" ","\\\\ ");
            getLog().debug(String.format("Escaped output directory path: %s", outputPath));
        }

        String testOutputPath = testOutputDirectory.getAbsolutePath();
        if(testOutputPath.contains(" ")) {
            testOutputPath = testOutputPath.replaceAll(" ","\\\\ ");
            getLog().debug(String.format("Escaped test output directory path: %s", testOutputPath));
        }

        return compoundArg("-R",
                outputPath,
                testOutputPath,
                runpath);
    }

    private void checkRunpathArgument(String directoryName, File directory) {
        if(!directory.exists()) {
            getLog().warn(String.format("%s directory does not exist: %s", directoryName, directory.getAbsolutePath()));
        } else if(!directory.isDirectory()) {
            getLog().warn(String.format("%s is not a directory: %s", directoryName, directory.getAbsolutePath()));
        } else if(!directory.canRead()) {
            getLog().warn(String.format("%s directory is not readable: %s", directoryName, directory.getAbsolutePath()));
        }
    }

    private List<String> tagsToInclude() {
        return compoundArg("-n", tagsToInclude);
    }

    private List<String> tagsToExclude() {
        return compoundArg("-l", tagsToExclude);
    }

    private List<String> parallel() {
        if (parallel) {
            String useSuiteSorting = suiteSorting ? "S" : "";
            String useThreadCount = threadCount == 0 ? "" : ("" + threadCount);
            return unmodifiableList(singletonList("-P" + useSuiteSorting + useThreadCount));
        }
        else
            return Collections.<String>emptyList();
    }

    //
    // Generates a -s argument for each suite in comma-delimited list
    // 'suites', with optionally a -z or -t argument for a test name
    // if one follows the suite name.
    //
    // Test names follow suite names after whitespace, and may be prefixed
    // by an '@' sign to indicate they are an exact test name instead of
    // a substring.  A -t argument is used for tests preceded by an '@'
    // sign, and -z is used for others.
    //
    private List<String> suites() {
        List<String> list = new ArrayList<String>();

        for (String suite: splitOnComma(suites)) {
            SuiteTestPair pair = new SuiteTestPair(suite);

            if (pair.suite != null) {
                list.add("-s");
                list.add(pair.suite);

                if (pair.test != null) {
                    addTest(list, pair.test);
                }
            }
        }
        return unmodifiableList(list);
    }

    //
    // Parses a string containing a Suite name followed
    // optionally by a test name.
    //
    // E.g. "HelloSuite hello there" would produce suite "HelloSuite"
    // and test "hello there".
    //
    static private class SuiteTestPair {
        String suite;
        String test;

        SuiteTestPair(String str) {
            if (str != null) {
                String trimStr = str.trim();

                if (trimStr.length() > 0) {
                    String[] splits = trimStr.split("(?s)\\s", 2);
                    if (splits.length > 1) {
                        suite = splits[0];
                        test = splits[1].trim();
                    }
                    else {
                        suite = trimStr;
                    }
                }
            }
        }
    }

    //
    // Adds a -t or -z arg for specified test name.  Uses -t if name is
    // prefixed by an '@' sign, or -z otherwise.
    //
    private void addTest(List list, String testParm) {
        if (testParm != null) {
            String test = testParm.trim();

            if (test.length() > 0) {
                if (test.charAt(0) == '@') {
                    String atTest = test.substring(1).trim();

                    if (atTest.length() > 0) {
                        list.add("-t");
                        list.add(atTest);
                    }
                }
                else {
                    list.add("-z");
                    list.add(test);
                }
            }
        }
    }

    //
    // Generates a -z or -t argument for each name in comma-delimited
    // 'tests' list, with -t used for those names prefixed by '@'.
    //
    private List<String> tests() {
        List<String> list = new ArrayList<String>();

        for (String test: splitOnComma(tests)) {
            addTest(list, test);
        }
        return unmodifiableList(list);
    }

    private List<String> spanScaleFactor() {
        List<String> list = new ArrayList<String>();
        if (spanScaleFactor != 1.0) {
            list.add("-F");
            list.add(spanScaleFactor + "");
        }
        return unmodifiableList(list);
    }

    private List<String> suffixes() {
       return compoundArg("-q", suffixes);
    }

    private List<String> membersOnlySuites() {
        return suiteArg("-m", membersOnlySuites);
    }

    private List<String> wildcardSuites() {
        return suiteArg("-w", wildcardSuites);
    }

    private List<String> testNGConfigFiles() {
        return suiteArg("-b", testNGConfigFiles);
    }

    private List<String> memoryFiles() {
        return suiteArg("-M", memoryFiles);
    }

    private List<String> testsFiles() {
        List<String> list = new ArrayList<String>();
        for (String param : splitOnComma(testsFiles)) {
            File file = new File(param);
            if (file.exists()) {
                list.add("-A");
                list.add(param);
            }
        }
        return unmodifiableList(list);
    }

    private List<String> junitClasses() {
        return suiteArg("-j", jUnitClasses);
    }
}
