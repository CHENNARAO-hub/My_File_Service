package com.enterprise.fileservice.exceptions;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException(String message) { super(message); }
}
