/*
 * The MIT License
 *
 * Copyright 2026 Vector Software, East Greenwich, Rhode Island USA
 */
package com.vectorcast.plugins.vectorcastexecution.job;

import net.sf.json.JSONObject;

/**
 * Immutable boundary around data submitted by a job-creation form.
 *
 * <p>This keeps Stapler and Jenkins request handling in the action layer.
 * Job construction receives this typed access point instead of reading a
 * {@code StaplerRequest} directly.</p>
 */
public final class JobFormData {
    private final JSONObject values;

    private JobFormData(final JSONObject inputValues) {
        values = inputValues == null ? new JSONObject() : inputValues;
    }

    /** Creates form data from the JSON object supplied by Stapler. */
    public static JobFormData from(final JSONObject inputValues) {
        return new JobFormData(inputValues);
    }

    /** Returns a text value, or the supplied default when absent. */
    public String text(final String name, final String defaultValue) {
        return values.optString(name, defaultValue);
    }

    /** Returns a boolean value, or the supplied default when absent. */
    public boolean flag(final String name, final boolean defaultValue) {
        return values.optBoolean(name, defaultValue);
    }

    /** Returns a long value, or the supplied default when absent. */
    public long number(final String name, final long defaultValue) {
        return values.optLong(name, defaultValue);
    }

    /** Returns a nested form section, when present. */
    public JobFormData section(final String name) {
        return new JobFormData(values.optJSONObject(name));
    }
}
