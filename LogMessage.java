package account;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity

public class LogMessage {
    @Id
    private long id;
    private String date;
    private String action;
    private String subject;
    private String object;
    private String path;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public String getDate() {
        return date;
    }

    public String getObject() {
        return object;
    }

    public String getPath() {
        return path;
    }

    public String getSubject() {
        return subject;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
