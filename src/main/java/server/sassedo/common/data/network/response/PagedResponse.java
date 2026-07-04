package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PagedResponse<T> {
    private List<T> content;
    private PageMeta meta;

    public PagedResponse(List<T> content, PageMeta meta) {
        this.content = content;
        this.meta = meta;
    }
}
