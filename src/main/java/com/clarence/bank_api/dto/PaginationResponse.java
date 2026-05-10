package com.clarence.bank_api.dto;

import java.util.List;

public class PaginationResponse {

    long totalTransactions;

    int currentPage;

    int leftoverPages;

    List<TransactionResponse> content;

    boolean hasNext;

    public PaginationResponse(){}

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
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

    public List<TransactionResponse> getContent() {
        return content;
    }

    public void setContent(List<TransactionResponse> content) {
        this.content = content;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
