package com.enterprise.fileservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FileAccessDeniedException extends RuntimeException {

    private static final String ERROR_CODE = "FILE_ACCESS_DENIED";

    public FileAccessDeniedException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}