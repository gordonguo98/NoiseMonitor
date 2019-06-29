package com.gordon.noisemonitor;

public class LastUpdatedTimeBean {
    private ReportedBean reported;
    private DesiredBean desired;

    public ReportedBean getReported() {
        return reported;
    }

    public void setReported(ReportedBean reported) {
        this.reported = reported;
    }

    public DesiredBean getDesired() {
        return desired;
    }

    public void setDesired(DesiredBean desired) {
        this.desired = desired;
    }

}
