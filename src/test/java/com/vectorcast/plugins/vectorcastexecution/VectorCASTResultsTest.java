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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.lang.Throwable;
import hudson.FilePath;
import org.apache.commons.io.FileUtils;

public class VectorCASTResultsTest {
    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    private Boolean runCommand(String command) {
        int retVal = 0;
        
        try {
            
            System.out.println("Command: " + command);
            //command = "set WORKSPACE=%cd% && " + command;
            
            System.out.println("starting command");
            Process p = Runtime.getRuntime().exec(command);            
          
            System.out.println("getting stdout buffer reader");
            BufferedReader stdOut   = new BufferedReader(new InputStreamReader(p.getInputStream()));

            System.out.println("getting stderr buffer reader");
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        
            System.out.println("waiting for process to finish");
            retVal = p.waitFor();
            
            System.out.println("finished...");
            
            System.out.println("Return Code: " + retVal);
            System.out.println("StdOut: ");

            String line  = "";
            
            while ((line = stdOut.readLine()) != null) {
                System.out.println(line);
            }     
            
            System.out.println("StdErr: ");
            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }            
            
        } catch (IOException  e) {
            // printStackTrace method 
            // prints line numbers + call stack 
            e.printStackTrace(); 
              
            // Prints what exception has been thrown 
            System.out.println(e); 
            retVal = -1;
        } catch (InterruptedException  e) {
            // printStackTrace method 
            // prints line numbers + call stack 
            e.printStackTrace(); 
              
            // Prints what exception has been thrown 
            System.out.println(e); 
            retVal = -1;
        }
        
        // return error
        return (0 != retVal);
    }
    
    private void processDir(File dir, String base, FilePath destDir)  throws IOException, InterruptedException {
        destDir.mkdirs();
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                FilePath newDest = new FilePath(destDir, file.getName());
                processDir(file, base + "/" + file.getName(), newDest);
            } else {
                File newFile = new File(destDir + File.separator + file.getName());
                FileUtils.copyFile(file, newFile);
            }
        }
    }
    
    private void copyScripts(String source, String destination) {
        try {
            
            File scriptDir = new File(source);
            File workspace_dir = new File(".");
            FilePath workspace_fp = new FilePath(workspace_dir);
            FilePath destScriptDir = new FilePath(workspace_fp, destination);
            processDir(scriptDir, "./", destScriptDir);
        } catch (IOException  e) {
        } catch (InterruptedException  e) {
        }
    }
                
    private Boolean compareCoverageFiles() {
        Boolean retVal = false;
        try {
            List<String> lines = Files.readAllLines(Paths.get("coverage_results_VectorCAST_MinGW_C_TestSuite_TUTORIAL_C.xml"));
            lines.remove(0);
            String actual = String.join("\n", lines);

            if (coverageResultsFile ==  actual) {
                retVal = true;
            } else {
                System.out.println ("Expected: " + coverageResultsFile);
                System.out.println ("Dctual  : " + actual);
            }
        } catch (IOException  e) {
        }         
        return retVal;
    }
        
    private Boolean compareTestResultsFiles() { 
        Boolean retVal = false;
        try {
            List<String> lines = Files.readAllLines(Paths.get("target/xml_data/test_results_enterprise_testing_demo_combined_final.xml"));
            String actual = String.join("\n", lines); ;

            if (testResultsFile ==  actual) {
                retVal = true;
            } else {
                System.out.println ("Expected: " + testResultsFile);
                System.out.println ("Dctual  : " + actual);
            }
        } catch (IOException  e) {
        }         
        return retVal;
    }

    @Test
    public void resultsTester() throws Exception {
        Boolean coverageResultsFilesMatched = true;
        Boolean testResultsFilesMatched = true;
        Boolean vpythonTracebackError = false;
        
        Map<String, String> env = System.getenv();

        if (env.containsKey("VECTORCAST_DIR")) {
            
            String VCD = env.get("VECTORCAST_DIR");
            String genResult      = VCD + "/vpython vc_scripts/generate-results.py --junit target/test-classes/enterprise_testing_demo.vcm";
            String genResultFinal = VCD + "/vpython vc_scripts/generate-results.py --junit target/test-classes/enterprise_testing_demo.vcm --final";

            copyScripts("target/classes/scripts","vc_scripts");

            vpythonTracebackError = runCommand(genResult);
            vpythonTracebackError = vpythonTracebackError || runCommand(genResultFinal);
            
            coverageResultsFilesMatched = compareCoverageFiles();
            testResultsFilesMatched     = compareTestResultsFiles();
            
            if (vpythonTracebackError) {
                throw new VectorCASTResultsException("Error in vpython scripts");
            }
            
            if (!coverageResultsFilesMatched) {
                throw new VectorCASTResultsException("Coverage results files don't match");
            }
            
            if (!testResultsFilesMatched) {
                throw new VectorCASTResultsException("Test results files don't match");
            }
        }
    }
    
    private static final String testResultsFile =
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
"<testsuites>\n"+
"    <testsuite errors=\"0\" tests=\"2\" failures=\"0\" name=\"TUTORIAL_C\" id=\"1\">\n"+
"\n"+
"        <testcase name=\"PLACE_ORDER.001\" classname=\"VectorCAST_MinGW_C.TestSuite.TUTORIAL_C\" time=\"0\">\n"+
"            \n"+
"            <system-out>\n"+
"PASS 4 / 4  &#xA;&#xA;Execution Report:&#xA; Start of Test Case:   PLACE_ORDER.001&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 1                           -----&#xA;----------------------------------------------------------------------&#xA;   Calling UUT: manager.c&#xA;     Subprogram: Place_Order&#xA;       Table =&gt; 2&#xA;       Seat =&gt; 0&#xA;       Order&#xA;         Soup =&gt; ONION&#xA;         Salad =&gt; CAESAR&#xA;         Entree =&gt; STEAK&#xA;         Beverage =&gt; MIXED_DRINK&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 2                           -----&#xA;----------------------------------------------------------------------&#xA;   Stub called: manager.c&#xA;     Subprogram: Get_Table_Record&#xA;       return&#xA;         Number_In_Party =&gt; 0&#xA;         Check_Total =&gt; 0&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 3                           -----&#xA;----------------------------------------------------------------------&#xA;   Stub called: manager.c&#xA;     Subprogram: Update_Table_Record&#xA;       Data&#xA;         Is_Occupied =&gt; v_true                                 &lt;match&gt;&#xA;         Number_In_Party =&gt; 1                                  &lt;match&gt;&#xA;         Order&#xA;           [0] &#xA;             Dessert =&gt; PIE                                    &lt;match&gt;&#xA;         Check_Total =&gt; 14                                     &lt;match&gt;&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 4                           -----&#xA;----------------------------------------------------------------------&#xA;   Returned from UUT: manager.c&#xA;     Subprogram: Place_Order&#xA;       Table =&gt; 2&#xA;       Seat =&gt; 0&#xA;       Order&#xA;         Soup =&gt; ONION&#xA;         Salad =&gt; CAESAR&#xA;         Entree =&gt; STEAK&#xA;         Beverage =&gt; MIXED_DRINK&#xA;&#xA;&#xA;   UUT Returned control to Driver ...&#xA;&#xA;End of Test Case&#xA;&#xA;&#xA;&#xA;Expected Results matched 100%                         ( 4 / 4 )   PASS&#xA;Test Status                                                       PASS&#xA;&#xA;                     \n"+
"            </system-out>\n"+
"        </testcase>\n"+
"\n"+
"        <testcase name=\"PLACE_ORDER.002\" classname=\"VectorCAST_MinGW_C.TestSuite.TUTORIAL_C\" time=\"0\">\n"+
"            \n"+
"            <system-out>\n"+
"PASS 4 / 4  &#xA;&#xA;Execution Report:&#xA; Start of Test Case:   PLACE_ORDER.002&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 1                           -----&#xA;----------------------------------------------------------------------&#xA;   Calling UUT: manager.c&#xA;     Subprogram: Place_Order&#xA;       Table =&gt; 2&#xA;       Seat =&gt; 0&#xA;       Order&#xA;         Soup =&gt; ONION&#xA;         Salad =&gt; CAESAR&#xA;         Entree =&gt; STEAK&#xA;         Beverage =&gt; MIXED_DRINK&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 2                           -----&#xA;----------------------------------------------------------------------&#xA;   Stub called: manager.c&#xA;     Subprogram: Get_Table_Record&#xA;       return&#xA;         Number_In_Party =&gt; 0&#xA;         Check_Total =&gt; 0&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 3                           -----&#xA;----------------------------------------------------------------------&#xA;   Stub called: manager.c&#xA;     Subprogram: Add_Included_Dessert&#xA;       Order&#xA;         [0] &#xA;           Dessert =&gt; NO_DESSERT&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 4                           -----&#xA;----------------------------------------------------------------------&#xA;   Stub called: manager.c&#xA;     Subprogram: Update_Table_Record&#xA;       Data&#xA;         Is_Occupied =&gt; v_true                                 &lt;match&gt;&#xA;         Number_In_Party =&gt; 1                                  &lt;match&gt;&#xA;         Order&#xA;           [0] &#xA;             Dessert =&gt; CAKE                                   &lt;match&gt;&#xA;         Check_Total =&gt; 14                                     &lt;match&gt;&#xA;&#xA;&#xA;----------------------------------------------------------------------&#xA;-----                          Event 5                           -----&#xA;----------------------------------------------------------------------&#xA;   Returned from UUT: manager.c&#xA;     Subprogram: Place_Order&#xA;       Table =&gt; 2&#xA;       Seat =&gt; 0&#xA;       Order&#xA;         Soup =&gt; ONION&#xA;         Salad =&gt; CAESAR&#xA;         Entree =&gt; STEAK&#xA;         Beverage =&gt; MIXED_DRINK&#xA;&#xA;&#xA;   UUT Returned control to Driver ...&#xA;&#xA;End of Test Case&#xA;&#xA;&#xA;&#xA;Expected Results matched 100%                         ( 4 / 4 )   PASS&#xA;Test Status                                                       PASS&#xA;&#xA;                     \n"+
"            </system-out>\n"+
"        </testcase>\n"+
"   </testsuite>\n"+
"\n"+
"\n"+
"</testsuites>\n"+
"\n";
    
    private static final String coverageResultsFile =
// ignore first line "<!-- VectorCAST/Jenkins Integration, Generated 12 NOV 2019  12:15:50 PM -->\n"+
"<report>\n"+
"  <version value=\"3\"/>\n"+
"  <stats>\n"+
"    <environments value=\"1\"/>\n"+
"    <units value=\"1\"/>\n"+
"    <subprograms value=\"6\"/>\n"+
"  </stats>\n"+
"  <data>\n"+
"    <all name=\"all environments\">\n"+
"      <coverage type=\"statement, %\" value=\"26% (12 / 45)\"/>\n"+
"      <coverage type=\"complexity, %\" value=\"0% (16 / 0)\"/>\n"+
"\n"+
"      <environment name=\"VectorCAST_MinGW_C_TestSuite_TUTORIAL_C\">\n"+
"        <coverage type=\"statement, %\" value=\"26% (12 / 45)\"/>\n"+
"        <coverage type=\"complexity, %\" value=\"0% (16 / 0)\"/>\n"+
"\n"+
"        <unit name=\"manager\">\n"+
"          <coverage type=\"statement, %\" value=\"26% (12 / 45)\"/>\n"+
"          <coverage type=\"complexity, %\" value=\"0% (16 / 0)\"/>\n"+
"          <subprogram name=\"Add_Included_Dessert\">\n"+
"            <coverage type=\"statement, %\" value=\"50% (2 / 4)\"/>\n"+
"            <coverage type=\"complexity, %\" value=\"0% (3 / 0)\"/>\n"+
"          </subprogram>\n"+
"          <subprogram name=\"Place_Order\">\n"+
"            <coverage type=\"statement, %\" value=\"58% (10 / 17)\"/>\n"+
"            <coverage type=\"complexity, %\" value=\"0% (5 / 0)\"/>\n"+
"          </subprogram>\n"+
"          <subprogram name=\"Clear_Table\">\n"+
"            <coverage type=\"statement, %\" value=\"0% (0 / 12)\"/>\n"+
"            <coverage type=\"complexity, %\" value=\"0% (2 / 0)\"/>\n"+
"          </subprogram>\n"+
"          <subprogram name=\"Get_Check_Total\">\n"+
"            <coverage type=\"statement, %\" value=\"0% (0 / 2)\"/>\n"+
"            <coverage type=\"complexity, %\" value=\"0% (1 / 0)\"/>\n"+
"          </subprogram>\n"+
"          <subprogram name=\"Add_Party_To_Waiting_List\">\n"+
"            <coverage type=\"statement, %\" value=\"0% (0 / 7)\"/>\n"+
"            <coverage type=\"complexity, %\" value=\"0% (3 / 0)\"/>\n"+
"          </subprogram>\n"+
"          <subprogram name=\"Get_Next_Party_To_Be_Seated\">\n"+
"            <coverage type=\"statement, %\" value=\"0% (0 / 3)\"/>\n"+
"            <coverage type=\"complexity, %\" value=\"0% (2 / 0)\"/>\n"+
"          </subprogram>\n"+
"        </unit>\n"+
"      </environment>\n"+
"    </all>\n"+
"  </data>\n"+
"</report>\n";
    
}
