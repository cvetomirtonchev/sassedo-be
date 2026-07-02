package server.sassedo.common.data.network.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageMeta {
    private int currentPage;
    private int pages;
    private long totalElements;

    public PageMeta(int currentPage, int pages, long totalElements) {
        this.currentPage = currentPage;
        this.pages = pages;
        this.totalElements = totalElements;
    }
}
