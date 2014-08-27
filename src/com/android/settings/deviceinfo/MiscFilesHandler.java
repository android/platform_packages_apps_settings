/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageMeasurement.FileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the selection and removal of Misc files.
 */
public class MiscFilesHandler extends ListActivity {
    private static final String TAG = "MemorySettings";
    private String mNumSelectedFormat;
    private String mNumBytesSelectedFormat;
    private MemoryMearurementAdapter mAdapter;
    private LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(true);
        setTitle(R.string.misc_files);
        mNumSelectedFormat = getString(R.string.misc_files_selected_count);
        mNumBytesSelectedFormat = getString(R.string.misc_files_selected_count_bytes);
        mAdapter = new MemoryMearurementAdapter(this);
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setContentView(R.layout.settings_storage_miscfiles_list);
        ListView lv = getListView();
        lv.setItemsCanFocus(true);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new ModeCallback(this));
        setListAdapter(mAdapter);
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {
        private int mDataCount;
        private final Context mContext;


        private final class ScannerClient implements MediaScannerConnectionClient {
            ArrayList<String> mPaths = new ArrayList<String>();
            private final Context mContext;
            private MediaScannerConnection mScannerConnection;
            boolean  mConnected;
            private Object mLock = new Object();

            public ScannerClient(Context context) {
                mContext = context;
                mScannerConnection = new MediaScannerConnection(context, this);
            }

            public void setScanPath(String path) {
                synchronized (mLock) {
                    if (mConnected) {
                        mScannerConnection.scanFile(path, null);
                    } else {
                        mPaths.add(path);
                        mScannerConnection.connect();
                    }
                }
            }

            @Override
            public void onMediaScannerConnected() {
                synchronized (mLock) {
                    mConnected = true;
                    if (!mPaths.isEmpty()) {
                        for (String path: mPaths) {
                            mScannerConnection.scanFile(path, null);
                        }
                    }
                }
             }

            public void disconnect() {
                if (mScannerConnection.isConnected()) {
                    mScannerConnection.disconnect();
                }
            }

            @Override
            public void onScanCompleted(String path, Uri uri) {
                int ret = mContext.getContentResolver().delete(uri, null, null);
                synchronized (mLock) {
                    if (mPaths != null) {
                        mPaths.remove(path);
                        if (mConnected && (mPaths.size() == 0)) {
                            mConnected = false;
                            disconnect();
                        }
                    }
                }
            }

        }

        //private MediaScannerConnection mScannerConnection;
        private ScannerClient mScannerClient;

        public ModeCallback(Context context) {
            mContext = context;
            mDataCount = mAdapter.getCount();
            mScannerClient = new ScannerClient(context);
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            final MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.misc_files_menu, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ListView lv = getListView();
            switch (item.getItemId()) {
            case R.id.action_delete:
                // delete the files selected
                SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
                int checkedCount = getListView().getCheckedItemCount();
                if (checkedCount > mDataCount) {
                    throw new IllegalStateException("checked item counts do not match. " +
                            "checkedCount: " + checkedCount + ", dataSize: " + mDataCount);
                }
                if (mDataCount > 0) {
                    ArrayList<Object> toRemove = new ArrayList<Object>();
                    String folderParentPath = null;
                    for (int i = 0; i < mDataCount; i++) {
                        if (!checkedItems.get(i)) {
                            //item not selected
                            continue;
                        }
                        if (StorageMeasurement.LOGV) {
                            Log.i(TAG, "deleting: " + mAdapter.getItem(i));
                        }
                        // delete the file
                        File file = new File(mAdapter.getItem(i).mFileName);
                        if (file.isDirectory()) {
                            deleteDir(file);
                            if (folderParentPath == null) {
                                /*
                                 * All the folders deleted by Misc app has the same
                                 * parent folder. So only need to remeber once for the
                                 * parent folder patch
                                 */
                                folderParentPath = file.getParent();
                            }
                        } else {
                            file.delete();
                            mScannerClient.setScanPath(file.getAbsolutePath());
                        }

                        toRemove.add(mAdapter.getItem(i));
                    }
                    if (folderParentPath != null) {
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                                        Uri.fromFile(new File(folderParentPath))));
                    }
                    mAdapter.removeAll(toRemove);
                    mAdapter.notifyDataSetChanged();
                    mDataCount = mAdapter.getCount();
                }
                mode.finish();
                break;

            case R.id.action_select_all:
                // check ALL items
                for (int i = 0; i < mDataCount; i++) {
                    lv.setItemChecked(i, true);
                }
                // update the title and subtitle with number selected and numberBytes selected
                onItemCheckedStateChanged(mode, 1, 0, true);
                break;
            }
            return true;
        }

        // Deletes all files and subdirectories under given dir.
        // Returns true if all deletions were successful.
        // If a deletion fails, the method stops attempting to delete and returns false.
        private boolean deleteDir(File dir) {
            String[] children = dir.list();
            if (children != null) {
                for (int i=0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
            // The directory is now empty so delete it
            return dir.delete();
        }

        public void onDestroyActionMode(ActionMode mode) {
            // This block intentionally left blank
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            ListView lv = getListView();
            int numChecked = lv.getCheckedItemCount();
            mode.setTitle(String.format(mNumSelectedFormat, numChecked, mAdapter.getCount()));

            // total the sizes of all items selected so far
            SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
            long selectedDataSize = 0;
            if (numChecked > 0) {
                for (int i = 0; i < mDataCount; i++) {
                    if (checkedItems.get(i)) {
                        // item is checked
                        selectedDataSize += mAdapter.getItem(i).mSize;
                    }
                }
            }
            mode.setSubtitle(String.format(mNumBytesSelectedFormat,
                    Formatter.formatFileSize(mContext, selectedDataSize),
                    Formatter.formatFileSize(mContext, mAdapter.getDataSize())));
        }
    }

    class MemoryMearurementAdapter extends BaseAdapter {
        private ArrayList<StorageMeasurement.FileInfo> mData = null;
        private long mDataSize = 0;
        private Context mContext;

        public MemoryMearurementAdapter(Activity activity) {
            mContext = activity;
            final StorageVolume storageVolume = activity.getIntent().getParcelableExtra(
                    StorageVolume.EXTRA_STORAGE_VOLUME);
            StorageMeasurement mMeasurement = StorageMeasurement.getInstance(
                    activity, storageVolume);
            if (mMeasurement == null) return;
            mData = (ArrayList<StorageMeasurement.FileInfo>) mMeasurement.mFileInfoForMisc;
            if (mData != null) {
                for (StorageMeasurement.FileInfo info : mData) {
                    mDataSize += info.mSize;
                }
            }
        }

        @Override
        public int getCount() {
            return (mData == null) ? 0 : mData.size();
        }

        @Override
        public StorageMeasurement.FileInfo getItem(int position) {
            if (mData == null || mData.size() <= position) {
                return null;
            }
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (mData == null || mData.size() <= position) {
                return 0;
            }
            return mData.get(position).mId;
        }

        public void removeAll(List<Object> objs) {
            if (mData == null) {
                return;
            }
            for (Object o : objs) {
                mData.remove(o);
                mDataSize -= ((StorageMeasurement.FileInfo) o).mSize;
            }
        }

        public long getDataSize() {
            return mDataSize;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final FileItemInfoLayout view = (convertView == null) ?
                    (FileItemInfoLayout) mInflater.inflate(R.layout.settings_storage_miscfiles,
                            parent, false) : (FileItemInfoLayout) convertView;
            FileInfo item = getItem(position);
            view.setFileName(item.mFileName);
            view.setFileSize(Formatter.formatFileSize(mContext, item.mSize));
            final ListView listView = (ListView) parent;
            final int listPosition = position;
            view.getCheckBox().setOnCheckedChangeListener(new OnCheckedChangeListener() {
                
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    listView.setItemChecked(listPosition, isChecked);
                }
                
            });
            view.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listView.getCheckedItemCount() > 0) {
                        return false;
                    }
                    listView.setItemChecked(listPosition, !view.isChecked());
                    return true;
                }
            });
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listView.getCheckedItemCount() > 0) {
                        listView.setItemChecked(listPosition, !view.isChecked());
                    }
                }
            });
            return view;
        }
    }
}
