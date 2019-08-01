package com.tango.bar;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class TransactionSummary {

    private final long timestamp;
    private final double max;
    private final double min;
    private final double avg;
    private final double volume;
    private final long count;

    public TransactionSummary(long timestamp, double max, double min, double avg, double volume, long count) {
        this.timestamp = timestamp;
        this.max = max;
        this.min = min;
        this.avg = avg;
        this.volume = volume;
        this.count = count;
    }

    @JsonIgnore
    public long getTimestamp() {
        return timestamp;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getAvg() {
        return avg;
    }

    @JsonIgnore
    public double getVolume() {
        return volume;
    }

    public long getCount() {
        return count;
    }
}
