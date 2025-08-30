package com.pdfscanner.toolkit;

public class FileItem {
    public final String name;
    public final long size;
    public final long date; // Matches your existing "date" field name
    public final String path;

    public FileItem(String name, long size, long date, String path) {
        this.name = name;
        this.size = size;
        this.date = date;
        this.path = path;
    }
}
