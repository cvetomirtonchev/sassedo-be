package server.sassedo.user.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordSuccess {
    private Boolean isSuccess;

    public ResetPasswordSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}
