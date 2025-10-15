package uk.gov.moj.cpp.authz.drools;

public final class Outcome {
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "Outcome{success=" + success + '}';
    }
}
