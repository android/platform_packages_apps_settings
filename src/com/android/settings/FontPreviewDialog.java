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

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
public class FontPreviewDialog extends AlertDialog {

    /** TextView. */
    private TextView mPreviewExplanation;
    /** TextView. */
    private TextView mPreviewText;

    protected FontPreviewDialog(Context context) {
        super(context);

        LayoutInflater inflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.font_preview, null);
        setView(view);

        mPreviewExplanation = (TextView) view.findViewById(R.id.previewExplanation);
        mPreviewText = (TextView) view.findViewById(R.id.previewText);

        mPreviewExplanation.setText(R.string.display_fonttype_explanation);
        mPreviewText.setText(R.string.display_fonttype_preview);
    }

    /**
     * @param font font
     * @param fontDisplay fontDisplay
     */
    public final void setPreviewText(final String font, final String fontDiaplay) {

        // init Typeface
        Typeface face = Typeface.create(font, Typeface.NORMAL);

        // set displayfont's typeface
        mPreviewText.setTypeface(face);

        // set display text
        mPreviewText.setText(R.string.display_fonttype_preview);

        // set title
        setTitle(fontDiaplay);
    }
}
