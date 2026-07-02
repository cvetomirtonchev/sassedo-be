package server.sassedo.common.data.network.response;

public class HelperTextResponse {
    private long id;
    private String key;
    private String value;

    public HelperTextResponse(long id, String key, String value) {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
