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

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.search.Search;
import hudson.search.SearchIndex;
import hudson.security.ACL;
import hudson.security.Permission;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import jenkins.model.Jenkins;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.acegisecurity.AccessDeniedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class DeleteJobsTest extends TestCase {
    @Mock
    private Jenkins mockJenkins;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(mockJenkins);
    }
    
    @Test
    public void deleteTest() throws Exception {
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        when(request.getSubmittedForm()).thenReturn(jsonForm);
        
        List<String> jobs = new ArrayList<>();
        final String PROJECT_KEEP1 = "jobToKeep";
        final String PROJECT_KEEP2 = "keep_project_otherEnv";
        final String PROJECT_MANAGE1 = "project.vcast_manage.singlejob";
        final String PROJECT_MANAGE2 = "project.vcast_manage.multijob";
        final String PROJECT_MANAGE3 = "project_otherEnv";
        jobs.add(PROJECT_KEEP1);
        jobs.add(PROJECT_KEEP2);
        jobs.add(PROJECT_MANAGE1);
        jobs.add(PROJECT_MANAGE2);
        jobs.add(PROJECT_MANAGE3);
        when(mockJenkins.getJobNames()).thenReturn(jobs);
        
        DeleteJobs deleteJobs = new DeleteJobs(request, response);
        deleteJobs.createJobList();
        Assert.assertEquals(3, deleteJobs.getJobsToDelete().size());
        Assert.assertTrue(deleteJobs.getJobsToDelete().contains(PROJECT_MANAGE1));
        Assert.assertTrue(deleteJobs.getJobsToDelete().contains(PROJECT_MANAGE2));
        Assert.assertTrue(deleteJobs.getJobsToDelete().contains(PROJECT_MANAGE3));
        Assert.assertFalse(deleteJobs.getJobsToDelete().contains(PROJECT_KEEP1));
        Assert.assertFalse(deleteJobs.getJobsToDelete().contains(PROJECT_KEEP2));

        List<Item> items = new ArrayList<>();
        Item job1 = new TestJob(PROJECT_KEEP1);
        Item job2 = new TestJob(PROJECT_KEEP2);
        Item job3 = new TestJob(PROJECT_MANAGE1);
        Item job4 = new TestJob(PROJECT_MANAGE2);
        Item job5 = new TestJob(PROJECT_MANAGE3);
        items.add(job1);
        items.add(job2);
        items.add(job3);
        items.add(job4);
        items.add(job5);
        when(mockJenkins.getAllItems()).thenReturn(items);
        deleteJobs.doDelete();
        for (Item item : items) {
            TestJob job = (TestJob)item;
            if (job.name.equals(PROJECT_KEEP1) || job.name.equals(PROJECT_KEEP2)) {
                Assert.assertFalse(job.deleted);
            } else {
                Assert.assertTrue(job.deleted);
            }
        }
    }
    
    public class TestJob implements Item {

        public String name;
        public boolean deleted = false;
        public TestJob(String name) {
            this.name = name;
        }

        @Override
        public ItemGroup<? extends Item> getParent() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Collection<? extends Job> getAllJobs() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getFullName() {
            return name;
        }

        @Override
        public String getDisplayName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getFullDisplayName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getRelativeNameFrom(ItemGroup g) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getRelativeNameFrom(Item item) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getUrl() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getShortUrl() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getAbsoluteUrl() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onCopiedFrom(Item src) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onCreatedFromScratch() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void save() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void delete() throws IOException, InterruptedException {
            deleted = true;
        }

        @Override
        public File getRootDir() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Search getSearch() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getSearchName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getSearchUrl() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public SearchIndex getSearchIndex() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ACL getACL() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void checkPermission(Permission permission) throws AccessDeniedException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean hasPermission(Permission permission) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        
    }
}
