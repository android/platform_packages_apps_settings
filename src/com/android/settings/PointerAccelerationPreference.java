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

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

public class PointerAccelerationPreference extends SeekBarDialogPreference implements
        SeekBar.OnSeekBarChangeListener {
    private final InputManager mIm;
    private SeekBar mSeekBar;

    private int mOldAcceleration;
    private boolean mRestoredOldState;

    private boolean mTouchInProgress;

    private ContentObserver mAccelerationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onAccelerationChanged();
        }
    };

    public PointerAccelerationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIm = (InputManager)getContext().getSystemService(Context.INPUT_SERVICE);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.POINTER_ACCELERATION), true,
                mAccelerationObserver);

        mRestoredOldState = false;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(InputManager.MAX_POINTER_ACCELERATION - InputManager.MIN_POINTER_ACCELERATION);
        mOldAcceleration = mIm.getPointerAcceleration(getContext());
        mSeekBar.setProgress(mOldAcceleration - InputManager.MIN_POINTER_ACCELERATION);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if (!mTouchInProgress) {
            mIm.tryPointerAcceleration(progress + InputManager.MIN_POINTER_ACCELERATION);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        mTouchInProgress = true;
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        mTouchInProgress = false;
        mIm.tryPointerAcceleration(seekBar.getProgress() + InputManager.MIN_POINTER_ACCELERATION);
    }

    private void onAccelerationChanged() {
        int acceleration = mIm.getPointerAcceleration(getContext());
        mSeekBar.setProgress(acceleration - InputManager.MIN_POINTER_ACCELERATION);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final ContentResolver resolver = getContext().getContentResolver();

        if (positiveResult) {
            mIm.setPointerAcceleration(getContext(),
                    mSeekBar.getProgress() + InputManager.MIN_POINTER_ACCELERATION);
        } else {
            restoreOldState();
        }

        resolver.unregisterContentObserver(mAccelerationObserver);
    }

    private void restoreOldState() {
        if (mRestoredOldState) return;

        mIm.tryPointerAcceleration(mOldAcceleration);
        mRestoredOldState = true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.progress = mSeekBar.getProgress();
        myState.oldAcceleration = mOldAcceleration;

        // Restore the old state when the activity or dialog is being paused
        restoreOldState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mOldAcceleration = myState.oldAcceleration;
        mSeekBar.setProgress(myState.progress);
        mIm.tryPointerAcceleration(myState.progress + InputManager.MIN_POINTER_ACCELERATION);
    }

    private static class SavedState extends BaseSavedState {
        int progress;
        int oldAcceleration;

        public SavedState(Parcel source) {
            super(source);
            progress = source.readInt();
            oldAcceleration = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(progress);
            dest.writeInt(oldAcceleration);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}

