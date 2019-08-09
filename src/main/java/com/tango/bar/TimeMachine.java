package com.tango.bar;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeMachine {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static volatile Clock clock = Clock.systemDefaultZone();

    private TimeMachine() {
    }

    public static void setDate(LocalDateTime date) {
        clock = Clock.fixed(date.toInstant(ZONE_ID.getRules().getOffset(date)), ZONE_ID);
    }

    public static void reset() {
        clock = Clock.systemDefaultZone();
    }

    public static long nowMillis() {
        return clock.millis();
    }
}
