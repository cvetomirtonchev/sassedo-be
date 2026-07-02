package server.sassedo.common.data.network.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserQuestionStatusRequest {
    private List<Long> questions;
}
