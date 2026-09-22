package com.vectorcast.plugins.vectorcastexecution.job;

import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobCreationRequestTest {
    @Test
    void parsesDefaultsAndKeepsValuesAfterJsonChanges() throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "  C:\\work\\demo  ");
        form.put("jobName", "  nightly build  ");
        form.put("sharedArtifactDir", "  C:\\artifacts  ");

        JobCreationRequest request = JobCreationRequest.parse(form);
        form.put("jobName", "changed");

        assertEquals("C:/work/demo.vcm", request.manageProjectName());
        assertEquals("nightly_build", request.jobName());
        assertEquals("--workspace=C:/artifacts", request.sharedArtifactDirectory());
        assertEquals(30, request.waitTime());
        assertEquals(1, request.waitLoops());
        assertTrue(request.useCBT());
        assertTrue(request.optionUseReporting());
        assertFalse(request.useImportedResults());
    }

    @Test
    void rejectsMissingExternalResultsFile() {
        JSONObject importedResults = new JSONObject();
        importedResults.put("value", 2);
        importedResults.put("externalResultsFilename", "  ");
        JSONObject form = new JSONObject();
        form.put("useImportedResults", true);
        form.put("importedResults", importedResults);

        assertThrows(ExternalResultsFileException.class,
            () -> JobCreationRequest.parse(form));
    }
}
