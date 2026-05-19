package com.enterprise.fileservice.DTO;


import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    // ─────────────────────────────────────────────
    // Data
    // ─────────────────────────────────────────────
    private List<T> content;           // files for this page

    // ─────────────────────────────────────────────
    // Pagination Info
    // ─────────────────────────────────────────────
    private int  currentPage;          // 0-based page number
    private int  totalPages;           // total number of pages
    private long totalElements;        // total number of files
    private int  pageSize;             // items per page
    private boolean isFirst;           // is this the first page
    private boolean isLast;            // is this the last page
    private boolean hasNext;           // is there a next page
    private boolean hasPrevious;       // is there a previous page

    // ─────────────────────────────────────────────
    // Sort Info
    // ─────────────────────────────────────────────
    private String sortBy;             // which field sorted on
    private String sortDir;            // asc or desc
}