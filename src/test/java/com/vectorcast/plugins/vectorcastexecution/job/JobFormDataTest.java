package com.vectorcast.plugins.vectorcastexecution.job;

import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JobFormDataTest {
    @Test
    void providesTypedValuesAndSafeDefaults() {
        JSONObject imported = new JSONObject();
        imported.put("value", 2);
        imported.put("externalResultsFilename", "results.vcr");
        JSONObject source = new JSONObject();
        source.put("project", "demo.vcm");
        source.put("enabled", true);
        source.put("parallel", 4);
        source.put("importedResults", imported);

        JobFormData form = JobFormData.from(source);

        assertEquals("demo.vcm", form.text("project", ""));
        assertEquals("default", form.text("missing", "default"));
        assertFalse(form.flag("missing", false));
        assertEquals(4, form.number("parallel", 0));
        assertEquals(2, form.section("importedResults").number("value", 0));
        assertEquals("results.vcr", form.section("importedResults")
            .text("externalResultsFilename", ""));
        assertEquals("", form.section("missing").text("value", ""));
    }
}
