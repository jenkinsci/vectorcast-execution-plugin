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
package com.vectorcast.plugins.vectorcastexecution;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * Class for initialising the VectorCAST create job.
 * 
 * On startup, create the job if it does not already exist.
 */
public class JobInit {
    private static final String JOBNAME = "VectorCAST Create Jobs from Manage Project";
    private static final String JOBCFG = "JobConfig.xml";
    
    /**
     * After all Jenkins job configurations have loaded, check if the 
     * VectorCAST create job already exists. Create it if required.
     */
//    @Initializer(after=InitMilestone.JOB_LOADED) 
//    public static void addVCJob() {
//        Logger log = Logger.getLogger(JobInit.class.getName());
//        Jenkins instance = Jenkins.getInstance();
//        if (instance != null) {
//            Collection<String> jobs = instance.getJobNames();
//            boolean add = true;
//            for (String job : jobs) {
//                if (job.equals(JOBNAME)) {
//                    add = false;
//                    break;
//                }
//            }
//            if (add) {
//                try {
//                    log.log(Level.INFO, "Add " + JOBNAME);
//                    InputStream is = JobInit.class.getClassLoader().getResourceAsStream(JOBCFG);
//                    instance.createProjectFromXML(JOBNAME, is);
//                } catch (IOException ex) {
//                    Logger.getLogger(JobInit.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }
//
//    }
}
