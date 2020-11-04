package org.scalatest.tools.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import static java.util.Collections.unmodifiableList;

/**
 * Provides internal utilities for the Mojo's operations.
 * 
 * @author Jon-Anders Teigen
 */
final class MojoUtils {
    private MojoUtils() {
    }

    static interface F {
        public String f(String in);
    }

    static F passThrough = new F() {
        public String f(String in) {
            return in;
        }
    };

    static F fileRelativeTo(final File relative) {
        return new F() {
            public String f(String in) {
                File file = new File(relative, in);
                File parentDir = file.getParentFile();
                createIfNotExists(parentDir); // sideeffect!
                return file.getAbsolutePath();
            }
        };
    }

    static F dirRelativeTo(final File relative){
        return new F(){
            public String f(String in){
                File dir = new File(relative, in);
                createIfNotExists(dir); // sideeffect!
                return dir.getAbsolutePath();
            }
        };
    }

    // sideeffect!
    private static synchronized void createIfNotExists(File dir) {
        if(!dir.exists() && !dir.mkdirs()){
            throw new IllegalStateException("Cannot create directory " + dir);
        }
    }

    static List<String> compoundArg(String name, String...strings){
        List<String> list = new ArrayList<String>();
        List<String> params = new ArrayList<String>();
        for(String commaSeparated : strings){
            params.addAll(splitOnComma(commaSeparated));
        }
        if (params.size() > 0) {
            list.add(name);
            String prefix = "";
            String a = "";
            for (String param : params) {
                a += prefix;
                a += param;
                prefix = " ";
            }
            list.add(a);
        }
        return unmodifiableList(list);
    }

    static List<String> suiteArg(String name, String commaSeparated) {
        List<String> list = new ArrayList<String>();
        for (String param : splitOnComma(commaSeparated)) {
            list.add(name);
            list.add(param);
        }
        return unmodifiableList(list);
    }

    static List<String> reporterArg(String name, String commaSeparated, F map) {
        List<String> r = new ArrayList<String>();
        for (String arg : splitOnComma(commaSeparated)) {
            String[] split = arg.split("\\s");
            if (split.length == 1) {
                r.add(name);
                r.add(map.f(split[0]));
            } else {
                r.add(name + split[0]);
                r.add(map.f(split[1]));
            }
        }
        return unmodifiableList(r);
    }

    //
    // Splits a comma-delimited string.  Supports backslash escapes for
    // commas where string should not be split.  E.g. "a, b, c" returns
    // list ("a", "b", "c"), but "a\, b, c" returns ("a, b", "c").
    //
    static List<String> splitOnComma(String cs) {
        List<String> args = new ArrayList<String>();
        if (cs == null) {
            return unmodifiableList(args);
        } else {
            String[] split = cs.split("(?<!\\\\),");
            for (String arg : split) {
                args.add(arg.trim().replaceAll("\\\\,", ","));
            }
            return unmodifiableList(args);
        }
    }

    static String[] concat(List<String>...lists){
        List<String> c = new ArrayList<String>();
        for(List<String> l : lists){
            c.addAll(l);
        }
        return c.toArray(new String[c.size()]);
    }

    private static String getJavaHome() {
        final String result;
        if (!isEmpty(System.getProperty("java.home"))) {
            result = System.getProperty("java.home");
        }
        else if (!isEmpty(System.getenv("JAVA_HOME"))) {
            result = System.getenv("JAVA_HOME");
        } else {
            result = null;
        }
        return result;
    }

    static String getJvm() {
        final String jh = getJavaHome();
        final String result;
        if (jh == null) {
            result = "java";
        } else {
            result = jh + File.separator + "bin" + File.separator + "java";
        }
        return result;
    }

    static String stripNewLines(String argLine) {
        return argLine.replaceAll("[\r\n]{1,2}", " ");
    }
}
