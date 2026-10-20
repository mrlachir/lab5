package com.example.util;
import java.util.*;

public class PaginationResult<T> {
    private final List<T> items;
    private final int currentPage, pageSize, totalPages;
    private final long totalItems;
    public PaginationResult(List<T> items, int currentPage, int pageSize, long totalItems) {
        if (currentPage < 1 || pageSize < 1 || totalItems < 0) throw new IllegalArgumentException("Invalid pagination");
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.currentPage = currentPage; this.pageSize = pageSize; this.totalItems = totalItems;
        this.totalPages = Math.toIntExact(totalItems / pageSize + (totalItems % pageSize == 0 ? 0 : 1));
    }
    public List<T> getItems() { return items; }
    public int getCurrentPage() { return currentPage; }
    public int getPageSize() { return pageSize; }
    public int getTotalPages() { return totalPages; }
    public long getTotalItems() { return totalItems; }
}
