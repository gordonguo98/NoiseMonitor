package com.gordon.noisemonitor;

public class MessageBean {

    private String requestId;
    private ReportedBean reported;
    private DesiredBean desired;
    private LastUpdatedTimeBean lastUpdatedTime;
    private long profileVersion;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

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

    public LastUpdatedTimeBean getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(LastUpdatedTimeBean lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    public long getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(long profileVersion) {
        this.profileVersion = profileVersion;
    }
}
