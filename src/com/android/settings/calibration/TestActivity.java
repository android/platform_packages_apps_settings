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

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

/**
 * The TestActivity provides the user with the opportunity to test a new
 * calibration out before deciding if it should be saved or not. The
 * calibration details are passed in via intent, and the TestActivity
 * will set a 'RESULT_CANCELED' or 'RESULT_OK' prior to finishing depending
 * on the user's actions.
 */
public class TestActivity extends Activity implements OnTouchListener {

    public static final String EXTRA_CALIBRATION = "com.android.settings.calibration.CALIBRATION";
    public static final String EXTRA_INPUTDEVICE = "com.android.settings.calibration.INPUTDEVICE";
    public static final String EXTRA_ORIENTATION = "com.android.settings.calibration.ORIENTATION";
    
    Transform calibrationCurrent, calibrationTesting;
    Transform calibrationIdentity = new Transform();
    InputDevice locked_device;
    int orientation;
    
    ImageView canvasView;
    Canvas canvas;
    Paint paint;
    PointF last;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity);
        
        Intent intent = getIntent();
        this.calibrationTesting = intent.getParcelableExtra(EXTRA_CALIBRATION);
        this.locked_device = intent.getParcelableExtra(EXTRA_INPUTDEVICE);
        this.orientation = intent.getIntExtra(EXTRA_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        Calibrator.lockOrientation(this, this.orientation);
        
        calibrationCurrent = Calibrator.getCalibration(this, this.locked_device, Calibrator.getRotation(this));
        
        canvasView = (ImageView)this.findViewById(R.id.testcanvas);
        canvasView.setOnTouchListener(this);
        
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }
    
    public void onClick(View view) {
        if (view == this.findViewById(R.id.cancel)) {
            Intent intent = new Intent(this, SampleActivity.class);
            intent.putExtra(SampleActivity.EXTRA_INPUTDEVICE, this.locked_device);
            intent.putExtra(SampleActivity.EXTRA_ORIENTATION, Calibrator.getCurrentOrientation(this));
            
            setResult(Activity.RESULT_CANCELED, intent);
            finish();
        }
        else if (view == this.findViewById(R.id.apply)) {
            if (this.calibrationTesting == null) {
                Log.w("TestActivity", "No calibration is available for saving");
            }
            else if (this.locked_device == null) {
                Log.w("TestActivity", "No device was set for applying calibration to");
            }
            else {
                Calibrator.setCalibration(this, this.locked_device, this.calibrationTesting, Calibrator.getRotation(this));
            }
            
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (!event.getDevice().getDescriptor().equals(locked_device.getDescriptor()))
            return false;
        
        if (canvas == null)
            setCanvas();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                last = new PointF(event.getX(), event.getY());
                break;
            
            case MotionEvent.ACTION_MOVE:
            	canvas.drawPath(buildPath(event, getTransform(v, event, calibrationTesting)), paint);
                last = new PointF(event.getX(), event.getY());
                break;
        }
        
        canvasView.invalidate();
        
        return true;
    }
    
    private void setCanvas() {
        Bitmap bitmap = Bitmap.createBitmap(canvasView.getWidth(), canvasView.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        canvasView.setImageBitmap(bitmap);
    }
    
    private Transform getTransform(View v, MotionEvent event, Transform desired) {
    	try {
	        Transform toView = new Transform(event, v).invert();
	        Transform currentTransform = calibrationCurrent.concat(toView);
	        Transform desiredTransform = desired.concat(toView);
	        return currentTransform.invert().concat(desiredTransform);
    	}
    	catch (NonInvertableException e) {
        	// If either the view or currentTransform are non-invertable,
    		// there's something seriously wrong with our view of the world
        	throw new RuntimeException("Unable to get a transform to the desired space", e);
        }
    }
    
    private Path buildPath(MotionEvent event, Transform t) {
        Path path = new Path();
        
        PointF l = t.transform(last);
        path.moveTo(l.x, l.y);
        
        float[] x = getAxisValues(event, MotionEvent.AXIS_X);
        float[] y = getAxisValues(event, MotionEvent.AXIS_Y);
        for (int i = 0; i <= event.getHistorySize(); i++) {
            PointF p = t.transform(new PointF(x[i], y[i]));
            path.lineTo(p.x, p.y);
        }
        
        return path;
    }
    
    private float[] getAxisValues(MotionEvent e, int axis) {
        final int histSize = e.getHistorySize();
        float[] values = new float[histSize + 1];
        
        for (int i = 0; i < histSize; i++) {
            values[i] = e.getHistoricalAxisValue(axis, i);
        }
        
        values[histSize] = e.getAxisValue(axis);
        return values;
    }

}
