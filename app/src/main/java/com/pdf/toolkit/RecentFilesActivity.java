package com.pdf.toolkit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecentFilesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private final List<FileItem> recentFileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_files);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Recent Files");
        }

        recyclerView = findViewById(R.id.recycler_view_recent_files);
        emptyView = findViewById(R.id.text_empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        FileListAdapter adapter = new FileListAdapter(recentFileList, this::openFile);
        recyclerView.setAdapter(adapter);

        loadRecentFiles();
    }

    private void loadRecentFiles() {
        recentFileList.clear();

        // Open our "notebook"
        SharedPreferences prefs = getSharedPreferences("RecentFiles", MODE_PRIVATE);
        // Get the saved list of file paths
        Set<String> paths = prefs.getStringSet("files", new HashSet<>());

        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) { // "Reality check" to make sure the file wasn't deleted
                recentFileList.add(new FileItem(file.getName(), file.length(), file.lastModified(), file.getAbsolutePath()));
            }
        }
        
        // Sort the list by date, newest first
        Collections.sort(recentFileList, (f1, f2) -> Long.compare(f2.date, f1.date));
        
        // Show a message if the list is empty
        if (recentFileList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void openFile(FileItem item) {
        if (item.name.toLowerCase().endsWith(".pdf")) {
            Intent intent = new Intent(this, PdfViewerActivity.class);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_NAME, item.path);
            startActivity(intent);
        } else {
            // This part is for opening other file types, which you might add later
            Toast.makeText(this, "This file type can't be opened from here.", Toast.LENGTH_SHORT).show();
        }
    }
}
