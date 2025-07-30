package com.pdf.toolkit;

public class FileItem {
    public final String name;
    public final String path;
    public final long size;
    public final long lastModified; // This field was missing

    public FileItem(String name, String path, long size, long lastModified) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
    }
}