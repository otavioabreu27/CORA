package org.orbitalsuite.cora;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.google.protobuf.Timestamp;

import org.orbitalsuite.orbital_dynamics.grpc.v1.TimeWindow;

/** Shared validation and conversion for REST requests and AI tools. */
public final class OrbitalInputs {

    private OrbitalInputs() {}

    public static long positiveId(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static String requiredText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    public static int defaultableRange(Integer value, String name, int min, int max) {
        int resolved = value == null ? 0 : value;
        if (resolved != 0 && (resolved < min || resolved > max)) {
            throw new IllegalArgumentException(name + " must be 0 or between " + min + " and " + max);
        }
        return resolved;
    }

    public static double nonNegative(Double value, String name) {
        if (value == null || !Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be a finite non-negative number");
        }
        return value;
    }

    public static double positive(Double value, String name) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be a finite positive number");
        }
        return value;
    }

    public static Timestamp timestamp(String value) {
        requiredText(value, "timestamp");
        try {
            var instant = Instant.parse(value);
            return Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("invalid timestamp (use RFC 3339 with a time zone): " + value, e);
        }
    }

    public static TimeWindow window(String start, String end) {
        var startTimestamp = timestamp(start);
        var endTimestamp = timestamp(end);
        if (compare(startTimestamp, endTimestamp) >= 0) {
            throw new IllegalArgumentException("start must be before end");
        }
        return TimeWindow.newBuilder().setStart(startTimestamp).setEnd(endTimestamp).build();
    }

    public static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, requiredText(value, "value")
                    .toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid value: " + value, e);
        }
    }

    public static <E extends Enum<E>> E prefixedEnumValue(Class<E> type, String value, String prefix) {
        String normalized = requiredText(value, "value").toUpperCase(Locale.ROOT).replace('-', '_');
        return enumValue(type, normalized.startsWith(prefix) ? normalized : prefix + normalized);
    }

    private static int compare(Timestamp left, Timestamp right) {
        int seconds = Long.compare(left.getSeconds(), right.getSeconds());
        return seconds != 0 ? seconds : Integer.compare(left.getNanos(), right.getNanos());
    }
}
