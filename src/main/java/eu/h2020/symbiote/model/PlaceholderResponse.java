package eu.h2020.symbiote.model;

/**
 * Class used as a response to RPC call requesting placeholder actions
 *
 * Created by tipech
 */
public class PlaceholderResponse {
    private int status;

    public PlaceholderResponse(int status) {
        this.status = status;
    }

    public PlaceholderResponse() {
    }

    /**
     * @return
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
    }
}
