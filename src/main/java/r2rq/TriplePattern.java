package r2rq;


public class TriplePattern {

    String subject;
    String property;
    String object;

    public TriplePattern (String sub, String prop, String obj) {
        subject = sub;
        property = prop;
        object = obj;
    }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public String getProperty() {
        return property;
    }
    public void setProperty(String property) {
        this.property = property;
    }
    public String getObject() {
        return object;
    }
    public void setObject(String object) {
        this.object = object;
    }

}
