package server.sassedo.user.data.network.request;

import java.util.Set;

public class UpdateUserRoleRequest {
    private long userId;
    private Set<Long> newRoles;

    public long getUserId() {
        return userId;
    }

    public Set<Long> getNewRoles() {
        return newRoles;
    }

    public void setNewRoles(Set<Long> newRoles) {
        this.newRoles = newRoles;
    }

    public void setUserId(long userId) {
        this.userId = userId;

    }
}
