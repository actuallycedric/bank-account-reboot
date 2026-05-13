package com.clarence.bank_api.dto;

import java.util.List;

public class PaginationResponse<T> {

    long totalElements;

    int currentPage;

    int leftoverPages;

    List<T> content;

    boolean hasNext;

    public PaginationResponse(){}

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getLeftoverPages() {
        return leftoverPages;
    }

    public void setLeftoverPages(int leftoverPages) {
        this.leftoverPages = leftoverPages;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
