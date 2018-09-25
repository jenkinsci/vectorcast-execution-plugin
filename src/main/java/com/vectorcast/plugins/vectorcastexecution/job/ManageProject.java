/*
 * The MIT License
 *
 * Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.vectorcast.plugins.vectorcastexecution.job;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Manage project
 */
public class ManageProject {
    /** Manage file */
    private String manageFile;
    /** Groups */
    private List<Group> groups;
    /** Sources - only valid if version of Manage file < 17 */
    private List<Source> sources;
    /** Compilers - only valid if version of Manage file >= 17 */
    private List<Compiler> compilers;
    /** Jobs */
    private List<MultiJobDetail> jobs;
    /**
     * Get groups
     * @return groups
     */
    public List<Group> getGroups() {
        return groups;
    }
    /**
     * Get jobs
     * @return jobs
     */
    public List<MultiJobDetail> getJobs() {
        return jobs;
    }
    /**
     * Constructor
     * @param manageFile manage file contents
     */
    public ManageProject(String manageFile) {
        this.manageFile = manageFile;
        groups = new ArrayList<>();
        sources = new ArrayList<>();
        compilers = new ArrayList<>();
        jobs = new ArrayList<>();
    }
    /**
     * Parse the project file
     * @throws IOException exception
     * @throws InvalidProjectFileException exception
     */
    public void parse() throws IOException, InvalidProjectFileException {
        Integer version = 14;
        Boolean SourceCollectionFound = false;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
            InputStream is = IOUtils.toInputStream(manageFile);
            Document doc = dbBuilder.parse(is);
            
            NodeList nList = doc.getElementsByTagName("project");
            Node projectNode = nList.item(0);
            String verStr = ((Element)projectNode).getAttribute("version");
            version = Integer.valueOf(verStr);
            for (int pos = 0; pos < nList.getLength(); pos++) {
                Node node = nList.item(pos);
                NodeList innerList = node.getChildNodes();
                for (int inner = 0; inner < innerList.getLength(); inner++) {
                    Node innerNode = innerList.item(inner);
                    if (innerNode.getNodeName().equals("group") &&
                        node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element)innerNode;
                        String name = element.getAttribute("name");
                        Group group = new Group(name);
                        groups.add(group);
                        group.parse(innerNode);
                    } else if (innerNode.getNodeName().equals("source-collection") &&
                        node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element)innerNode;
                        String name = element.getAttribute("name");
                        Source source = new Source(name);
                        sources.add(source);
                        source.parse(innerNode);
                        SourceCollectionFound = true;
                    } else if (version >= 17 && innerNode.getNodeName().equals("compiler") &&
                        node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element)innerNode;
                        Compiler compiler = new Compiler();
                        compilers.add(compiler);
                        compiler.parse(innerNode);
                    }
                }
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(NewSingleJob.class.getName()).log(Level.SEVERE, null, ex);
            throw new InvalidProjectFileException();
        } catch (SAXException ex) {
//            Logger.getLogger(NewSingleJob.class.getName()).log(Level.SEVERE, null, ex);
            throw new InvalidProjectFileException();
        }
        if (SourceCollectionFound) {
            for (Source source : sources) {
                for (Platform platform : source.platforms) {
                    for (Compiler compiler : platform.compilers) {
                        for (TestSuite testSuite : compiler.testsuites) {
                            for (Group group : testSuite.groups) {
                                for (Environment env : group.getEnvs()) {
                                    MultiJobDetail job = new MultiJobDetail(
                                            source.getName(),
                                            platform.getName(),
                                            compiler.getName(),
                                            testSuite.getName(),
                                            env.getName());
                                    jobs.add(job);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (Compiler compiler : compilers) {
                for (TestSuite testSuite : compiler.testsuites) {
                    for (Group group : testSuite.groups) {
                        for (Environment env : group.getEnvs()) {
                            MultiJobDetail job = new MultiJobDetail(
                                    /*source*/null,
                                    /*platform*/null,
                                    compiler.getName(),
                                    testSuite.getName(),
                                    env.getName());
                            jobs.add(job);
                        }
                    }
                }
            }
        }
    }
    
    private abstract class BaseElement {
        private String name;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public BaseElement(String name) {
            this.name = name;
        }
    }
    private class Environment extends BaseElement {
        public Environment(String name) {
            super(name);
        }
    }
    private class Compiler extends BaseElement {
        private List<TestSuite> testsuites;
        public Compiler() {
            super(null);
            testsuites = new ArrayList<TestSuite>();
        }
        public void parse(Node node) {
            // Find compiler name and testsuites
            NodeList list = node.getChildNodes();
            for (int pos = 0; pos < list.getLength(); pos++) {
                Node innerNode = list.item(pos);
                if (innerNode.getNodeName().equals("compiler") &&
                    innerNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)innerNode;
                    String compilerName = element.getElementsByTagName("name").item(0).getTextContent();
                    setName(compilerName);
                } else if (innerNode.getNodeName().equals("testsuite") &&
                    innerNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)innerNode;
                    String name = element.getAttribute("name");
                    TestSuite testSuite = new TestSuite(name);
                    testsuites.add(testSuite);
                    testSuite.parse(innerNode);
                }

            }
        }
    }
    private class Group extends BaseElement {
        private List<Environment> envs;
        public Group(String name) {
            super(name);
            envs = new ArrayList<Environment>();
        }
        public List<Environment> getEnvs() {
            return envs;
        }
        public void parse(Node node) {
            NodeList list = node.getChildNodes();
            for (int pos = 0; pos < list.getLength(); pos++) {
                Node innerNode = list.item(pos);
                if (innerNode.getNodeName().equals("environment") &&
                    innerNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)innerNode;
                    String name = element.getAttribute("name");
                    Environment env = new Environment(name);
                    envs.add(env);
                }
            }
        }
    }
    private class Platform extends BaseElement {
        private List<Compiler> compilers;
        public Platform(String name) {
            super(name);
            compilers = new ArrayList<Compiler>();
        }
        public List<Compiler> getCompilers() {
            return compilers;
        }
        public void parse(Node node) {
            NodeList list = node.getChildNodes();
            for (int pos = 0; pos < list.getLength(); pos++) {
                Node innerNode = list.item(pos);
                if (innerNode.getNodeName().equals("compiler") &&
                    innerNode.getNodeType() == Node.ELEMENT_NODE) {
                    Compiler compiler = new Compiler();
                    compilers.add(compiler);
                    compiler.parse(innerNode);
                }
            }
        }
    }
    private class Source extends BaseElement {
        private List<Platform> platforms;
        public Source(String name) {
            super(name);
            platforms = new ArrayList<Platform>();
        }
        public void parse(Node node) {
            NodeList list = node.getChildNodes();
            for (int pos = 0; pos < list.getLength(); pos++) {
                Node innerNode = list.item(pos);
                if (innerNode.getNodeName().equals("platform") &&
                    innerNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)innerNode;
                    String name = element.getAttribute("name");
                    Platform platform = new Platform(name);
                    platforms.add(platform);
                    platform.parse(innerNode);
                }
            }
        }
    }
    private class TestSuite extends BaseElement {
        List<Group> groups;
        public TestSuite(String name) {
            super(name);
            groups = new ArrayList<Group>();
        }
        public void parse(Node node) {
            NodeList list = node.getChildNodes();
            for (int pos = 0; pos < list.getLength(); pos++) {
                Node innerNode = list.item(pos);
                if (innerNode.getNodeName().equals("group") &&
                    innerNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)innerNode;
                    String name = element.getAttribute("name");
                    for (Group grp : getGroups()) {
                        if (grp.getName().equals(name)) {
                            groups.add(grp);
                            break;
                        }
                    }
                }
            }
        }
    }
}
