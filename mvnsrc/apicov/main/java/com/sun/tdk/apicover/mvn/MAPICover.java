/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.apicover.mvn;

import com.sun.tdk.apicover.Main;
import com.sun.tdk.signaturetest.Result;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.mvn.MLogAdapter;
import com.sun.tdk.signaturetest.mvn.MSuperBase;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 * @goal apicover
 * @phase test
 */
public class MAPICover extends MSuperBase {

    /**
     * @parameter @required
     */
    protected String api;

    /**
     * @parameter @required
     */
    protected List<String> tests;

    /**
     * @parameter
     */
    protected List<String> apiIncludes;

    /**
     * @parameter
     */
    protected List<String> apiIncludeWs;

    /**
     * @parameter
     */
    protected List<String> apiExcludes;

    /**
     * @parameter
     */
    protected List<String> tsIncludes;

    /**
     * @parameter
     */
    protected List<String> tsIncludeWs;

    /**
     * @parameter
     */
    protected List<String> tsExcludes;

    /**
     * @parameter
     */
    protected List<String> excludeLists;

    /**
     * @parameter
     */
    protected String report;

    /**
     * @parameter
     */
    protected String mode;

    /**
     * @parameter
     */
    protected String detail;

    /**
     * @parameter
     */
    protected String format;

    /**
     * @parameter default-value="false"
     */
    protected boolean excludeInterfaces;

    /**
     * @parameter default-value="false"
     */
    protected boolean excludeAbstractClasses;

    /**
     * @parameter default-value="false"
     */
    protected boolean excludeAbstractMethods;

    /**
     * @parameter default-value="false"
     */
    protected boolean excludeFields;

    /**
     * @parameter default-value="false"
     */
    protected boolean includeConstantFields;

    /**
     * @parameter default-value="false"
     */
    protected boolean debug;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("<APICover>");
        String oldExit = System.setProperty(Result.NO_EXIT, Boolean.TRUE.toString());
        if (oldExit == null) {
            oldExit = Boolean.TRUE.toString();
        }

        Main main = new Main();
        PrintWriter wr = new MLogAdapter(getLog());
        main.run(createParams(), wr, null);
        wr.flush();
        wr.close();

        System.setProperty(Result.NO_EXIT, oldExit);
        getLog().debug("</APICover>");
    }

    private String[] createParams() {
        ArrayList params = new ArrayList();

        if (!"".equals(api)) {
            params.add(Option.API.getKey());
            params.add(api);
        }

        addList(params, Option.TS.getKey(), tests);
        addList(params, Option.API_INCLUDE.getKey(), apiIncludes);
        addList(params, Option.API_INCLUDEW.getKey(), apiIncludeWs);
        addList(params, Option.API_EXCLUDE.getKey(), apiExcludes);
        addList(params, Option.TS_ICNLUDE.getKey(), tsIncludes);
        addList(params, Option.TS_ICNLUDEW.getKey(), tsIncludeWs);
        addList(params, Option.TS_EXCLUDE.getKey(), tsExcludes);
        addList(params, Option.EXCLUDE_LIST.getKey(), excludeLists);

        if (report != null && !"".equals(report)) {
            params.add(Option.REPORT.getKey());
            params.add(report);
        }

        if (mode != null && !"".equals(mode)) {
            params.add(Option.MODE.getKey());
            params.add(mode);
        }

        if (detail != null && !"".equals(detail)) {
            params.add(Option.DETAIL.getKey());
            params.add(detail);
        }

        if (format != null && !"".equals(format)) {
            params.add(Option.FORMAT.getKey());
            params.add(format);
        }

        addFlag(params, Option.EXCLUDE_INTERFACES.getKey(), excludeInterfaces);
        addFlag(params, Option.EXCLUDE_ABSTRACT_CLASSES.getKey(), excludeAbstractClasses);
        addFlag(params, Option.EXCLUDE_ABSTRACT_METHODS.getKey(), excludeAbstractMethods);
        addFlag(params, Option.EXCLUDE_FIELDS.getKey(), excludeFields);
        addFlag(params, Option.INCLUDE_CONSTANT_FIELDS.getKey(), includeConstantFields);
        addFlag(params, Option.DEBUG.getKey(), debug);

        return (String[]) params.toArray(new String[]{});
    }

    private void addList(ArrayList params, String option, List<String> list) {
        if (list != null && list.size() > 0) {
            params.add(option);
            Iterator<String> it = list.iterator();
            StringBuffer sb = new StringBuffer();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(File.pathSeparator);
                }
            }
            params.add(sb.toString());
        }
    }

    private void addFlag(ArrayList params, String option, boolean opt) {
        if (opt) {
            params.add(option);
        }
    }

}
