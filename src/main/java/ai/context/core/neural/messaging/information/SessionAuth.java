package ai.context.core.neural.messaging.information;

import java.io.Serializable;

public class SessionAuth implements Serializable{
    private final String auth;

    public SessionAuth(String auth) {
        this.auth = auth;
    }

    public String getAuth() {
        return auth;
    }
}
