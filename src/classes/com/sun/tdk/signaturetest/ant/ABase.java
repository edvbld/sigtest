/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.ant;

import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.core.context.Option;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for ant wrappers such as ASetup and ATest
 *
 * @author Mikhail Ershov
 */
public class ABase extends ASuperBase {

    Path classpath;
    final List<APackage> pac = new ArrayList<>();
    private final List<AExclude> exclude = new ArrayList<>();
    String fileName;
    private String apiVersion;

    void createBaseParameters(List<String> params) {
        params.add(Option.FILE_NAME.getKey());
        params.add(fileName);
        params.add(Option.CLASSPATH.getKey());
        String[] cp = classpath.list();
        StringBuffer cpb = new StringBuffer();
        for (int i = 0; i < cp.length; i++) {
            cpb.append(cp[i]);
            if (i != cp.length - 1) {
                cpb.append(File.pathSeparatorChar);
            }
        }
        params.add(cpb.toString());
        if (apiVersion != null) {
            params.add(SigTest.APIVERSION_OPTION);
            params.add(apiVersion);
        }
        for (APackage aPackage : pac) {
            params.add(Option.PACKAGE.getKey());
            params.add(aPackage.value);
        }
        for (AExclude aExclude : exclude) {
            params.add(Option.EXCLUDE.getKey());
            params.add(aExclude.value);
        }
    }

    // classpath
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }

    public Path createClasspath() {
        return createClasspath(getProject()).createPath();
    }

    private Path createClasspath(Project p) {
        if (classpath == null) {
            classpath = new Path(p);
        }
        return classpath;
    }

    // exclude
    public void setExclude(String s) {
        AExclude ae = new AExclude();
        exclude.add(ae);
        ae.setPackage(s);
    }

    public AExclude createExclude() {
        AExclude ae = new AExclude();
        exclude.add(ae);
        return ae;
    }

    // package
    public void setPackage(String s) {
        APackage ae = new APackage();
        pac.add(ae);
        ae.setName(s);
    }

    public APackage createPackage() {
        APackage ap = new APackage();
        pac.add(ap);
        return ap;
    }

    // filename
    public void setFilename(String s) {
        fileName = s;
    }

    // APIVersion
    public void setApiVersion(String s) {
        apiVersion = s;
    }

    public static class AExclude extends DataType {

        String value;

        public void setPackage(String p) {
            value = p;
        }

        public void setClass(String p) {
            value = p;
        }
    }

    public static class APackage extends DataType {

        String value;

        public void setName(String p) {
            value = p;
        }
    }
}
