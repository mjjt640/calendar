package com.example.calendar.data.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ScheduleBackupJsonConverter {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String toJson(ScheduleBackupPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Backup payload must not be null.");
        }
        return gson.toJson(payload);
    }

    public ScheduleBackupPayload fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Backup json must not be empty.");
        }
        ScheduleBackupPayload payload = gson.fromJson(json, ScheduleBackupPayload.class);
        if (payload == null) {
            throw new IllegalArgumentException("Backup json is invalid.");
        }
        if (payload.schedules == null) {
            payload.schedules = new java.util.ArrayList<>();
        }
        if (payload.recurrenceSeries == null) {
            payload.recurrenceSeries = new java.util.ArrayList<>();
        }
        if (payload.recurrenceExceptions == null) {
            payload.recurrenceExceptions = new java.util.ArrayList<>();
        }
        return payload;
    }
}
