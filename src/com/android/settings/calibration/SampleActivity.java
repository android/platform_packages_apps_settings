/**
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.calibration;

import com.android.settings.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import java.util.Map;

/**
 * The SampleActivity provides the user with a set of targets to tap,
 * building up a map that describes the relationship between the ideal
 * target center points and the actual click points. When all targets
 * have been clicked, the map is used to generate a Transform which
 * can be applied by the system.
 */
public class SampleActivity extends Activity {

    public static final String EXTRA_INPUTDEVICE = "com.android.settings.calibration.INPUTDEVICE";
    public static final String EXTRA_ORIENTATION = "com.android.settings.calibration.ORIENTATION";
    public static final int REQUEST_CALIBRATION_TEST = 167241797;
    
    Map<PointF,PointF> map = new java.util.HashMap<PointF,PointF>();
    InputDevice locked_device;
    int remaining_targets;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_activity);
        
        handleIntent(getIntent());
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CALIBRATION_TEST) {
            if (resultCode == Activity.RESULT_CANCELED)
                handleIntent(data);
            else if (resultCode == Activity.RESULT_OK)
                finish();
            else
                finish();
        }
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null) {
            InputDevice device = intent.getParcelableExtra(EXTRA_INPUTDEVICE);
            int orientation = intent.getIntExtra(EXTRA_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            
            this.locked_device = device;
            Calibrator.lockOrientation(this, orientation);
        }
        else if (remaining_targets == 0 && map.size() == 0) {
            Calibrator.lockCurrentOrientation(this, false);
            chooseDevice();
        }
        
        resetCalibration();
    }
    
    private void chooseDevice() {
        final InputDevice[] devices = Calibrator.getDevices();
        
        if (devices.length == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.calibration_sample_nodevices_title);
            builder.setMessage(R.string.calibration_sample_nodevices_msg);
            builder.setPositiveButton(R.string.calibration_sample_nodevices_btn, new android.content.DialogInterface.OnClickListener() {
                
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.create().show();
        }else {
            this.locked_device = devices[0];
            return;
        }
    }
    
    private void resetCalibration() {
        Log.i("SampleActivity", "Resetting calibration");
        ((TextView)findViewById(R.id.instructionText)).setText(R.string.calibration_sample_instructions);
        
        map.clear();

        for (remaining_targets = 0; ; remaining_targets++) {
            String id = "target" + remaining_targets;
            int resId = getResources().getIdentifier(id, "id", getPackageName());
            View target = findViewById(resId);

            if (target == null)
                break;

            target.setEnabled(true);
            target.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) { return onTargetTouch(v, event); }
            });
        }
    }
    
    private void lockCalibration(MotionEvent event) {
        if (this.locked_device == null) {
            Calibrator.lockCurrentOrientation(this, true);
            this.locked_device = event.getDevice();
            
            Log.i("SampleActivity", "Locked to input device '" + this.locked_device.getName() + "'.");
        }
    }
    
    private boolean onTargetTouch(View v, MotionEvent event) {
        if (validTargetTouch(v, event)) {
            lockCalibration(event);
            v.setEnabled(false);
            addMapping(v, event);
            remaining_targets--;
        }
        
        Log.d("SampleActivity", remaining_targets + " calibration targets remain.");
        
        if (remaining_targets <= 0)
            complete();
        
        return false;
    }
    
    private boolean validTargetTouch(View v, MotionEvent event) {
        boolean valid = true;
        
        if (!v.isEnabled()) {
            Log.v("SampleActivity", "Invalid target touch: target disabled.");
            valid = false;
        }
        
        if (locked_device != null && !locked_device.getDescriptor().equals(event.getDevice().getDescriptor())) {
            Log.v("SampleActivity", "Invalid target touch: expected touch from '" + locked_device.getName() + "' [" + locked_device.getDescriptor() + "], not '" + event.getDevice().getName() + "' [" + event.getDevice().getDescriptor() + "]).");
            valid = false;
        }
        
        if ((event.getSource() & InputDevice.SOURCE_STYLUS) != InputDevice.SOURCE_STYLUS) {
            Log.v("SampleActivity", "Invalid target touch: event source is not a stylus.");
            valid = false;
        }
        
        Log.v("SampleActivity", "Target touch is " + (valid ? "" : "not") + " valid.");
        return valid;
    }
    
    private void complete() {
        ((TextView)findViewById(R.id.instructionText)).setText(R.string.calibration_sample_complete);
        
        Transform newCal = new Transform(map);
        int orientation = Calibrator.getCurrentOrientation(this);
        Log.i("SampleActivity", "Calibration for orientation " + orientation + " is " + newCal);
        
        Intent intent = new Intent(this, TestActivity.class);
        intent.putExtra(TestActivity.EXTRA_CALIBRATION, newCal);
        intent.putExtra(TestActivity.EXTRA_INPUTDEVICE, locked_device);
        intent.putExtra(TestActivity.EXTRA_ORIENTATION, orientation);
        startActivityForResult(intent, REQUEST_CALIBRATION_TEST);
    }
    
    private void addMapping(View target, MotionEvent event) {
        PointF touchPoint  = new PointF(event.getRawX(), event.getRawY());
        PointF targetPoint = getCenterPointAbsolute(target);
        
        Transform t = new Transform(event, this);
        touchPoint = t.transform(touchPoint);
        targetPoint = t.transform(targetPoint);
        
        try {
            Transform c = Calibrator.getCalibration(this, event.getDevice(), Calibrator.getRotation(this)).invert();
            touchPoint = c.transform(touchPoint);
        }
        catch (NonInvertableException e) {
            // This shouldn't ever happen, and there's no reasonable
            // way to gracefully recover if it does.
            throw new RuntimeException("Unable to invert the current calibration", e);
        }
        
        map.put(targetPoint, touchPoint);
    }
    
    private static PointF getCenterPointAbsolute(View v) {
        PointF target = new PointF(v.getWidth()/2, v.getHeight()/2);
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return new PointF(loc[0]+target.x, loc[1]+target.y);
    }
}
