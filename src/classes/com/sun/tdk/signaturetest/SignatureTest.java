/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.classpath.Classpath;
import com.sun.tdk.signaturetest.classpath.ClasspathImpl;
import com.sun.tdk.signaturetest.core.*;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.core.context.TestOptions;
import com.sun.tdk.signaturetest.errors.*;
import com.sun.tdk.signaturetest.loaders.LoadingHints;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.Filter;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;
import com.sun.tdk.signaturetest.updater.Updater;
import com.sun.tdk.signaturetest.util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * <b>SignatureTest</b> is the main class of signature test.
 * <p>
 * The main purpose of signature test is to ensure that programs written in Java
 * using the class libraries of one Java implementation of a specific API
 * version (ie. 1.1, 1.2, etc.) can rely on having the same and only the same
 * Java APIs available in other implementations of the same API version. This is
 * a more stringent requirement than simple binary compatibility as defined in
 * the Java Language Specification, chapter 13. It is in essence a two-way
 * binary compatibility requirement. Therefore, third party implementations of
 * the Java API library must retain binary compatibility with the JavaSoft API.
 * Also, the JavaSoft API implementation must retain binary compatibility with
 * the Java API library under test.</p>
 * <p>
 * <b>SignatureTest</b> implements the standard JavaTest 3.0
 * {@code com.sun.javatest.Test} interface and uses the standard
 * {@code main()} method implementation. <b>SignatureTest</b> allows to
 * check only specified by command line the package or packages.
 * </p>
 * <p>
 * SignatureTest tracks the following aspects of binary compatibility: <ul>
 * <li>Fully qualified name of class or interface <li>Class modifiers abstract
 * and final <li>Superclasses and superinterfaces <li>Public and protected class
 * members </ul>
 * <p><b>SignatureTest</b> tracks all of the super classes and all of the super
 * interfaces of each public class and public interface within required
 * packages.
 * </p>
 * <p>
 * <b>SignatureTest</b> tracks all of the public and protected class members for
 * each public class and interface.</p>
 * <p>
 * For each constructor or method tracked, <b>SignatureTest</b> tracks all
 * modifiers except native and synchronized. It also tracks other attributes for
 * constructors and methods: method name, argument types and order, return type,
 * and the declared throwables.</p>
 * <p>
 * For each field tracked, <b>SignatureTest</b> tracks all modifiers except
 * transient. It also tracks these other attributes for fields: data type, and
 * field name.
 * </p>
 * <p>
 * Usage: {@code java com.sun.tdk.signaturetest.SignatureTest}
 * &lt;options&gt;
 * <br>where &lt;options&gt; includes:
 * <br><dl> <dt><code><b>-TestURL</b></code> &lt;URL&gt; <dd> URL of signature file.
 * <br><dt><code><b>-FileName</b></code> &lt;n&gt; <dd> Path name of signature file
 * name.
 * <br><dt><code><b>-Package</b></code> &lt;package&gt; <dd> Name of the package to
 * be tested. It is implied, that all subpackages the specified package should
 * also be tested. Such option should be included for each package (but
 * subpackages), which is required to be tested.
 * <br><dt><code><b>-PackageWithoutSubpackages</b></code> &lt;package&gt; <dd> Name
 * of the package, which is to be traced itself excluding its subpackages. Such
 * option should be included for each package required to be traced excluding
 * subpackages.
 * <br><dt><code><b>-Exclude</b></code> &lt;package_or_class_name&gt; <dd> Name of
 * the package or class, which is not required to be traced, despite of it is
 * implied by {@code -Package} or by
 * {@code -PackageWithoutSubpackages} options. If the specified parameter
 * names a package, all its subpackages are implied to be also excluded. Such
 * option should be included for each package (but subpackages) or class, which
 * is not required to be traced.
 * <br><dt><code><b>-FormatPlain</b></code> <dd> Do not reorder errors report.
 * <br><dt><code><b>-AllPublic</b></code> <dd> Trace public nested classes, which
 * are member of classes having default scope.
 * <br><dt><code><b>-Classpath</b></code> &lt;path&gt; <dd> Path to packages being
 * tested. If there are several directories and/or zip-files containing the
 * required packages, all of them should be specified here. Use
 * {@code java.io.File.pathSeparator} to separate directory and/or zip-file
 * names in the specified path. Only classes from &lt;path&gt; will be used for
 * tracking adding classes.
 * <br><dt><code><b>-static</b></code> <dd> Run signature test in static mode.
 * <br><dt><code><b>-Version</b></code> &lt;version&gt; <dd> Specify API version. If
 * this parameter is not specified, API version is assumed to be that reported
 * by {@code getProperty("java.version")}.
 * <br><dt><code><b>-CheckValue</b></code>
 * <dd> Check values of primitive constant. This option can be used in static
 * mode only.
 * <br><dt><code><b>-Verbose</b></code>
 * <dd> Enable error diagnostic for inherited class members.
 * </dl>
 *
 * @author Jonathan Gibbons
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Mikhail Ershov
 */
public class SignatureTest extends SigTest {

    // Test specific options
    public static final String CHECKVALUE_OPTION = "-CheckValue";
    public static final String NOCHECKVALUE_OPTION = "-NoCheckValue";
    public static final String MODE_OPTION = "-Mode";
    public static final String ENABLESUPERSET_OPTION = "-EnableSuperSet";
    public static final String FILES_OPTION = "-Files";
    public static final String NOMERGE_OPTION = "-NoMerge";
    public static final String WRITE_OPTION = "-Write";
    public static final String UPDATE_FILE_OPTION = "-Update";
    public static final String SECURE_PACKAGES_OPTION = "-Secure";
    private String logName = null;
    private String outFormat = null;
    private boolean extensibleInterfaces = false;
    private Set<String> orderImportant;
    private static final I18NResourceBundle i18nSt = I18NResourceBundle.getBundleForClass(SignatureTest.class);

    /**
     * Log-file is not the System.err
     */
    private boolean logFile = false;
    /**
     * When signature test checks API, this table collects names of that classes
     * present in both signature file and in the API being tested.
     */
    private Set<String> trackedClassNames;
    /**
     * Enable constant checking whenever possible, starting with sigtest 1.2.1
     */
    private Boolean isValueTracked = null;
    private boolean isOneWayConstantChecking = false;
    private String writeFileName = null;
    private String updateFileName = null;
    /**
     * Check mode selected.
     */
    private String mode = null;
    public static final String BINARY_MODE = "bin";
    private static final String SOURCE_MODE = "src";
    private static final String EXT_MODE = "src-ext";
    private static final String FORMAT_PLAIN = "plain";
    private static final String FORMAT_HUMAN = "human";
    private static final String FORMAT_BACKWARD = "backward";
    private static final String ORDANN_OPTION = "-OrdAnn";
    private boolean isSupersettingEnabled = false;
    private boolean isThrowsRemoved = false;
    private ClassHierarchy signatureClassesHierarchy;
    private final Erasurator erasurator = new Erasurator();
    protected Exclude exclude;
    private int readMode = MultipleFileReader.MERGE_MODE;
    protected final PackageGroup secure = new PackageGroup(true);

    /**
     * Run the test using command-line; return status via numeric exit code.
     *
     * @see #run(String[], PrintWriter, PrintWriter)
     * @param args arguments
     */
    public static void main(String[] args) {
        SignatureTest t = getInstance();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    protected static SignatureTest getInstance() {
        return new SignatureTest();
    }

    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param args arguments.
     * @param log This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     * @see #main(String[])
     */
    public void run(String[] args, PrintWriter log, PrintWriter ref) {

//        long startTime = System.currentTimeMillis();
        AppContext.getContext().clean();
        setLog(log);
        mode = null;
        try {
            ClassLoader cl = SignatureTest.class.getClassLoader();
            exclude = (Exclude) cl.loadClass(System.getProperty("exclude.plugin")).newInstance();
        } catch (Exception e) {
            exclude = new DefaultExcludeList();
        }

        if (parseParameters(args)) {
            check();
            if (logFile) {
                getLog().println(toString());
            }
        }
        if (getClasspath() != null) {
            getClasspath().close();
        }

//        long runTime = System.currentTimeMillis() - startTime;
//        SigTest.log.println("Execution time: " + ((double) runTime) / 1000 + " second(s)");
        // don't close logfile if it was readSignatureFile in test harness
        if (logFile) {
            getLog().close();
            System.out.println(i18nSt.getString("SignatureTest.mesg.see_log", logName));
        }
    }

    /**
     * clean up constant values for non-static constants if this feature is not
     * supported by the format
     */
    private static void correctConstants(final ClassDescription currentClass) {
        for (FieldDescr fd : currentClass.getDeclaredFields()) {
            if (!fd.isStatic()) {
                fd.setConstantValue(null);
            }
        }
    }

    /**
     * Parse options specific for <b>SignatureTest</b>, and pass other options
     * to <b>SigTest</b> parameters parser.
     *
     * @param args Same as {@code args[]} passes to {@code main()}.
     */
    private boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        TestOptions to = AppContext.getContext().getBean(TestOptions.class);

        args = exclude.parseParameters(args);

        final String optionsDecoder = "decodeOptions";

        parser.addOption(FILES_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(APIVERSION_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(OUT_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(CLASSCACHESIZE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(EXTENSIBLE_INTERFACES_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XNOTIGER_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XVERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(CHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(NOCHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(ENABLESUPERSET_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(UPDATE_FILE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(MODE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(VERBOSE_OPTION, OptionInfo.optionVariableParams(0, 1), optionsDecoder);
        parser.addOption(PLUGIN_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(NOMERGE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(WRITE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(ERRORALL_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(ORDANN_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(SECURE_PACKAGES_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);

        parser.addOptions(bo.getOptions(), optionsDecoder);
        parser.addOptions(to.getOptions(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            getLog().println(e.getMessage());
            return failed(e.getMessage());
        }

        if (!processHelpOptions()) {
            return false;
        }

        packages.addPackages(bo.getValues(Option.PACKAGE));
        purePackages.addPackages(bo.getValues(Option.PURE_PACKAGE));
        excludedPackages.addPackages(bo.getValues(Option.EXCLUDE));
        apiIncl.addPackages(bo.getValues(Option.API_INCLUDE));
        apiExcl.addPackages(bo.getValues(Option.API_EXCLUDE));

        if (packages.isEmpty() && purePackages.isEmpty() && apiIncl.isEmpty()) {
            packages.addPackage("");
        }

        initDefaultAnnotations();

        if (bo.getValue(Option.FILE_NAME) != null) {
            readMode = MultipleFileReader.CLASSPATH_MODE;
        }
        // ==================

        if (bo.isSet(Option.STATIC) && !parser.isOptionSpecified(Option.CLASSPATH.getKey())) {
            return error(i18nSt.getString("SignatureTest.error.static.missing_option", Option.CLASSPATH.getKey()));
        }

        if (bo.getValue(Option.FILE_NAME) == null && !parser.isOptionSpecified(FILES_OPTION)) {
            String[] invargs = {Option.FILE_NAME.getKey(), FILES_OPTION};
            return error(i18nSt.getString("SignatureTest.error.options.filename_options", invargs));
        }

        if (bo.getValue(Option.FILE_NAME) != null && parser.isOptionSpecified(FILES_OPTION)) {
            String[] invargs = {Option.FILE_NAME.getKey(), FILES_OPTION};
            return error(i18nSt.getString("Setup.error.options.cant_be_used_together", invargs));
        }

        if (to.isSet(Option.BACKWARD) && to.isSet(Option.FORMATHUMAN)) {
            String[] invargs = {Option.BACKWARD.getKey(), Option.FORMATHUMAN.getKey()};
            return error(i18nSt.getString("Setup.error.options.cant_be_used_together", invargs));
        }

        logFile = false;
        if (logName != null) {

            try {
                setLog(new PrintWriter(new FileWriter(logName), true));
                logFile = true;
            } catch (IOException x) {
                if (bo.isSet(Option.DEBUG)) {
                    SwissKnife.reportThrowable(x);
                }
                return error(i18nSt.getString("SignatureTest.error.out.invfile", OUT_OPTION));
            }
        }

        // create ClasspathImpl for added classes finding
        try {
            //setClasspath(new ClasspathImpl(bo.getValue(Option.CLASSPATH)));
            if (!bo.isSet(Option.STATIC) && isPlatformEnumerationSupported()) {
                try {
                    Class<?> epci = Class.forName("com.sun.tdk.signaturetest.classpath.EnumPlatformClasspathImpl");
                    Classpath cp = (Classpath) epci.getConstructor().newInstance();
                    setClasspath(cp);
                } catch (Exception e) {
                    setClasspath(new ClasspathImpl(bo.getValue(Option.CLASSPATH)));
                }
            } else {
                setClasspath(new ClasspathImpl(bo.getValue(Option.CLASSPATH)));
            }
        } catch (SecurityException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            getLog().println(i18nSt.getString("SignatureTest.error.sec.newclasses"));
        }

        if (bo.isSet(Option.STATIC) && getClasspath().isEmpty()) {
            return error(i18nSt.getString("SignatureTest.error.classpath.unspec"));
        }

        return passed();
    }


    /*
     * Detects if the current platform supports Java9's
     * ModuleReader::list for enumerating available classes
     */
    private static boolean isPlatformEnumerationSupported() {
        try {
            Class.forName("java.lang.module.ModuleReader").getMethod("list");
            //System.out.println("PlatformEnumeration detected");
            return true;
        } catch (Throwable t) {
            //System.out.println("PlatformEnumeration is NOT detected");
            return false;
        }
    }


    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {

        TestOptions to = AppContext.getContext().getBean(TestOptions.class);
        if (to.readOptions(optionName, args)) return;

        if (optionName.equalsIgnoreCase(FILES_OPTION)) {
            sigFileNameList = args[0];
        } else if (optionName.equalsIgnoreCase(EXTENSIBLE_INTERFACES_OPTION)) {
            extensibleInterfaces = true;
        } else if (optionName.equalsIgnoreCase(CHECKVALUE_OPTION)) {
            // default is true as of 1.2.1
            isValueTracked = Boolean.TRUE;
        } else if (optionName.equalsIgnoreCase(NOCHECKVALUE_OPTION)) {
            isValueTracked = Boolean.FALSE;
        } else if (optionName.equalsIgnoreCase(WRITE_OPTION)) {
            writeFileName = args[0];
        } else if (optionName.equalsIgnoreCase(UPDATE_FILE_OPTION)) {
            updateFileName = args[0];
        } else if (optionName.equalsIgnoreCase(MODE_OPTION)) {
            if (!SOURCE_MODE.equalsIgnoreCase(args[0]) && !BINARY_MODE.equalsIgnoreCase(args[0]) && !EXT_MODE.equalsIgnoreCase(args[0])) {
                throw new CommandLineParserException(i18nSt.getString("SignatureTest.error.arg.invalid", MODE_OPTION));
            }
            mode = args[0];
        } else if (optionName.equalsIgnoreCase(OUT_OPTION)) {
            logName = args[0];
        } else if (optionName.equalsIgnoreCase(ENABLESUPERSET_OPTION)) {
            isSupersettingEnabled = true;
        } else if (optionName.equalsIgnoreCase(NOMERGE_OPTION)) {
            readMode = MultipleFileReader.CLASSPATH_MODE;
        } else if (optionName.equalsIgnoreCase(ORDANN_OPTION)) {
            if (orderImportant == null) {
                orderImportant = new TreeSet<>();
            }
            orderImportant.addAll(Arrays.asList(CommandLineParser.parseListOption(args)));
        } else if (optionName.equalsIgnoreCase(SECURE_PACKAGES_OPTION)) {
            secure.addPackages(CommandLineParser.parseListOption(args));
        } else {
            super.decodeCommonOptions(optionName, args);
        }
    }

    /**
     * Prints help text.
     */
    protected void usage() {

        String nl = System.getProperty("line.separator");

        String sb = getComponentName() + " - " + i18nSt.getString("SignatureTest.usage.version", Version.Number) +
                nl + i18nSt.getString("SignatureTest.usage.start") +
                nl + i18nSt.getString("Sigtest.usage.delimiter") +
                nl + i18nSt.getString("SignatureTest.usage.static", Option.STATIC) +
                nl + i18nSt.getString("SignatureTest.usage.mode", MODE_OPTION) +
                nl + i18nSt.getString("SignatureTest.usage.backward", new Object[]{Option.BACKWARD.getKey(), Option.BACKWARD.getAlias()}) +
                nl + i18nSt.getString("SignatureTest.usage.classpath", Option.CLASSPATH.getKey()) +
                nl + i18nSt.getString("SignatureTest.usage.filename", Option.FILE_NAME) +
                nl + i18nSt.getString("SignatureTest.usage.or") +
                nl + i18nSt.getString("SignatureTest.usage.files", new Object[]{FILES_OPTION, File.pathSeparator}) +
                nl + i18nSt.getString("SignatureTest.usage.package", Option.PACKAGE.getKey()) +
                nl + i18nSt.getString("SignatureTest.usage.human", new Object[]{Option.FORMATHUMAN.getKey(), Option.FORMATHUMAN.getAlias()}) +
                nl + i18nSt.getString("SignatureTest.usage.out", OUT_OPTION) +
                nl + i18nSt.getString("Sigtest.usage.delimiter") +
                nl + i18nSt.getString("SignatureTest.usage.testurl", Option.TEST_URL) +
                nl + i18nSt.getString("SignatureTest.usage.packagewithoutsubpackages", Option.PURE_PACKAGE.getKey()) +
                nl + i18nSt.getString("SignatureTest.usage.exclude", Option.EXCLUDE.getKey()) +
                nl + i18nSt.getString("SignatureTest.usage.nomerge", NOMERGE_OPTION) +
                nl + i18nSt.getString("SignatureTest.usage.update", UPDATE_FILE_OPTION) +
                nl + i18nSt.getString("SignatureTest.usage.apiversion", APIVERSION_OPTION) +
                nl + i18nSt.getString("SignatureTest.usage.checkvalue", CHECKVALUE_OPTION) +
                nl + i18nSt.getString("SignatureTest.usage.formatplain", Option.FORMATPLAIN) +
                nl + i18nSt.getString("SignatureTest.usage.extinterfaces", EXTENSIBLE_INTERFACES_OPTION) +
                nl + i18nSt.getString("Sigtest.usage.delimiter") +
                nl + i18nSt.getString("SignatureTest.usage.classcachesize", new Object[]{CLASSCACHESIZE_OPTION, DefaultCacheSize}) +
                nl + i18nSt.getString("SignatureTest.usage.verbose", new Object[]{VERBOSE_OPTION, NOWARN}) +
                nl + i18nSt.getString("SignatureTest.usage.debug", Option.DEBUG.getKey()) +
                nl + i18nSt.getString("SignatureTest.usage.error_all", ERRORALL_OPTION) +
                nl + i18nSt.getString("Sigtest.usage.delimiter") +
                nl + i18nSt.getString("SignatureTest.helpusage.version", Option.VERSION.getKey()) +
                nl + i18nSt.getString("SignatureTest.usage.help", Option.HELP.getKey()) +
                nl + i18nSt.getString("Sigtest.usage.delimiter") +
                nl + i18nSt.getString("SignatureTest.usage.end");
        System.err.println(sb);
    }

    private void initDefaultAnnotations() {
        if (orderImportant == null) {
            orderImportant = new TreeSet<>();
            orderImportant.add("javax.xml.bind.annotation.XmlType");
            orderImportant.add("java.beans.ConstructorProperties");
        }
    }

    protected String getComponentName() {
        return "Test";
    }

    public boolean useErasurator() {
        return !isTigerFeaturesTracked || BINARY_MODE.equals(mode);
    }

    /**
     * Do run signature test provided its arguments are successfully parsed.
     *
     * @see #parseParameters(String[])
     */
    private boolean check() {

        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        TestOptions to = AppContext.getContext().getBean(TestOptions.class);

        String sigFileName = bo.getValue(Option.FILE_NAME);
        String testURL = bo.getValue(Option.TEST_URL);
        if (testURL == null) {
            testURL = "";
        }

        if (to.isSet(Option.FORMATPLAIN)) {
            outFormat = FORMAT_PLAIN;
        } else if (to.isSet(Option.FORMATHUMAN)) {
            outFormat = FORMAT_HUMAN;
        } else if (to.isSet(Option.BACKWARD)) {
            outFormat = FORMAT_BACKWARD;
        }

        if (pluginClass != null) {
            pluginClass.init(this);
        }

        String msg;
        PrintWriter log = getLog();
        //  Open the specified sigfile and read standard headers.

        if (readMode == MultipleFileReader.MERGE_MODE && sigFileNameList != null && sigFileNameList.contains(File.pathSeparator)) {
            try {
                if (writeFileName == null) {
                    File tmpF = File.createTempFile("sigtest", "sig");
                    writeFileName = tmpF.getAbsolutePath();
                    tmpF.deleteOnExit();
                }
                Merge m = Merge.getInstance();
                String[] args = new String[]{"-Files", sigFileNameList, "-Write", writeFileName};
                if (BINARY_MODE.equals(mode)) {
                    args = new String[]{"-Files", sigFileNameList, "-Write", writeFileName, "-Binary"};
                }
                m.run(args, log, null);
                if (!m.isPassed()) {
                    error(m.getReason());
                    return false;
                }
                readMode = MultipleFileReader.CLASSPATH_MODE;
                sigFileName = writeFileName;
                sigFileNameList = null;
                testURL = "";

            } catch (IOException ex) {
                SwissKnife.reportThrowable(ex, log);
                return error(i18nSt.getString("SignatureTest.error.tmpsigfile"));
            }
        } else {
            readMode = MultipleFileReader.CLASSPATH_MODE;
        }

        // apply update file if it was specified
        if (updateFileName != null) {
            try {
                Updater up = new Updater();
                File res = File.createTempFile("sigtest", "sig");
                String resFileName = res.getAbsolutePath();
                res.deleteOnExit();
                up.perform(updateFileName, sigFileName, resFileName, log);
                sigFileName = resFileName;
            } catch (IOException e) {
                SwissKnife.reportThrowable(e);
            }

        }

        MultipleFileReader in = new MultipleFileReader(log, readMode, getFileManager());
        String linesep = System.getProperty("line.separator");
        boolean result;

        if (sigFileNameList != null) {
            result = in.readSignatureFiles(testURL, sigFileNameList);
        } else {
            result = in.readSignatureFile(testURL, sigFileName);
        }

        if (!result) {
            in.close();
            msg = i18nSt.getString("SignatureTest.error.sigfile.invalid", sigFileNameList == null ? sigFileName : sigFileNameList);
            log.println(msg);
            return error(msg);
        }

        if (!prepareCheck(in, log)) {
            return false;
        }

        //  Reading the sigfile: main loop.
        boolean buildMembers = in.isFeatureSupported(FeaturesHolder.BuildMembers);
        MemberCollectionBuilder sigfileMCBuilder = null;
        if (buildMembers) {
            sigfileMCBuilder = new MemberCollectionBuilder(this, "source:sigfile");
        }

        Erasurator localErasurator = new Erasurator();
        msg = null;
        try {

            ClassDescription currentClass;

            // check that set of classes is transitively closed
            ClassSet closedSet = new ClassSet(signatureClassesHierarchy, true);

            in.rewind();
            while ((currentClass = in.nextClass()) != null) {
                closedSet.addClass(currentClass.getQualifiedName());
            }

            Set<String> missingClasses = closedSet.getMissingClasses();
            if (!missingClasses.isEmpty() && !allowMissingSuperclasses()) {

                log.print(i18nSt.getString("SignatureTest.error.required_classes_missing"));
                int count = 0;
                for (String missingClass : missingClasses) {
                    if (count != 0) {
                        log.print(", ");
                    }
                    log.print(missingClass);
                    ++count;
                }
                log.println();

                return error(i18nSt.getString("SignatureTest.error.non_transitively_closed_set"));
            }

            in.rewind();

            boolean supportNSC = in.isFeatureSupported(FeaturesHolder.NonStaticConstants);

            while ((currentClass = in.nextClass()) != null) {
                if (Xverbose) {
                    getLog().println(i18nSt.getString("SignatureTest.mesg.verbose.check", currentClass.getQualifiedName()));
                    getLog().flush();
                }
                if (to.isSet(Option.CHECK_EXCESS_CLASSES_ONLY)) {
                    trackedClassNames.add(currentClass.getQualifiedName());
                } else {
                    if (buildMembers && sigfileMCBuilder != null) {
                        try {
                            if (isAPICheckMode()) {
                                sigfileMCBuilder.setBuildMode(MemberCollectionBuilder.BuildMode.SIGFILE);
                            }
                            sigfileMCBuilder.createMembers(currentClass, addInherited(), false, true);
                        } catch (ClassNotFoundException e) {
                            if (bo.isSet(Option.DEBUG)) {
                                SwissKnife.reportThrowable(e);
                            }
                        }
                    }

                    if (useErasurator()) {
                        currentClass = localErasurator.erasure(currentClass);
                    }

                    Transformer t = PluginAPI.BEFORE_TEST.getTransformer();
                    if (t != null) {
                        try {
                            t.transform(currentClass);
                        } catch (ClassNotFoundException e) {
                            if (bo.isSet(Option.DEBUG)) {
                                SwissKnife.reportThrowable(e);
                            }
                        }
                    }

                    if (currentClass.isModuleOrPackaheInfo() && isTigerFeaturesTracked) {
                        verifyMduleOrPackageInfo(currentClass);
                    } else {
                        verifyClass(currentClass, supportNSC);
                    }
                    if (!isAPICheckMode()) {
                        // save memory
                        currentClass.setMembers(null);
                    }
                }
            }

        } catch (OutOfMemoryError e) {
            msg = i18nSt.getString("SignatureTest.error.sigfile.oome");
        } catch (StackOverflowError e) {
            msg = i18nSt.getString("SignatureTest.error.sigfile.soe");
        } catch (VirtualMachineError e) {
            msg = i18nSt.getString("SignatureTest.error.sigfile.vme", e.getMessage());
        } catch (IOException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            msg = i18nSt.getString("SignatureTest.error.sigfile.prob") + linesep + e;
        } catch (SecurityException e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            msg = i18nSt.getString("SignatureTest.error.sigfile.sec") + linesep + e;
        } catch (AssertionError ass) {
            SwissKnife.reportThrowable(ass);
        } catch (Error e) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(e);
            }
            msg = i18nSt.getString("SignatureTest.error.unknownerror") + e;
        }

        if (msg != null) {
            in.close();
            log.println(msg);
            return error(msg);
        }

        //  Finished - the sigfile closed.
        if (!isSupersettingEnabled) {
            checkAddedClasses();
        }

        if (isTigerFeaturesTracked) {
            checkAddedPackages();
        }

        int auxErrorCount = 0;
        getErrorManager().printErrors();
        if (reportWarningAsError) {
            auxErrorCount = errorMessages.size();
            printErrors();
        }
        log.println("");

        String repmsg = exclude.report();
        if (isVerbose) {
            System.out.println(repmsg);
        }

        int numErrors = getErrorManager().getNumErrors() + auxErrorCount;
        in.close();
        if (numErrors == 0) {
            return passed();
        } else {
            return failed(i18nSt.getString("MTest.msg.failed",
                    Integer.toString(numErrors)));
        }

    }

    // allows missing superclasses, turns off
    // "The following classes are required, but missing in the signature files"
    // error. Needs for specific extensions such as compiler TCK
    protected boolean allowMissingSuperclasses() {
        return false;
    }

    // Can be overriden in an extension, allows to disable
    // normalization methods throw list for required set
    // in api check mode
    // Needs for specific extensions such as compiler TCK
    protected boolean normalizeReq() {
        return true;
    }

    protected boolean isAPICheckMode() {
        return false;
    }

    // for APICheck
    protected void setupLoaders(ClassDescriptionLoader loader,
                                ClassDescriptionLoader second) {
    }

    /**
     * Check if packages being tested do not contain any extra class, which is
     * not described in the {@code signatureFile}. For each extra class
     * detected, error message is appended to the {@code log}.
     */
    private void checkAddedClasses() {
        //check that new classes are not added to the tracked packages.

        if (getClasspath() == null) {
            return;
        }

        try {
            String name;
            while (getClasspath().hasNext()) {
                name = ExoticCharTools.encodeExotic(getClasspath().nextClassName());
                // Check that class isn't tracked and this class is
                // accessible in the current tested mode
                checkAddedClass(name);
            }
        } catch (SecurityException ex) {
            BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(ex);
            }
            getLog().println(i18nSt.getString("SignatureTest.mesg.classpath.sec"));
            getLog().println(ex);
        }
    }

    private void checkAddedClass(String name) {
        if (!trackedClassNames.contains(name) && isPackageMember(name)) {
            BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
            try {
                ClassDescription c = testableHierarchy.load(name);
                if (c.isModuleOrPackaheInfo()) {
                    if (isTigerFeaturesTracked) {
                        checkAnnotations(null, c, null, null, null, testableHierarchy);
                    }
                } else {
                    if (testableHierarchy.isAccessible(c)) {
                        exclude.check(c, c);

                        Filter f = PluginAPI.BEFORE_TEST.getFilter();
                        if (f != null && !f.accept(c)) {
                            return;
                        }

                        checkSupers(c);  // Issue 42 - avoid dummy "added class" message
                        getErrorManager().addError(MessageType.getAddedMessageType(c.getMemberType()), c.getQualifiedName(), c.getMemberType(), null, c);
                    }
                }
            } catch (ClassNotFoundException | LinkageError ex) {
                if (bo.isSet(Option.DEBUG)) {
                    SwissKnife.reportThrowable(ex);
                }
            } catch (ExcludeException e) {
                if (isVerbose) {
                    getLog().println(i18nSt.getString("SignatureTest.mesg.verbose.checkAddedClass", new Object[]{name, e.getMessage()}));
                    getLog().flush();
                }
            }

        }
    }

    private void checkAddedPackages() {
        List<String> wrk = new ArrayList<>();

        for (String trackedClassName : trackedClassNames) {
            String pkg = ClassDescription.getPackageName(trackedClassName);
            if (!wrk.contains(pkg)) {
                wrk.add(pkg);
            }
        }

        Collections.sort(wrk);

        for (String o : wrk) {
            String fqn = ClassDescription.getPackageInfo(o);

            if (!trackedClassNames.contains(fqn)) {
                try {
                    ClassDescription c = testableHierarchy.load(fqn);
                    checkAnnotations(null, c, null, null, null, testableHierarchy);
                } catch (Throwable e) {
                    // ignore because .package-info may not exist!
                }
            }
        }

    }

    private static void transformPair(ClassDescription parentReq, MemberDescription required,
                                      ClassDescription parentFou, MemberDescription found) {
        // number of simple transformations for found - required pair

        // Issue 54
        // public constructor of an abstract class and the same but protected
        // constructor of the same abstract class are mutual compatible
        if (required.isConstructor() && found.isConstructor()
                && parentReq.isAbstract() && parentFou.isAbstract()
                && ((required.isProtected() && found.isPublic())
                || (required.isPublic() && found.isProtected()))) {

            required.setModifiers(required.getModifiers() & ~Modifier.PUBLIC.getValue());
            required.setModifiers(required.getModifiers() | Modifier.PROTECTED.getValue());

            found.setModifiers(found.getModifiers() & ~Modifier.PUBLIC.getValue());
            found.setModifiers(found.getModifiers() | Modifier.PROTECTED.getValue());

        }

       if(found.hasModifier(Modifier.FINAL) && !required.hasModifier(Modifier.FINAL)
                && found.isMethod() && required.isMethod()
                && !found.getDeclaringClassName().equals(required.getDeclaringClassName())) {
            found.removeModifier(Modifier.FINAL);
        }

    }

    /**
     * Check if the {@code required} class described in signature file also
     * presents (and is public or protected) in the API being tested. If this
     * method fails to findByName that class in the API being tested, it appends
     * corresponding message to the errors {@code log}.
     *
     * @return {@code Status.failed("...")} if security exception occurred;
     * or {@code Status.passed("")} otherwise.
     */
    protected boolean verifyClass(ClassDescription required, boolean supportNSC) {
        // checks that package from tested API

        String name = required.getQualifiedName();
        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

        if (!isPackageMember(name)) {
            return passed();
        }

        try {
            exclude.check(required, required);
            ClassDescription found = testableHierarchy.load(name);

            checkSupers(found);

            if (testableHierarchy.isAccessible(found)) {

                if (isAPICheckMode()) {
                    testableMCBuilder.setBuildMode(MemberCollectionBuilder.BuildMode.TESTABLE);
                    testableMCBuilder.setSecondClassHierarchy(signatureClassesHierarchy);
                }

                testableMCBuilder.createMembers(found, addInherited(), true, false);

                Filter f = PluginAPI.BEFORE_TEST.getFilter();
                if (f != null && !f.accept(found)) {
                    return passed();
                }

                Transformer t = PluginAPI.BEFORE_TEST.getTransformer();
                if (t != null) {
                    t.transform(found);
                }

                if (isThrowsRemoved) {
                    required.removeThrows();
                    found.removeThrows();
                } else {
                    normalizer.normThrows(found, true, isAPICheckMode());
                    if (isAPICheckMode() && normalizeReq()) {
                        normalizer.normThrows(required, true, true);
                    }
                }

                if (useErasurator()) {
                    found = erasurator.erasure(found);
                } else if (FORMAT_BACKWARD.equals(outFormat)) {
                    if (!hasClassParameter(required) && hasClassParameter(found)) {
                        found = erasurator.erasure(found);
                        required = erasurator.erasure(required);
                    }
                }

                if (!supportNSC) {
                    correctConstants(found);
                }

                verifyClass(required, found);

            } else {
                getErrorManager().addError(MessageType.MISS_CLASSES, name, MemberType.CLASS, null, required);
            }
        } catch (SuperClassesNotFoundException ex) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(ex);
            }
            String[] names = ex.getMissedClasses();
            for (String name1 : names) {
                getErrorManager().addError(MessageType.MISS_SUPERCLASSES, name1, MemberType.CLASS, ex.getClassName(), required);
            }
        } catch (ClassNotFoundException ex) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(ex);
            }
            getErrorManager().addError(MessageType.MISS_CLASSES, name, MemberType.CLASS, null, required);
        } catch (LinkageError er) {
            if (bo.isSet(Option.DEBUG)) {
                SwissKnife.reportThrowable(er);
            }
            getErrorManager().addError(MessageType.ERROR_LINKERR, name, MemberType.CLASS,
                    i18nSt.getString("SignatureTest.mesg.linkerr.thrown", er),
                    i18nSt.getString("SignatureTest.mesg.linkerr.notlink", name), required);

            trackedClassNames.add(name);
        } catch (ExcludeException e) {
            trackedClassNames.add(name);
            if (isVerbose) {
                getLog().println(i18nSt.getString("SignatureTest.mesg.verbose.verifyClass", new Object[]{name, e.getMessage()}));
                getLog().flush();
            }
        }
        return passed();
    }

    private static void checkSupers(ClassDescription cl) throws SuperClassesNotFoundException {
        ArrayList<String> fNotFound = new ArrayList<>();
        SuperClass sc = cl.getSuperClass();
        ClassHierarchy hi = cl.getClassHierarchy();
        if (sc != null) {
            try {
                hi.load(sc.getQualifiedName());
            } catch (ClassNotFoundException ex) {
                fNotFound.add(ex.getMessage());
            }
        }
        SuperInterface[] sif = cl.getInterfaces();
        if (sif != null) {
            for (SuperInterface superInterface : sif) {
                try {
                    hi.load(superInterface.getQualifiedName());
                } catch (ClassNotFoundException ex) {
                    fNotFound.add(ex.getMessage());
                }
            }
        }
        String[] fProblems = fNotFound.toArray(new String[]{});
        if (fProblems.length > 0) {
            throw new SuperClassesNotFoundException(fProblems, cl.getQualifiedName());
        }
    }

    private static boolean hasClassParameter(ClassDescription cl) {
        String tp = cl.getTypeParameters();
        boolean result = (tp != null) && (!tp.isEmpty());
        // check all the members also
        if (!result) {
            for (Iterator<MemberDescription> e = cl.getMembersIterator(); e.hasNext(); ) {
                MemberDescription mr = e.next();
                String tpM = mr.getTypeParameters();
                if ((tpM != null) && (!tpM.isEmpty())) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private void verifyMduleOrPackageInfo(ClassDescription required) {

        assert (isTigerFeaturesTracked);

        // checks that package from tested API
        String name = required.getQualifiedName();
        if (!isPackageMember(name)) {
            return;
        }

        trackedClassNames.add(name);
        ClassDescription found = null;
        try {
            found = testableHierarchy.load(name);
            //loader.createMembers(found);
        } catch (Exception e) {
            //  just ignore it ...
        }

        checkAnnotations(required, found, null, null, signatureClassesHierarchy, testableHierarchy);
    }

    private void excluded(ClassDescription testedClass, MemberDescription md) throws ExcludeException {
        if (md != null) {
            if (md.isField() || md.isMethod() || md.isConstructor() || md.isInner()) {
                exclude.check(testedClass, md);
            }
        }
    }

    /**
     * Compare descriptions of the {@code required} and {@code found}
     * classes. It is assumed, that description for the {@code required}
     * class is read from signature file, and the {@code found} description
     * belongs to that API being tested. If the descriptions compared are not
     * equal to each other (class names differ, or there are different sets of
     * public members in them), this method appends corresponding error messages
     * to the {@code log}-file. Note, that equality of class or member
     * names may do not imply that they have the same {@code static} and
     * {@code protected} attributes, and {@code throws} clause, if the
     * chosen {@code converter} enables weaker equivalence.
     */
    protected void verifyClass(ClassDescription required, ClassDescription found) {

        // adds class name to the table of the tracked classes.
        trackedClassNames.add(found.getQualifiedName());

        if (getErrorManager() instanceof SortedErrorFormatter) {
            ((SortedErrorFormatter) getErrorManager()).tested(found);
        }

        // track class modifiers
        correctClassModifiers(required, found);
        checkClassDescription(required, found);

        // track members declared in the signature file.
        for (Iterator<MemberDescription> e = required.getMembersIterator(); e.hasNext(); ) {
            MemberDescription requiredMember = e.next();
            try {
                excluded(required, requiredMember);
                trackMember(required, found, requiredMember, found.findMember(requiredMember));
            } catch (ExcludeException e1) {
                if (isVerbose) {
                    getLog().println(i18nSt.getString("SignatureTest.mesg.verbose.verifyMember",
                            new Object[]{required.getQualifiedName(),
                                    requiredMember.toString(),
                                    e1.getMessage()}));
                    getLog().flush();
                }
            }
        }

        // track members which are added in the current implementation.
        if (!isSupersettingEnabled) {
            for (Iterator<MemberDescription> e = found.getMembersIterator(); e.hasNext(); ) {
                MemberDescription foundMember = e.next();
                if (!required.containsMember(foundMember)) {
                    try {
                        excluded(found, foundMember);
                        trackMember(required, found, null, foundMember);
                    } catch (ExcludeException e1) {
                        if (isVerbose) {
                            getLog().println(i18nSt.getString("SignatureTest.mesg.verbose.verifyMember2",
                                    new Object[]{found.getQualifiedName(),
                                            foundMember.toString(),
                                            e1.getMessage()}));
                            getLog().flush();
                        }
                    }
                }
            }
        }
    }

    private void correctClassModifiers(ClassDescription required, ClassDescription found) {

        // see CR 7157095 and option -secure
        if (secure.checkName(found.getQualifiedName())) {
            if (required.isAbstract() != found.isAbstract()) {
                if (!SwissKnife.canBeSubclassed(found) && !SwissKnife.canBeSubclassed(required)) {
                    found.addModifier(Modifier.ABSTRACT);
                    required.addModifier(Modifier.ABSTRACT);
                }
            }
        }

        // CODETOOLS-7901685
        if (required.hasModifier(Modifier.ENUM) && found.hasModifier(Modifier.ENUM)) {
            fixEnum(required);
            fixEnum(found);
        }

    }

    private static void fixEnum(ClassDescription enumClassDescr) {
        // CODETOOLS-7901685
        enumClassDescr.addModifier(Modifier.FINAL);
        enumClassDescr.removeModifier(Modifier.ABSTRACT);
        for (MethodDescr mr : enumClassDescr.getDeclaredMethods()) {
            mr.addModifier(Modifier.FINAL);
            mr.removeModifier(Modifier.ABSTRACT);
        }
    }

    /**
     * Compare names of the {@code required} and {@code found}
     * classes. It is assumed, that description for the {@code required}
     * class is read from signature file, and the {@code found} description
     * belongs to that API being tested. If the descriptions compared are not
     * equal to each other, this method appends corresponding error messages to
     * the {@code log}-file. Note, that equality of descriptions may do not
     * imply that they have the same {@code static} and
     * {@code protected} attributes, if the chosen {@code converter}
     * enables weaker equivalence.
     */
    private void checkClassDescription(ClassDescription required, ClassDescription found) {

        checkAnnotations(required, found, null, null, signatureClassesHierarchy, testableHierarchy);

        if (!required.isCompatible(found)) {
            getErrorManager().addError(MessageType.MISS_CLASSES,
                    required.getQualifiedName(), MemberType.CLASS, required.toString(), required);
            getErrorManager().addError(MessageType.ADD_CLASSES,
                    found.getQualifiedName(), MemberType.CLASS, found.toString(), found);
        }
    }

    private MemberDescription transformMember(ClassDescription parent, MemberDescription member) {
        MemberDescription clonedMember = member;

        if (parent.hasModifier(Modifier.FINAL)
                && member.isMethod()
                && member.getDeclaringClassName().equals(parent.getQualifiedName())) {

            MethodDescr md = (MethodDescr) member;
            // below is a fix for issue 21
            try {
                if (!member.hasModifier(Modifier.FINAL)) {
                    if (!testableHierarchy.isMethodOverriden(md)) {
                        clonedMember = (MemberDescription) member.clone();
                        clonedMember.addModifier(Modifier.FINAL);
                    }
                } else {
                    if (testableHierarchy.isMethodOverriden(md)) {
                        clonedMember = (MemberDescription) member.clone();
                        clonedMember.removeModifier(Modifier.FINAL);
                    }
                }
            } catch (ClassNotFoundException e) {
                SwissKnife.reportThrowable(e);
            }
            // end of fix
        }

        if (BINARY_MODE.equals(mode) && member.isMethod() && member.hasModifier(Modifier.STATIC) && member.hasModifier(Modifier.FINAL)) {
            clonedMember = (MemberDescription) member.clone();
            clonedMember.removeModifier(Modifier.FINAL);
        }

        return clonedMember;
    }
    /**
     * Compare the {@code required} and {@code found} sets of class
     * members having the same signature {@code name}. It is assumed, that
     * the {@code required} description was read from signature file, and
     * the {@code found} description belongs to the API being tested. If
     * these two member descriptions are not equal to each other, this method
     * appends corresponding error messages to the {@code log}-file.
     *
     * @param parentReq ClassDesription for contained class from required set
     * @param parentFou ClassDesription for contained class from found set
     * @param required  the required field
     * @param found     the field (or lack thereof) which is present
     */
    private void trackMember(ClassDescription parentReq, ClassDescription parentFou, MemberDescription required, MemberDescription found) {
        // note: this method is also used to print out an error message
        //       when the implementation being tested has extra fields.
        //       the third parameter is null in this case
        String name = parentReq.getQualifiedName();

//        Fortify
//        if (logger.isLoggable(Level.FINE)) {
//            logger.fine("trackMember \n r:" + required + " \n f:" + found);
//        }
        if (required != null) {
            required = transformMember(parentReq, required);
        }

        if (found != null) {
            found = transformMember(parentFou, found);
        }

        if (required != null && found != null) {

            transformPair(parentReq, required, parentFou, found);

            checkAnnotations(required, found, parentReq, parentFou, signatureClassesHierarchy, testableHierarchy);

            // element matching is basically equality of the signature.
            // the signature can be changed depending on the particular
            // levels of enforcement being used (e.g. include constant values
            // or not)
            if (required.isCompatible(found)) {
//        Fortify
//                if (logger.isLoggable(Level.FINE)) {
//                    logger.fine("compatible! :-)");
//                }
                return;                  // OK
            }

            // one way constant checking if constant values don't match
            if (isOneWayConstantChecking && required.isField()) {

                assert found.isField();

                String rConstValue = ((FieldDescr) required).getConstantValue();
                String fConstValue = ((FieldDescr) found).getConstantValue();
                if (rConstValue == null && fConstValue != null
                        && ((FieldDescr) required).isCompatible(found, true)) {
//        Fortify
//                    if (logger.isLoggable(Level.FINE)) {
//                        logger.fine("compatible! :-)");
//                    }
                    return;     // OK
                }

                // reflection can't read values (couldn't make it accessible ) - it's ok
                // is it bug or according to the specification?
                if (fConstValue == null && rConstValue != null) {
                    if (((FieldDescr) required).isCompatible(found, true)) {
//        Fortify
//                        if (logger.isLoggable(Level.FINE)) {
//                            logger.fine("compatible! :-)");
//                        }
                        return;     // OK
                    }
                }

            }
        }

        if (required != null) {
            getErrorManager().addError(MessageType.getMissingMessageType(required.getMemberType()), name, required.getMemberType(), required.toString(), required);
//        Fortify
//            if (logger.isLoggable(Level.FINE)) {
//                logger.fine("missing :-( " + required);
//            }
        }

        // APICheck should ignore added members, but compiler TCK extensions where
        // isAPICheckMode() is also true should not. So checking getComponentName()
        // instead of isAPICheckMode()
        if (!isSupersettingEnabled && !getComponentName().equals("ApiCheck") && found != null) {
            getErrorManager().addError(MessageType.getAddedMessageType(found.getMemberType()), name, found.getMemberType(), found.toString(), found);
//        Fortify
//            if (logger.isLoggable(Level.FINE)) {
//                logger.fine("added :-( " + found);
//            }
        }
    }

    private void checkAnnotations(MemberDescription base, MemberDescription test,
                                  ClassDescription baseCl, ClassDescription testCl,
                                  ClassHierarchy baseCh, ClassHierarchy testCh) {

        if (!isTigerFeaturesTracked) {
            return;
        }

        AnnotationItem[] baseAnnotList = base == null ? AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY
                : removeUndocumentedAnnotations(base.getAnnoList(), signatureClassesHierarchy);

        AnnotationItem[] testAnnotList = test == null ? AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY
                : removeUndocumentedAnnotations(test.getAnnoList(), testableHierarchy);

        if (baseCh != null) {
            baseAnnotList = unpackContainerAnnotations(baseAnnotList, baseCh);
            normalizeArrayParaemeters(baseAnnotList, orderImportant, baseCh);
        }

        if (testCh != null) {
            testAnnotList = unpackContainerAnnotations(testAnnotList, testCh);
            normalizeArrayParaemeters(testAnnotList, orderImportant, testCh);
        }

        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);
        // RI JSR 308 doesn't support reflection yet
        if (!bo.isSet(Option.STATIC)) {
            baseAnnotList = removeExtendedAnnotations(baseAnnotList);
        }

        if (baseAnnotList.length == 0 && testAnnotList.length == 0) {
            return;
        }

        // NOTE: getAnnoList() always returns sorted annotations array!
        int bl = baseAnnotList.length;
        int tl = testAnnotList.length;
        int bPos = 0;
        int tPos = 0;

        while ((bPos < bl) && (tPos < tl)) {
            int comp = baseAnnotList[bPos].compareTo(testAnnotList[tPos]);
            if (comp < 0) {
                reportError(baseCl, base, baseAnnotList[bPos].toString(), false);
                bPos++;
            } else {
                if (comp > 0) {
                    reportError(testCl, test, testAnnotList[tPos].toString(), true);
                    tPos++;
                } else {
                    tPos++;
                    bPos++;
                }
            }
        }
        while (bPos < bl) {
            reportError(baseCl, base, baseAnnotList[bPos].toString(), false);
            bPos++;
        }
        while (tPos < tl) {
            reportError(testCl, test, testAnnotList[tPos].toString(), true);
            tPos++;
        }
    }

    private static AnnotationItem[] removeExtendedAnnotations(AnnotationItem[] baseAnnotList) {

        if (baseAnnotList == null) {
            return AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY;
        }

        List<AnnotationItem> list = new ArrayList<>(Arrays.asList(baseAnnotList));
        Iterator<AnnotationItem> it = list.iterator();

        while (it.hasNext()) {
            if (it.next() instanceof AnnotationItemEx) {
                it.remove();
            }
        }

        return list.toArray(AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY);
    }

    private void reportError(ClassDescription fromClass, MemberDescription fid, String anno, boolean added) {
        if (fid != null) {
            MessageType mt = added ? MessageType.ADD_ANNO : MessageType.MISS_ANNO;
            String className;
            String defenition;
            if (fromClass == null) {
                className = fid.getQualifiedName();
                defenition = anno;
            } else {
                className = fromClass.getQualifiedName();
                if (fid.isMethod()) {
                    defenition = ((MethodDescr) fid).getSignature() + ":" + anno;
                } else {
                    defenition = fid.getName() + ":" + anno;
                }
            }

            getErrorManager().addError(mt, className, fid.getMemberType(), defenition, fid);
        }
    }

    protected boolean prepareCheck(MultipleFileReader in, PrintWriter log) {

        BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

        if (isValueTracked == null) {
            isValueTracked = Boolean.TRUE;
        }

        if (EXT_MODE.equals(mode)) {
            Modifier.VOLATILE.setTracked(true);
        } else {
            Modifier.VOLATILE.setTracked(false);
        }

        if (mode == null) {
            mode = SOURCE_MODE;
        }
        MemberType.setMode(BINARY_MODE.equals(mode));

        isOneWayConstantChecking = isValueTracked && BINARY_MODE.equals(mode) || !bo.isSet(Option.STATIC);

        if (SOURCE_MODE.equals(mode) || EXT_MODE.equals(mode)) {
            isThrowsRemoved = false;
        }

        if (BINARY_MODE.equals(mode)) {
            isThrowsRemoved = true;
        }
        MemberType.setMode(BINARY_MODE.equals(mode));

        if (isValueTracked && !in.isFeatureSupported(FeaturesHolder.ConstInfo)) {
            String errmsg = i18nSt.getString("SignatureTest.mesg.sigfile.noconst");
            log.println(errmsg);
            return failed(errmsg);
        }
        //  If sigfile doesn't contain constant values, constant checking
        //  is impossible
        if (!in.isFeatureSupported(FeaturesHolder.ConstInfo)) {
            isValueTracked = Boolean.FALSE;
        }

        setConstantValuesTracked(isValueTracked);
        FieldDescr.setConstantValuesTracked(isConstantValuesTracked());
        log.println(i18nSt.getString("SignatureTest.mesg.sigtest.report"));
        log.println(i18nSt.getString("SignatureTest.mesg.sigtest.basevers", in.getApiVersion()));
        log.println(i18nSt.getString("SignatureTest.mesg.sigtest.testvers", apiVersion));

        if (!isThrowsRemoved) {
            log.println(i18nSt.getString("SignatureTest.mesg.sigtest.checkmode.norm", mode));
        } else {
            log.println(i18nSt.getString("SignatureTest.mesg.sigtest.checkmode.removed", mode));
        }

        if (isValueTracked) {
            log.println(i18nSt.getString("SignatureTest.mesg.sigtest.constcheck", i18nSt.getString("SignatureTest.mesg.sigtest.constcheck.on")));
        } else {
            log.println(i18nSt.getString("SignatureTest.mesg.sigtest.constcheck", i18nSt.getString("SignatureTest.mesg.sigtest.constcheck.off")));
        }

        if (!isTigerFeaturesTracked) {
            log.println(i18nSt.getString("SignatureTest.mesg.sigtest.tigercheck"));
        }

        log.println();

        getClasspath().printErrors(log);

        trackedClassNames = new HashSet<>();

        ClassDescriptionLoader loader = getClassDescrLoader();
        setupLoaders(loader, in);
        loader = getClassDescrLoader();
        AppContext.getContext().setClassLoader(loader);

        if (!isValueTracked && loader instanceof LoadingHints) {
            ((LoadingHints) loader).addLoadingHint(LoadingHints.DONT_READ_VALUES);
        }

        testableHierarchy = new ClassHierarchyImpl(loader);
        testableMCBuilder = new MemberCollectionBuilder(this, "source:testable");
        signatureClassesHierarchy = new ClassHierarchyImpl(in);

        // creates ErrorFormatter.
        if (FORMAT_PLAIN.equals(outFormat)) {
            setErrorManager(new ErrorFormatter(log));
        } else if (FORMAT_HUMAN.equals(outFormat)) {
            setErrorManager(new HumanErrorFormatter(log, isVerbose,
                    reportWarningAsError ? Level.WARNING : Level.SEVERE));
        } else if (FORMAT_BACKWARD.equals(outFormat)) {
            setErrorManager(new BCProcessor(log, isVerbose, BINARY_MODE.equals(mode),
                    testableHierarchy, signatureClassesHierarchy,
                    reportWarningAsError ? Level.WARNING : Level.SEVERE, extensibleInterfaces));
        } else {
            setErrorManager(new SortedErrorFormatter(log, isVerbose));
        }

        return true;
    }

    static class SuperClassesNotFoundException extends ClassNotFoundException {

        private final String[] scNames;
        private final String clName;

        private SuperClassesNotFoundException(String[] scNames, String clName) {
            if (scNames == null || scNames.length == 0) {
                throw new IllegalArgumentException("Superclass list can not be empty");
            }
            this.clName = clName;
            this.scNames = scNames;
        }

        public String getMessage() {
            if (scNames.length == 1) {
                return ("Superclass " + scNames[0] + " of class " + clName + " not found");
            } else {
                StringBuffer sb = new StringBuffer("[");
                for (int i = 0; i < scNames.length; i++) {
                    sb.append(scNames[i]);
                    if (i != scNames.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                return ("Superclasses " + sb + " of class " + clName + " not found");
            }
        }

        private String getClassName() {
            return clName;
        }

        private String[] getMissedClasses() {
            return scNames;
        }
    }

    /**
     * This class is used to store excluded signatures.
     */
    static class DefaultExcludeList implements Exclude {

        public DefaultExcludeList() {
        }


        /* (non-Javadoc)
         * @see com.sun.tdk.signaturetest.core.Exclude#check(java.lang.String)
         */
        public void check(ClassDescription testedClassName, MemberDescription signature) throws ExcludeException {
        }

        /* (non-Javadoc)
         * @see com.sun.tdk.signaturetest.core.Exclude#parseParameters(java.util.Vector)
         */
        public String[] parseParameters(String[] args) {
            return args;
        }


        /* (non-Javadoc)
         * @see com.sun.tdk.signaturetest.core.Exclude#report()
         */
        public String report() {
            return null;
        }
    }
}
