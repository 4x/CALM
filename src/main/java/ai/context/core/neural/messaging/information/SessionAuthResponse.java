package ai.context.core.neural.messaging.information;

public class SessionAuthResponse {
    private final String authID;
    private final String clusterID;

    public SessionAuthResponse(String authID, String clusterID) {
        this.authID = authID;
        this.clusterID = clusterID;
    }

    public String getAuthID() {
        return authID;
    }

    public String getClusterID() {
        return clusterID;
    }
}
