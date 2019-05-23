package gov.nasa.jpl.mbee.mms.domains;


/**
 * Created by dlam on 7/26/18.
 * sync doc from one server to another server, can be different refs
 */
public class DocSyncRequest {

    private String fromTicket;

    private String toTicket;

    private String docId;

    private String fromMmsServer;

    private String toMmsServer;

    private String fromRefId;
    private String toRefId;

    private String fromProjectId;

    private String toProjectId;

    private String comment;

    public DocSyncRequest() {
    }

    public String getFromTicket() {
        return this.fromTicket;
    }

    public String getToTicket() {
        return this.toTicket;
    }

    public String getDocId() {
        return this.docId;
    }

    public String getFromMmsServer() {
        return this.fromMmsServer;
    }

    public String getToMmsServer() {
        return this.toMmsServer;
    }

    public String getFromRefId() {
        return this.fromRefId;
    }

    public String getToRefId() {
        return this.toRefId;
    }

    public String getFromProjectId() {
        return this.fromProjectId;
    }

    public String getToProjectId() {
        return this.toProjectId;
    }

    public String getComment() {
        return this.comment;
    }

    public void setFromTicket(String fromTicket) {
        this.fromTicket = fromTicket;
    }

    public void setToTicket(String toTicket) {
        this.toTicket = toTicket;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public void setFromMmsServer(String fromMmsServer) {
        this.fromMmsServer = fromMmsServer;
    }

    public void setToMmsServer(String toMmsServer) {
        this.toMmsServer = toMmsServer;
    }

    public void setFromRefId(String fromRefId) {
        this.fromRefId = fromRefId;
    }

    public void setToRefId(String toRefId) {
        this.toRefId = toRefId;
    }

    public void setFromProjectId(String fromProjectId) {
        this.fromProjectId = fromProjectId;
    }

    public void setToProjectId(String toProjectId) {
        this.toProjectId = toProjectId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String toString() {
        return "DocSyncRequest(fromTicket=" + this.getFromTicket() + ", toTicket=" + this.getToTicket() + ", docId=" + this.getDocId() + ", fromMmsServer=" + this.getFromMmsServer() + ", toMmsServer=" + this.getToMmsServer() + ", fromRefId=" + this.getFromRefId() + ", toRefId=" + this.getToRefId() + ", fromProjectId=" + this.getFromProjectId() + ", toProjectId=" + this.getToProjectId() + ", comment=" + this.getComment() + ")";
    }
}
