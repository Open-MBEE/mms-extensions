package gov.nasa.jpl.mbee.mms.domains;


public class DocMergeRequest {

    private String ticket;

    private String docId;

    private String fromRefId;

    private String toRefId;

    private String mmsServer;

    private String projectId;

    private String comment;

    public DocMergeRequest() {
    }

    public String getTicket() {
        return this.ticket;
    }

    public String getDocId() {
        return this.docId;
    }

    public String getFromRefId() {
        return this.fromRefId;
    }

    public String getToRefId() {
        return this.toRefId;
    }

    public String getMmsServer() {
        return this.mmsServer;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public String getComment() {
        return this.comment;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public void setFromRefId(String fromRefId) {
        this.fromRefId = fromRefId;
    }

    public void setToRefId(String toRefId) {
        this.toRefId = toRefId;
    }

    public void setMmsServer(String mmsServer) {
        this.mmsServer = mmsServer;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String toString() {
        return "DocMergeRequest(ticket=" + this.getTicket() +  ", docId=" + this.getDocId() + ", fromRefId=" + this.getFromRefId() + ", toRefId=" + this.getToRefId() + ", mmsServer=" + this.getMmsServer() + ", projectId=" + this.getProjectId() + ", comment=" + this.getComment() + ")";
    }
}