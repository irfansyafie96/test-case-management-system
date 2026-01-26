package com.yourproject.tcm.model.dto;

/**
 * Data Transfer Object for completion summary statistics
 * Used to display execution completion statistics to users
 */
public class CompletionSummaryDTO {
    private long total;
    private long passed;
    private long failed;
    private long blocked;
    private long pending;

    public CompletionSummaryDTO() {
    }

    public CompletionSummaryDTO(long total, long passed, long failed, long blocked, long pending) {
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.blocked = blocked;
        this.pending = pending;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPassed() {
        return passed;
    }

    public void setPassed(long passed) {
        this.passed = passed;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getBlocked() {
        return blocked;
    }

    public void setBlocked(long blocked) {
        this.blocked = blocked;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }
}