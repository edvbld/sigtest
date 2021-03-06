/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.apicover;

import com.sun.tdk.signaturetest.Version;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.util.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class CMerge {

    private final static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(CMerge.class);
    String oFile;
    String[] iFiles;
    boolean strict = false;
    CovDocument[] docs;
    public static final String STRICT_OPTION = "-Strict";
    Document result;
    Element resultReport;
    final XPathFactory xPathfactory = XPathFactory.newInstance();

    public static void main(String[] args) {
        new CMerge().perform(args);
    }

    private void perform(String[] args) {

        if (parseParameters(args)) {
            perform();
        } else if (args.length > 0 && Option.VERSION.accept(args[0])) {
            System.err.println(Version.getVersionInfo());
        } else {
            usage();
        }
    }

    private void perform() {
        docs = new CovDocument[iFiles.length];
        try {

            for (int i = 0; i < iFiles.length; i++) {
                docs[i] = new CovDocument(iFiles[i]);
            }

            if (checkAndMerge(docs, strict)) {
                saveToXml(result, oFile);
            }

        } catch (SAXException | TransformerException | XPathExpressionException | ParserConfigurationException | IOException ex) {
            SwissKnife.reportThrowable(ex);
        }
    }

    protected void usage() {
        String nl = System.getProperty("line.separator");
        String sb = nl + getComponentName() + " - " + i18n.getString("Merge.usage.version", Version.Number) +
                nl + i18n.getString("Merge.usage.start") +
                nl + i18n.getString("Merge.usage.delimiter") +
                nl + i18n.getString("Merge.usage.files", Option.FILES.getKey()) +
                nl + i18n.getString("Merge.usage.write", Option.WRITE.getKey()) +
                nl + i18n.getString("Merge.usage.strict", STRICT_OPTION) +
                nl + i18n.getString("Merge.usage.delimiter") +
                nl + i18n.getString("Merge.helpusage.version", Option.VERSION.getKey()) +
                nl + i18n.getString("Merge.usage.help", Option.HELP.getKey()) +
                nl + i18n.getString("Merge.usage.delimiter") +
                nl + i18n.getString("Merge.usage.end");
        System.err.println(sb);
    }

    protected String getComponentName() {
        return "CoverageMerge";
    }

    private boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.
        if (args == null || args.length == 0 || Option.HELP.accept(args[0])) {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(Option.FILES, optionsDecoder);
        parser.addOption(Option.WRITE, optionsDecoder);
        parser.addOption(STRICT_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(Option.HELP, optionsDecoder);
        parser.addOption(Option.VERSION, optionsDecoder);

        try {
            parser.processArgs(args);
            if (oFile != null) {
                checkValidWriteFile();
            }
        } catch (CommandLineParserException e) {
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    private void checkValidWriteFile() throws CommandLineParserException {
        File canonicalFile = null;
        try {
            canonicalFile = (new File(oFile)).getCanonicalFile();
        } catch (IOException e) {
            throw new CommandLineParserException(i18n.getString("Merge.could.not.resolve.file", oFile));
        }

        for (String iFile : iFiles) {
            try {
                File sigFile = (new File(iFile)).getCanonicalFile();
                if (canonicalFile.equals(sigFile)) {
                    throw new CommandLineParserException(i18n.getString("Merge.notunique.writefile"));
                }
            } catch (IOException ex) {
                throw new CommandLineParserException(i18n.getString("Merge.could.not.resolve.file", iFile));
            }
        }

        try {
            FileOutputStream f = new FileOutputStream(oFile);
            f.close();
        } catch (IOException e) {
            throw new CommandLineParserException(i18n.getString("Merge.could.not.create.write.file"));
        }
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        if (optionName.equalsIgnoreCase(Option.FILES.getKey())) {
            StringTokenizer st = new StringTokenizer(args[0], File.pathSeparator);
            List<String> list = new ArrayList<>();
            while (st.hasMoreElements()) {
                list.add(st.nextToken());
            }
            iFiles = list.toArray(new String[0]);
        } else if (optionName.equalsIgnoreCase(Option.WRITE.getKey())) {
            oFile = args[0];
        } else if (optionName.equalsIgnoreCase(STRICT_OPTION)) {
            strict = true;
        }
    }

    private boolean checkAndMerge(CovDocument[] docs, boolean strict) throws XPathExpressionException, ParserConfigurationException {
        // check configuration
        Option[] keys = {Option.EXCLUDE_LIST,
                Option.EXCLUDE_INTERFACES,
                Option.EXCLUDE_ABSTRACT_CLASSES,
                Option.EXCLUDE_ABSTRACT_METHODS,
                Option.EXCLUDE_FIELDS,
                Option.INCLUDE_CONSTANT_FIELDS,
                Option.MODE};

        HashMap<String, String> confs = new HashMap<>();
        for (Option opt : keys) {
            confs.put(opt.getKey(), "NA");
        }

        for (CovDocument d : docs) {
            for (Option opt : keys) {
                String key = opt.getKey();
                String m = d.getConfigValue(key, "no");
                String o = confs.get(key);
                if (!o.equals(m)) {
                    if (o.equals("NA")) {
                        confs.put(key, m);
                    } else {
                        System.err.println("Files contain incompatible heads for " + key);
                        return false;
                    }
                }
            }
        }

        // create a result document
        result = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        resultReport = result.createElement(XC.REPORT);
        result.appendChild(resultReport);
        Element eHead = result.createElement(XC.HEAD);
        resultReport.appendChild(eHead);
        for (Map.Entry<String, String> en : confs.entrySet()) {
            if (en.getValue().equals("NA")) {
                continue;
            }
            Element e = result.createElement(XC.HEAD_PROPERTY);
            e.setAttribute(XC.HEAD_PROPERTY_NAME, en.getKey());
            e.setAttribute(XC.HEAD_PROPERTY_VALUE, en.getValue());
            eHead.appendChild(e);
        }

        // check for different versions of the same class
        // and prepare data for merging
        Map<String, Element[]> allClasses = new HashMap<>();
        for (int i = 0; i < docs.length; i++) {
            CovDocument d = docs[i];
            for (Element cl : d.getClasses()) {
                String qName = CovDocument.getQname(cl);
                if (!allClasses.containsKey(qName)) {
                    allClasses.put(qName, new Element[docs.length]);
                }
                allClasses.get(qName)[i] = cl;
            }
        }

        // check and merge
        for (Map.Entry<String, Element[]> en : allClasses.entrySet()) {
            String qName = en.getKey();
            Element[] similarClasses = en.getValue();
            Set<String> atsG = null;
            NamedNodeMap classAttributes = null;

            for (Element c : similarClasses) {
                if (c != null) {
                    classAttributes = c.getAttributes();
                    HashSet<String> atsL = new HashSet<>();
                    for (int i = 0; i < classAttributes.getLength(); i++) {
                        String aName = classAttributes.item(i).getNodeName();
                        String aVal = classAttributes.item(i).getNodeValue();

                        // ignore some known attributes
                        if (!aName.equals(XC.CLASS_NAME)
                                && !aName.equals(XC.CLASS_MEMBERS)
                                && !aName.equals(XC.CLASS_TESTED)
                                && !aName.equals(XC.CLASS_TYPEARGS)) {
                            atsL.add(aName);
                        }
                    }
                    if (atsG == null) {
                        atsG = atsL;
                    } else { // compare modifiers
                        String m1 = atsG.toString();
                        String m2 = atsL.toString();
                        if (!m1.equals(m2)) {
                            System.err.println("Incompatible attributes for class " + qName + ":");
                            System.err.println(m1 + " and " + m2);
                            return false;
                        }
                    }
                }
            }
            // check members
            HashMap<String, Element> members = new HashMap<>();
            boolean firstTime = true;
            for (Element classElement : similarClasses) {
                if (classElement != null) {
                    NodeList memberList = classElement.getChildNodes();
                    for (int i = 0; i < memberList.getLength(); i++) {
                        Node n = memberList.item(i);
                        if (n instanceof Element) {
                            Element member = (Element) n;
                            String memberID = member.getNodeName() + " "
                                    + member.getAttribute(XC.MEMBER_NAME) + " "
                                    + member.getAttribute(XC.MEMBER_SIG);

                            // collect member's attributes
                            String myAttrsToCheck = getMemberModifsAsString(member);
                            boolean isCov = Integer.parseInt(member.getAttribute(XC.MEMBER_TESTED)) > 0;

                            if (firstTime) {
                                members.put(memberID, member);
                                if (i == memberList.getLength() - 1) {
                                    // last member
                                    firstTime = false;
                                }
                            } else {
                                if (strict && !members.containsKey(memberID)) {
                                    System.err.println("Different members in class class " + qName);
                                    System.err.println("Member " + memberID);
                                    return false;
                                }
                                if (!members.containsKey(memberID)) {
                                    // add member
                                    members.put(memberID, member);
                                } else {
                                    String m1 = getMemberModifsAsString(members.get(memberID));
                                    if (!m1.equals(myAttrsToCheck)) {
                                        System.err.println("Incompatible member attributes in class " + qName);
                                        System.err.println("member " + memberID);
                                        System.err.println(m1 + " and " + myAttrsToCheck);
                                        return false;
                                    }
                                    // update coverage
                                    if (isCov) {
                                        Element stored = members.get(memberID);
                                        stored.setAttribute(XC.MEMBER_TESTED, "1");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            /// write a class data
            addClass(qName, classAttributes, members.values());
        }
        return true;
    }

    private static String getMemberModifsAsString(Element member) {
        // collect member's attributes
        HashSet<String> modifS = new HashSet<>();
        NamedNodeMap attrs = member.getAttributes();
        for (int j = 0; j < attrs.getLength(); j++) {
            String aName = attrs.item(j).getNodeName();

            // ignore some known attributes
            if (!aName.equals(XC.MEMBER_NAME)
                    && !aName.equals(XC.MEMBER_TYPE)
                    && !aName.equals(XC.MEMBER_SIG)
                    && !aName.equals(XC.MEMBER_TESTED)
                    && !aName.equals(XC.MEMBER_VMSIG)) {
                modifS.add(aName);
            }
        }
        return modifS.toString();
    }

    private static void saveToXml(Document d, String oFile) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(d);
        StreamResult sresult = new StreamResult(new File(oFile));
        transformer.transform(source, sresult);
    }

    private void addClass(String qName, NamedNodeMap classAttributes, Collection<Element> members) {
        try {
            Element pck = createPackagesFor(qName);
            Element cl = result.createElement(XC.CLASS);
            for (int i = 0; i < classAttributes.getLength(); i++) {
                String aName = classAttributes.item(i).getNodeName();
                String aVal = classAttributes.item(i).getNodeValue();
                cl.setAttribute(aName, aVal);
            }
            int memberCount = 0;
            int testedCount = 0;
            for (Element member : members) {
                Element newM = result.createElement(member.getNodeName());
                NamedNodeMap membAttributes = member.getAttributes();
                for (int i = 0; i < membAttributes.getLength(); i++) {
                    String aName = membAttributes.item(i).getNodeName();
                    String aVal = membAttributes.item(i).getNodeValue();
                    newM.setAttribute(aName, aVal);
                    if (aName.equals(XC.MEMBER_TESTED) && aVal.equals("1")) {
                        testedCount++;
                    }
                }
                cl.appendChild(newM);
                memberCount++;
                cl.setAttribute(XC.CLASS_MEMBERS, String.valueOf(memberCount));
                cl.setAttribute(XC.CLASS_TESTED, String.valueOf(testedCount));
            }
            pck.appendChild(cl);
            updateCounters(pck, memberCount, testedCount);
        } catch (XPathExpressionException ex) {
            SwissKnife.reportThrowable(ex);
        }
    }

    private Element createPackagesFor(String qName) throws XPathExpressionException {
        StringTokenizer st = new StringTokenizer(qName, ".");
        int parts = st.countTokens();
        int pn = 1;
        String path = "/" + XC.REPORT + "/" + XC.PACKAGE + "[@" + XC.PACKAGE_NAME + "='']";
        Element packageE = checkForPackage(path, "", resultReport);
        while (st.hasMoreTokens() && pn < parts) {
            String pnm = st.nextToken().trim();
            // /report/package[@name='']/package[@name='java']/package[@name='lang']  ...
            path = path + "/" + XC.PACKAGE + "[@" + XC.PACKAGE_NAME + "='" + pnm + "']";
            packageE = checkForPackage(path, pnm, packageE);
            pn++;
        }
        return packageE;
    }

    private Element checkForPackage(String path, String name, Element parentPkg) throws XPathExpressionException {
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(path);
        Element par = (Element) expr.evaluate(result, XPathConstants.NODE);
        if (par == null) {
            par = result.createElement(XC.PACKAGE);
            par.setAttribute(XC.PACKAGE_NAME, name);
            par.setAttribute(XC.PACKAGE_MEMBERS, "0");
            par.setAttribute(XC.PACKAGE_TESTED, "0");
            parentPkg.appendChild(par);
        }
        return par;
    }

    private static void updateCounters(Element pck, int memberCount, int testedCount) {
        Element p = pck;
        while (p.getNodeName().equals(XC.PACKAGE)) {
            int count = Integer.parseInt(p.getAttribute(XC.PACKAGE_MEMBERS)) + memberCount;
            int tested = Integer.parseInt(p.getAttribute(XC.PACKAGE_TESTED)) + testedCount;
            p.setAttribute(XC.PACKAGE_MEMBERS, String.valueOf(count));
            p.setAttribute(XC.PACKAGE_TESTED, String.valueOf(tested));
            p = (Element) p.getParentNode();
        }
    }

    private static class CovDocument {

        DocumentBuilder builder;
        Document doc;
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        String fileName;

        public CovDocument(String fileName) throws ParserConfigurationException, SAXException, IOException {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(fileName);
            this.fileName = fileName;
        }

        public String getConfigValue(String key, String def) throws XPathExpressionException {
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("/" + XC.REPORT
                    + "/" + XC.HEAD
                    + "/" + XC.HEAD_PROPERTY);
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Element h = (Element) nl.item(i);
                if (h.getAttribute(XC.HEAD_PROPERTY_NAME).equals(key)) {
                    String v = h.getAttribute(XC.HEAD_PROPERTY_VALUE);
                    if (v == null || v.isEmpty()) {
                        return def;
                    } else {
                        return v;
                    }
                }
            }
            return def;
        }

        private Set<Element> getClasses() throws XPathExpressionException {
            return getElementsByKind(XC.CLASS);
        }

        private Set<Element> getElementsByKind(String name) throws XPathExpressionException {
            Set<Element> result = new HashSet<>();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//" + name);
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                result.add((Element) nl.item(i));
            }
            return result;
        }

        private static String getQname(Element e) {
            StringBuilder qName = new StringBuilder(e.getAttribute(XC.PACKAGE_NAME));
            Node parentNode;
            Node currNode = e;
            while ((parentNode = currNode.getParentNode()) != null) {
                if (!(parentNode instanceof Element)) {
                    break;
                }
                Element p = (Element) parentNode;
                if (!p.getNodeName().equals(XC.PACKAGE)
                        && !p.getNodeName().equals(XC.CLASS)) {
                    break;
                }
                String pName = p.getAttribute(XC.PACKAGE_NAME);
                if (pName != null && pName.isEmpty()) {
                    break;
                }
                qName.insert(0, pName + ".");
                currNode = p;
            }
            return qName.toString();
        }
    }
}
