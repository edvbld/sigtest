/*
 * $Id: F40Format.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.sigfile.f40;

import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.Format;
import com.sun.tdk.signaturetest.sigfile.Reader;
import com.sun.tdk.signaturetest.sigfile.Writer;

/**
 * @author Roman Makarchuk
 */
public class F40Format extends Format {

    public F40Format() {
        addSupportedFeature(FeaturesHolder.ConstInfo);
        addSupportedFeature(FeaturesHolder.TigerInfo);
        addSupportedFeature(FeaturesHolder.MergeModeSupported);
        addSupportedFeature(FeaturesHolder.BuildMembers);
        addSupportedFeature(FeaturesHolder.ListOfHiders);
        addSupportedFeature(FeaturesHolder.CopyRight);
    }

    public String getVersion() {
        return "#Signature file v4.0";
    }

    public Reader getReader() {
        return new F40Reader(this);
    }

    public Writer getWriter() {
        return new F40Writer();
    }
    public static final String HIDDEN_FIELDS = "hfds";
    public static final String HIDDEN_CLASSES = "hcls";
}