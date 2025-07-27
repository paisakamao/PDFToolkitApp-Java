package com.pdf.toolkit;

// This class simply holds the data for one file in your list.
public class FileItem {
    String name;
    long size;
    long date;
    String path; // The full path to the file

    public FileItem(String name, long size, long date, String path) {
        this.name = name;
        this.size = size;
        this.date = date;
        this.path = path;
    }
}