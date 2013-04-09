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

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.graphics.Matrix;
import android.hardware.input.InputManager;
import android.hardware.input.TouchCalibration;

import java.util.List;

/**
 * Provides a common set of methods to be used by Activities that are
 * involved in the pen calibration process.
 */
public class Calibrator {
	
    /**
     * Determine if the given InputDevice can be calibrated. This can be
     * used to filter out devices whose input should not be considered
     * as a candidate for calibration, whether because they are missing
     * the necessary axes, aren't direct-input devices, etc.
     * 
     * @param dev  the device to check for calibration prerequisites
     * @return     'false' if the device cannot be calibrated, 'true' otherwise
     */
    public static boolean canCalibrate(InputDevice dev) {
        InputDevice.MotionRange x = dev.getMotionRange(MotionEvent.AXIS_X);
        InputDevice.MotionRange y = dev.getMotionRange(MotionEvent.AXIS_Y);
        if (x == null || y == null) {
            Log.v("Calibrator", "Cannot calibrate " + dev.getName() + " because it is missing an X or Y axis.");
            return false;
        }
        
        if ((dev.getSources() & InputDevice.SOURCE_TOUCHSCREEN) != InputDevice.SOURCE_TOUCHSCREEN) {
            Log.v("Calibrator", "Cannot calibrate " + dev.getName() + " because it is not a direct input device.");
            return false;
        }
        if ((dev.getSources() & InputDevice.SOURCE_STYLUS) != InputDevice.SOURCE_STYLUS) {
            Log.v("Calibrator", "Cannot calibrate " + dev.getName() + " because it is not a stylus device.");
            return false;
        }
        if (dev.isVirtual()) {
            Log.v("Calibrator", "Cannot calibrate " + dev.getName() + " because it is virtual.");
            return false;
        }
        
        Log.v("Calibrator", "Assuming " + dev.getName() + " can be calibrated.");
        return true;
    }
    
    /**
     * Get a list of all currently attached input devices which are
     * capable of being calibrated. 
     * 
     * @return an array of input devices that can be calibrated
     */
    public static InputDevice[] getDevices() {
        List<InputDevice> l = new java.util.LinkedList<InputDevice>();
        
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice dev = InputDevice.getDevice(id);
            if (canCalibrate(dev))
                l.add(dev);
        }
        
        InputDevice[] array = new InputDevice[l.size()];
        l.toArray(array);
        return array;
    }
    
    /**
     * Get a Transform which represents the calibration currently set for
     * a given device and rotation. The returned Transform will take raw
     * hardware coordinates and return calibrated hardware coordinates.
     * If no calibration exits, an identity transformation will be returned. 
     * 
     * @param ctx              A context which can be used to gain access
     *                         to the input service.
     * @param device           The input device whose calibration should
     *                         be obtained.
     * @param surfaceRotation  The rotation that the calibration takes
     *                         effect with.
     * @return                 The Transform from raw hardware coordinates
     *                         to calibrated coordinates which is currently
     *                         being applied by the system. 
     */
    static Transform getCalibration(Context ctx, InputDevice device, int surfaceRotation) {
        InputManager manager = (InputManager)ctx.getSystemService(Context.INPUT_SERVICE);
        TouchCalibration systemCalibration = manager.getTouchCalibration(device.getDescriptor(), surfaceRotation);
        if (systemCalibration == null)
            return new Transform();
        
        float[] matrix = new float[]{1,0,0, 0,1,0, 0,0,1};
        System.arraycopy(systemCalibration.getAffineTransform(), 0, matrix, 0, 6);
        Matrix m = new Matrix();
        m.setValues(matrix);
        return new Transform(m);
    }
    
    /**
     * Set the calibration to be used for the given input device and
     * rotation equal to the provided Transform.
     * 
     * @param ctx              A context which can be used to gain access
     *                         to the input service.
     * @param device           The input device whose calibration should
     *                         be updated.
     * @param calibration      The calibration which the system should apply
     *                         to raw hardware coordinates before any other
     *                         processing to obtain screen coordinates.
     * @param surfaceRotation  The rotation for which the calibration should
     *                         take effect. 
     */
    static void setCalibration(Context ctx, InputDevice device, Transform calibration, int surfaceRotation) {
        float[] matrix = new float[9];
        calibration.m.getValues(matrix);
        
        Log.v("Calibrator", "Setting affine transform = " + matrix.toString());
        
        TouchCalibration tc = new TouchCalibration(
                matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]
        );
        
        InputManager manager = (InputManager)ctx.getSystemService(Context.INPUT_SERVICE);
        manager.setTouchCalibration(device.getDescriptor(), surfaceRotation, tc);
    }

    /**
     * Returns the current orientation of an Activity under any Android
     * version. Note that pre-Gingerbread only support portrait and
     * landscape, and cannot distinguish between their reversed variants.
     * 
     * @param ctx  The context to perform the orientation check as
     * @return     The current physical device orientation as an ActivityInfo constant
     */
    static int getCurrentOrientation(Context ctx) {
        WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
        final int r = wm.getDefaultDisplay().getRotation();
        final int o = ctx.getResources().getConfiguration().orientation;
        
        if (o == Configuration.ORIENTATION_PORTRAIT) {
            if (r == Surface.ROTATION_0 || r == Surface.ROTATION_270) {
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            }
            else if (r == Surface.ROTATION_90 || r == Surface.ROTATION_180) {
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
            else {
                throw new IllegalStateException("Current rotation (" + r + ") is not understood.");
            }
        }
        else if (o == Configuration.ORIENTATION_LANDSCAPE) {
            if (r == Surface.ROTATION_0 || r == Surface.ROTATION_90) {
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
            else if (r == Surface.ROTATION_180 || r == Surface.ROTATION_270) {
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
            else {
                throw new IllegalStateException("Current rotation (" + r + ") is not understood.");
            }
        }
        else {
            throw new IllegalStateException("Current orientation (" + o + ") is not understood.");
        }
    }
    
    /**
     * Returns the current display rotation for an Activity. Note that
     * the display rotation defines how the display surface itself has
     * been rotated, while orientation defines how Android should respond
     * to accelerometer data.
     * 
     * @param ctx  The context to perform the rotation check as
     * @return     The current rotation of the device surface as a Surface constant 
     */
    static int getRotation(Context ctx) {
        WindowManager wm = (WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getRotation();
    }
    
    /**
     * Lock or unlock the given Activity to its current orientation. Note
     * that pre-Gingerbread, this may still result in the app rotation 180
     * degrees before locking. This is caused by these early versions'
     * inability to distinguish e.g. landscape from reverse-landscape.
     * 
     * @param activity  The activity that the lock should apply to
     * @param lock      'true' if the orientation should be locked, 'false' otherwise
     */
    static void lockCurrentOrientation(Activity activity, boolean lock) {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        
        if (lock) {
            orientation = getCurrentOrientation(activity);
        }
        lockOrientation(activity, orientation);
    }
    
    /**
     * Lock the given Activity to the specified orientation. This is
     * really nothing more than an alias to activity.setRequestedOrientation.
     * 
     * @param activity     The activity the lock should apply to
     * @param orientation  The specific orientation that should be used
     */
    static void lockOrientation(Activity activity, int orientation) {
        activity.setRequestedOrientation(orientation);
    }
}
