package org.scalatest.tools.maven;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

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
    private static void createIfNotExists(File dir) {
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
        return list;
    }

    static List<String> suiteArg(String name, String commaSeparated) {
        List<String> list = new ArrayList<String>();
        for (String param : splitOnComma(commaSeparated)) {
            list.add(name);
            list.add(param);
        }
        return list;
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
        return r;
    }

    //
    // Splits a comma-delimited string.  Supports backslash escapes for
    // commas where string should not be split.  E.g. "a, b, c" returns
    // list ("a", "b", "c"), but "a\, b, c" returns ("a, b", "c").
    //
    static List<String> splitOnComma(String cs) {
        List<String> args = new ArrayList<String>();
        if (cs == null) {
            return args;
        } else {
            String[] split = cs.split("(?<!\\\\),");
            for (String arg : split) {
                args.add(arg.trim().replaceAll("\\\\,", ","));
            }
            return args;
        }
    }

    static String[] concat(List<String>...lists){
        List<String> c = new ArrayList<String>();
        for(List<String> l : lists){
            c.addAll(l);
        }
        return c.toArray(new String[c.size()]);
    }
}
