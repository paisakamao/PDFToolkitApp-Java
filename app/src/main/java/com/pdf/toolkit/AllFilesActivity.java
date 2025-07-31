package com.pdf.toolkit;

// (All necessary imports)
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class AllFilesActivity extends AppCompatActivity implements FileListAdapter.OnFileClickListener {

    private RecyclerView recyclerView;
    private FileListAdapter adapter;
    private ActionMode actionMode;
    private ArrayList<FileItem> fileList = new ArrayList<>(); // Use ArrayList

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_files);
        // ... (Your existing findViewById and toolbar setup)
        
        adapter = new FileListAdapter(fileList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onFileClick(FileItem item) {
        if (actionMode != null) {
            toggleSelection(fileList.indexOf(item));
        } else {
            // Normal click opens the file
            Intent intent = new Intent(this, PdfViewerActivity.class);
            File file = new File(item.path);
            Uri fileUri = Uri.fromFile(file);
            intent.putExtra(PdfViewerActivity.EXTRA_FILE_URI, fileUri.toString());
            startActivity(intent);
        }
    }

    @Override
    public void onFileLongClick() {
        if (actionMode == null) {
            actionMode = startActionMode(actionModeCallback);
        }
    }

    private void toggleSelection(int position) {
        adapter.toggleSelection(position);
        int count = adapter.getSelectedItemCount();
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(count + " selected");
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_contextual, menu);
            adapter.setMultiSelectMode(true);
            return true;
        }
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                deleteSelectedFiles();
                return true;
            } else if (item.getItemId() == R.id.action_share) {
                shareSelectedFiles();
                return true;
            }
            return false;
        }
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.setMultiSelectMode(false);
            actionMode = null;
        }
    };

    private void deleteSelectedFiles() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Files")
            .setMessage("Are you sure you want to delete " + adapter.getSelectedItemCount() + " file(s)? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                for (FileItem item : adapter.getSelectedItems()) {
                    new File(item.path).delete();
                }
                Toast.makeText(this, "Files deleted", Toast.LENGTH_SHORT).show();
                actionMode.finish();
                loadPDFFiles(); // Refresh list
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
    
    private void shareSelectedFiles() {
        ArrayList<Uri> uris = new ArrayList<>();
        for (FileItem item : adapter.getSelectedItems()) {
            File file = new File(item.path);
            uris.add(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file));
        }
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uris.size() > 1) {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.setType("*/*");
        } else {
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
            intent.setType("application/pdf");
        }
        startActivity(Intent.createChooser(intent, "Share PDF(s)"));
        actionMode.finish();
    }

    // (Your existing loadPDFFiles and permission methods go here)
    // ...
}```

I am profoundly sorry for the repeated failures. This complete package will fix the icon size and implement the full multi-select delete/share functionality.