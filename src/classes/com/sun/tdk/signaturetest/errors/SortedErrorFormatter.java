/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.errors;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.plugin.MessageTransformer;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;
import java.util.*;

/**
 * <b>SortedErrorFormatter</b> formats error messages created by
 * <b>SignatureTest</b> and by <b>APIChangesTest</b>. This class prints messages
 * sorted error type and by name of class affected by the error.
 *
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @version 05/03/22
 */
public class SortedErrorFormatter extends ErrorFormatter {

    private static final I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(SortedErrorFormatter.class);
    /**
     * Headers for message groups by error type.
     */
    private Map<String, String> testedsuper = new HashMap<>();
    private boolean verbose = false;
    /**
     * Messages buffer.
     */
    protected List<Message> failedMessages;
    /**
     * Tabulator position.
     */
    protected static final int tabSize = 20;

    /**
     * Assign the given <b>PrintWriter</b> to print error messages.
     */
    public SortedErrorFormatter(PrintWriter out, boolean isv) {
        super(out);
        failedMessages = new ArrayList<>();
        verbose = isv;
    }

    public void tested(ClassDescription tested) {

        if (!getTestedsuper().containsKey(tested.getQualifiedName())) {
            if (tested.getSuperClass() != null) {
                getTestedsuper().put(tested.getQualifiedName(), tested.getSuperClass().getQualifiedName());
            }

            MemberDescription[] items = tested.getInterfaces();
            if (items != null) {
                for (MemberDescription item : items) {
                    //System.err.println("-interface "+tested.getName()+" "+items[I].getName());
                    getTestedsuper().put(tested.getQualifiedName(), item.getQualifiedName());
                }
            }
        }
    }

    /**
     * Append new error message to the {@code failedMessages} buffer.
     *
     * @see #failedMessages
     */
    public void addError(MessageType kind, String className, MemberType type, String def, String tail, MemberDescription errorObject) {
        Message c = createError(kind, className, type, def, tail, errorObject);
        failedMessages.add(c);
        if (!kind.isWarning()) {
            numErrors++;
        }
    }

    /**
     * Print all error messages collected by {@code failedMessages}.
     */
    public void printErrors() {

        int exmsgs = msgExclude(getTestedsuper());
        String nl = System.getProperty("line.separator");

        MessageTransformer t = PluginAPI.BEFORE_MESSAGE_SORT.getMessageTransformer();
        if (t != null) {
            failedMessages = t.changeMessageList(failedMessages);
        }

        sortErrors();

        boolean hasHeader = false;
        int length = failedMessages.size();
        MessageType lastType = null;
        numErrors = 0;
        for (Message current : failedMessages) {
            // recalculate num errors because of possible PluginAPI filtering
            if (!current.messageType.isWarning()) {
                numErrors++;
            }
            if (current.messageType != lastType) {
                hasHeader = true;
                out.println(nl + current.messageType.getLocMessage() + nl
                        + space('-', current.messageType.getLocMessage().length()) + nl);
                lastType = current.messageType;
            }
            if (hasHeader) {
                if (current.definition.isEmpty()) {
                    out.println(current.className);
                } else {
                    int currentTab = (current.className.length() + 1) / tabSize;
                    if ((current.className.length() + 1) % tabSize != 0) {
                        currentTab++;
                    }
                    currentTab *= tabSize;
                    out.println(current.className + ":"
                            + space(' ', currentTab - current.className.length() - 1)
                            + current.definition);
                    if (isVerbose() && !current.tail.isEmpty()) {
                        out.println(i18n.getString("SortedErrorFormatter.error.affected", current.tail));
                    }
                }
            } else {
                out.println(current);
            }
        }
        if (!failedMessages.isEmpty()) {
            out.println("");
        }

        if (exmsgs > 0) {
            out.println(i18n.getString("SortedErrorFormatter.error.dupmesg", Integer.toString(exmsgs)));
        }
    }

    protected void sortErrors() {
        Collections.sort(failedMessages, new Comparator<Message>() {
            @Override
            public int compare(Message m1, Message m2) {
                // Full Messages compare. Note that Message.compareTo does not do it!
                // Full compare required to guarantee identical output from one execution to another
                // in different modes

                int comp = m1.messageType.compareTo(m2.messageType);

                if (comp == 0) {
                    comp = m1.className.compareTo(m2.className);
                    if (comp == 0) {
                        comp = m1.definition.compareTo(m2.definition);
                        if (comp == 0) {
                            if (m1.tail != null && m2.tail != null) {
                                comp = m1.tail.compareTo(m2.tail);
                            } else {
                                if (m1.tail == null) {
                                    comp = -1;
                                } else {
                                    comp = 1;
                                }
                            }
                        }
                    }
                    return comp;
                }
                return comp;
            }
        });
    }

    /**
     * Return string consisting of {@code len} copies of the symbol
     * {@code c}.
     */
    protected static String space(char c, int len) {
        char[] buff = new char[len];
        for (int i = 0; i < len; i++) {
            buff[i] = c;
        }
        return new String(buff);
    }

    public int msgExclude(Map<String, String> supernames) {

        int excludedMessages = 0;

        int i, k, n;
        List<List<Message>> vv = new ArrayList<>();

        for (i = 0; i < failedMessages.size(); i++) {
            Message msgi = failedMessages.get(i);

            List<Message> v = null;

            for (n = 0; n < vv.size(); n++) {
                List<Message> x = vv.get(n);
                if (msgCompare(x.get(0), msgi)) {
                    v = vv.get(n);
                    break;
                }
            }

            if (v == null) {
                for (k = i + 1; k < failedMessages.size(); k++) {
                    Message msgk = failedMessages.get(k);

                    if (msgCompare(msgk, msgi)) {
                        if (v == null) {
                            v = new ArrayList<>();
                            vv.add(v);
                            v.add(msgi);
                        }
                        v.add(msgk);
                    }
                }
            }
        }

        List<Message> exclude = new ArrayList<>();

        for (n = 0; n < vv.size(); n++) {
            List<Message> v = vv.get(n);
            //System.out.println("-Duplicate group-");

            for (k = 0; k < v.size(); k++) {
                rep:
                for (boolean flag = true; flag; ) {
                    flag = false;
                    Message msgk = v.get(k);
                    String supk = supernames.get(msgk.className);
                    if (supk != null) {
                        for (i = k + 1; i < v.size(); i++) {
                            Message msgi = v.get(i);
                            if (msgi.className.equals(supk)) {
                                v.set(k, msgi);
                                v.set(i, msgk);
                                flag = true;
                                //System.out.println("swap "+I+" "+k);
                                continue rep;
                            }
                        }
                    }
                }
            }

            for (k = v.size(); --k >= 0; ) {
                Message msgk = v.get(k);
                //System.out.println(MsgShow(msgk));
                String supk = supernames.get(msgk.className);
                if (supk != null) {
                    for (i = k; --i >= 0; ) {
                        Message msgi = v.get(i);
                        if (msgi.className.equals(supk)) {
                            if (!msgi.tail.isEmpty()) {
                                msgi.tail += ",";
                            }
                            msgi.tail += msgk.className;
                            if (!msgk.tail.isEmpty()) {
                                msgi.tail += "," + msgk.tail;
                            }
                            exclude.add(v.get(k));
                            excludedMessages++;
                            //System.out.println(MsgShow(msgk)+"-excluded");
                            break;
                        }
                    }
                }
            }
        }

        for (i = failedMessages.size(); --i >= 0; ) {
            Message msgi = failedMessages.get(i);

            for (k = 0; k < exclude.size(); k++) {
                Message msgk = exclude.get(k);
                if (msgi == msgk) {
                    //System.out.println(MsgShow(msgi)+"-removed");
                    failedMessages.remove(i);
                    break;
                }
            }
        }

        return excludedMessages;
    }

    protected boolean msgCompare(Message m1, Message m2) {
        return m1.messageType == m2.messageType
                && m1.definition.equals(m2.definition);
    }

    protected Map<String, String> getTestedsuper() {
        return testedsuper;
    }

    protected void setTestedsuper(Map<String, String> testedsuper) {
        this.testedsuper = testedsuper;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
