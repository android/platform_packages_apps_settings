/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.ListActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.font.FontManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Display Font And Type Settings Class.
 */
public class DisplayFontTypeSettings extends ListActivity {

    /** Check Log. */
    private static final boolean DEBUG_LOG = true;

    /** Instance of ListView. */
    private ListView mListView;

    /** Instance of Typeface. */
    private Typeface mFace;

    /** Array of FontDisplaySettings. */
    private String[] mFontDisplayStrings;

    /** Array of FontNameSettings. */
    private String[] mFontNameStrings;

    /** Info of Fonts. */
    private FontManager.Font[] mFont;

    /** init FontType. */
    private int mFontType = 0;

    /**
     * Font settings to view related items.
     *
     * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
     * @param savedInstanceState Bundle
     */
    public final void onCreate(final Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Light);
        super.onCreate(savedInstanceState);

        getListView().setCacheColorHint(0);

        // Title of Activity
        setTitle(R.string.fontsettings_title);

        // All of default FontTypes
        mFont = FontManager.getSelectableDefaultFonts();

        // the array of default Fonts is null
        if (mFont.length <= 0) {
            showErrorDialog(R.string.common_db_getng, true);
        }

        // Remove unnecessary fonts from the list of areas
        mFontDisplayStrings = new String[mFont.length];
        mFontNameStrings = new String[mFont.length];

        int i = 0;
        int j = 0;
        for (;i < mFont.length; i++) {
            mFontDisplayStrings[j] = mFont[i].getDisplayName();
            mFontNameStrings[j] = mFont[i].getName();
            j++;
        }

        // set listAdapter
        setListAdapter(new ArrayAdapter < String > (this,
            com.android.internal.R.layout.simple_list_item_single_choice,
            mFontDisplayStrings));

        // get listView
        mListView = getListView();
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    /**
     * Click process for ListItem.
     * @param list List
     * @param v    View
     * @param position Position
     * @param id List ID
     *
     */
    @Override
    protected final void onListItemClick(final ListView list, final View v,
        final int position, final long id) {

        showPreviewDialog(position);
        super.onListItemClick(list, v, position, id);
    }

    /**
     * Show the preview dialog
     *
     * @see android.app.Activity#onResume()
     */
    private void showPreviewDialog(final int position) {
        final FontPreviewDialog dlg = new FontPreviewDialog(this);
        dlg.setPreviewText(mFont[position].getName(), mFont[position].getDisplayName());
        dlg.setButton(DialogInterface.BUTTON_POSITIVE, getText(R.string.common_dialog_ok),
             new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog,
                    final int which) {

                    // update the displayed font type
                    final Configuration config = getBaseContext().getResources().getConfiguration();
                    config.font = mFontNameStrings[position];
                    try {
                        ActivityManagerNative.getDefault().updateConfiguration(config);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    // get the resource
                    Resources r = getResources();
                    String states = r.getString(R.string.display_fonttype_text);

                    Toast.makeText(getBaseContext(), mFontDisplayStrings[position] + states, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        dlg.setButton(DialogInterface.BUTTON_NEGATIVE, getText(R.string.common_dialog_cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog,
                    final int which) {
                    mListView.setItemChecked(mFontType, true);
                }
            });
        dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface paramDialogInterface, int paramInt, KeyEvent paramKeyEvent) {
                switch(paramInt) {
                    case KeyEvent.KEYCODE_SEARCH:
                    case KeyEvent.KEYCODE_BACK:
                        if(dlg != null){
                            mListView.setItemChecked(mFontType, true);
                            dlg.dismiss();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        dlg.show();
    }

    /**
     * Update the displayed value of list
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected final void onResume() {
        super.onResume();

        // Get the current name of font type
        for (int i = 0; i < mFont.length; i++) {
            if (mFont[i].getName().equals(
                FontManager.getSelectedDefaultFontName())) {

                // the positon of radio button
                mListView.setItemChecked(i, true);

                // get the current position
                mFontType = i;
                break;
            }
        }

        // set the current position
        mListView.setSelection(mFontType);
    }

    /**
     * Show the error dialog.
     *
     * @param message Resource ID
     * @param check flag of Activity is finished
     */
    private void showErrorDialog(final int message, final boolean check) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.common_dialog_failed);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setMessage(message);
        builder.setPositiveButton(R.string.common_dialog_ok,
                new DialogInterface.OnClickListener() {

                public void onClick(final DialogInterface dialog,
                    final int which) {
                    if (check) {
                        finish();
                    }
                }
            });
        builder.show();
    }
}
