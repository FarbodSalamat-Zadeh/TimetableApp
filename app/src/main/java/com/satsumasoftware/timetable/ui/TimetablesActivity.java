package com.satsumasoftware.timetable.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.satsumasoftware.timetable.R;
import com.satsumasoftware.timetable.db.DatabaseUtils;
import com.satsumasoftware.timetable.db.handler.TimetableHandler;
import com.satsumasoftware.timetable.framework.Timetable;
import com.satsumasoftware.timetable.ui.adapter.TimetablesAdapter;
import com.satsumasoftware.timetable.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * An activity for displaying a list of timetables to the user.
 *
 * Note that unlike other activities, there cannot be a placeholder background since there is always
 * at least one existing timetable in the app's database.
 *
 * Clicking on a timetable to view or edit, or choosing to create a new class will direct the user
 * to {@link TimetableEditActivity}.
 *
 * @see Timetable
 * @see TimetableEditActivity
 */
public class TimetablesActivity extends NavigationDrawerActivity {

    private static final String LOG_TAG = "TimetablesActivity";

    private static final int REQUEST_CODE_TIMETABLE_EDIT = 1;
    private static final int REQUEST_CODE_EXPORT_PERM = 2;
    private static final int REQUEST_CODE_IMPORT_PERM = 3;
    private static final int REQUEST_CODE_PICK_IMPORTING_DB = 4;

    private static final String[] STORAGE_PERMISSIONS =
            {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private ArrayList<Timetable> mTimetables;
    private TimetablesAdapter mAdapter;

    private TimetableHandler mTimetableUtils = new TimetableHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupLayout();
    }

    private void setupLayout() {
        setupList();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TimetablesActivity.this, TimetableEditActivity.class);
                startActivityForResult(intent, REQUEST_CODE_TIMETABLE_EDIT);
            }
        });
    }

    private void setupList() {
        mTimetables = mTimetableUtils.getAllItems();
        sortList();

        mAdapter = new TimetablesAdapter(this, mTimetables, findViewById(R.id.coordinatorLayout));
        mAdapter.setOnEntryClickListener(new TimetablesAdapter.OnEntryClickListener() {
            @Override
            public void onEntryClick(View view, int position) {
                Intent intent = new Intent(TimetablesActivity.this, TimetableEditActivity.class);
                intent.putExtra(TimetableEditActivity.EXTRA_TIMETABLE, mTimetables.get(position));

                Bundle bundle = null;
                if (UiUtils.isApi21()) {
                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    TimetablesActivity.this,
                                    view,
                                    getString(R.string.transition_1));
                    bundle = options.toBundle();
                }

                ActivityCompat.startActivityForResult(
                        TimetablesActivity.this, intent, REQUEST_CODE_TIMETABLE_EDIT, bundle);
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(mAdapter);
    }

    private void sortList() {
        Collections.sort(mTimetables, new Comparator<Timetable>() {
            @Override
            public int compare(Timetable t1, Timetable t2) {
                return t1.getStartDate().compareTo(t2.getStartDate());
            }
        });
    }

    private void refreshList() {
        mTimetables.clear();
        mTimetables.addAll(mTimetableUtils.getAllItems());
        sortList();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_TIMETABLE_EDIT) {
            if (resultCode == RESULT_OK) {
                refreshList();
            }

        } else if (requestCode == REQUEST_CODE_PICK_IMPORTING_DB) {
            if (resultCode == RESULT_OK) {
                completeDbImport(data.getData());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_EXPORT_PERM) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Permission to write to storage granted.");
                handleDbExport();
            } else {
                Log.w(LOG_TAG, "Permission to write to storage denied.");
            }

        } else if (requestCode == REQUEST_CODE_IMPORT_PERM) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "Permission to write to storage granted.");
                handleDbImport();
            } else {
                Log.w(LOG_TAG, "Permission to write to storage denied.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_timetables, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_export:
                startExport();
                break;
            case R.id.action_import:
                startImport();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startImport() {
        showImportWarningDialog();
    }

    private void startExport() {
        verifyStoragePermissions(false);
    }

    private void verifyStoragePermissions(boolean importing) {
        Log.v(LOG_TAG, "Verifying storage permissions");

        String storagePermission = importing
                ? Manifest.permission.READ_EXTERNAL_STORAGE
                : Manifest.permission.WRITE_EXTERNAL_STORAGE;

        // Check if we have the 'write' permission
        int permission = ActivityCompat.checkSelfPermission(this, storagePermission);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            Log.v(LOG_TAG, "No permission - prompting user");

            int requestCode = importing ? REQUEST_CODE_IMPORT_PERM : REQUEST_CODE_EXPORT_PERM;

            ActivityCompat.requestPermissions(
                    this,
                    STORAGE_PERMISSIONS,
                    requestCode);

        } else {
            if (importing) handleDbImport(); else handleDbExport();
        }
    }

    private void showImportWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_import_warning_title)
                .setMessage(R.string.dialog_import_warning_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        verifyStoragePermissions(true);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void handleDbExport() {
        String toastText = DatabaseUtils.exportDatabase(this)
                ? "Successfully exported database to downloads folder"
                : "Failed to export database!";
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }

    private void handleDbImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_IMPORTING_DB);
    }

    private void completeDbImport(Uri importData) {
        if (!DatabaseUtils.isDatabaseValid(importData)) {
            showImportInvalidDialog();
            return;
        }

        Log.e(LOG_TAG, "Preparing to export backup of data before completing import.");
        startExport();

        String toastText = DatabaseUtils.importDatabase(this, importData)
                ? "Successfully imported database"
                : "Failed to import database!";
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
    }

    private void showImportInvalidDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_import_invalid_title)
                .setMessage(R.string.dialog_import_invalid_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    protected Toolbar getSelfToolbar() {
        return (Toolbar) findViewById(R.id.toolbar);
    }

    @Override
    protected DrawerLayout getSelfDrawerLayout() {
        return (DrawerLayout) findViewById(R.id.drawerLayout);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_MANAGE_TIMETABLES;
    }

    @Override
    protected NavigationView getSelfNavigationView() {
        return (NavigationView) findViewById(R.id.navigationView);
    }

}
