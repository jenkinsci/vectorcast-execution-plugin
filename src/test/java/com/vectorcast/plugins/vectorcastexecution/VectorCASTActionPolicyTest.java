package com.vectorcast.plugins.vectorcastexecution;

import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;
import hudson.scm.NullSCM;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Checks menu visibility and icon compatibility without a controller startup. */
class VectorCASTActionPolicyTest {
    @ParameterizedTest
    @CsvSource({"2.360, vector_favicon.png", "2.361, vector_favicon_bw.png",
        "1.600, vector_favicon.png", "development, vector_favicon.png"})
    void selectsCompatibleIconsForAllMenuLocations(String version, String icon) {
        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.hasPermission(VcastUtils.getViewPermission())).thenReturn(true);
        String originalVersion = Jenkins.VERSION;
        try (MockedStatic<Jenkins> access = mockStatic(Jenkins.class)) {
            access.when(Jenkins::get).thenReturn(jenkins);
            Jenkins.VERSION = version;
            String expected = "/plugin/vectorcast-execution/icons/" + icon;
            assertEquals(expected, new NamedAction().getIconFileName());
            assertEquals(expected, new VectorCASTJobRoot().getIconFileName());
            assertEquals(expected, new VectorCASTFolderAction(null).getIconFileName());
        } finally {
            Jenkins.VERSION = originalVersion;
        }
    }

    @Test
    void hidesMenusWhenTheUserCannotViewVectorcast() {
        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.hasPermission(VcastUtils.getViewPermission())).thenReturn(false);
        try (MockedStatic<Jenkins> access = mockStatic(Jenkins.class)) {
            access.when(Jenkins::get).thenReturn(jenkins);
            assertNull(new NamedAction().getIconFileName());
            assertNull(new VectorCASTJobRoot().getIconFileName());
            assertNull(new VectorCASTJobRoot().getUrlName());
            assertNull(new VectorCASTFolderAction(null).getIconFileName());
        }
    }

    @Test
    void suppliesDefaultsForActionsWithoutAFolder() {
        NamedAction action = new NamedAction();
        assertEquals("NamedAction", action.getUrlName());
        assertEquals("NamedAction", action.getDisplayName());
        assertInstanceOf(NullSCM.class, action.getTheScm());
        assertNull(action.getFolder());
        VectorCASTFolderAction folderAction = new VectorCASTFolderAction(null);
        assertEquals("", folderAction.getFolderName());
        assertEquals("", folderAction.getFolderFullName());
    }

    private static final class NamedAction extends JobBase {
    }
}
