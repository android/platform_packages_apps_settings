/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.debug.AdbManager;
import android.debug.IAdbManager;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.wifi.qrcode.QrCamera;
import com.android.settings.wifi.qrcode.QrDecorateView;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

// test code
import com.android.settings.development.tests.WirelessDebuggingManager;
import com.android.settings.development.tests.Constants;

public class AdbQrcodeScannerFragment extends InstrumentedFragment implements
        SurfaceTextureListener,
        QrCamera.ScannerCallback {
    private final String TAG = this.getClass().getSimpleName();

    // UI components
    private TextView mTitle;
    private TextView mDescription;
    // Camera related stuff
    private QrCamera mCamera;
    private TextureView mTextureView;
    private QrDecorateView mDecorateView;
    private View mQrCameraView;
    private View mVerifyingView;

    private IAdbManager mAdbManager;

    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION.equals(action)) {
                Integer res = intent.getIntExtra(
                        AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_FAIL);
                if (res.equals(AdbManager.WIRELESS_STATUS_SUCCESS)) {
                    Intent i = new Intent();
                    i.putExtra(
                            WirelessDebugging.PAIRING_DEVICE_REQUEST_TYPE,
                            WirelessDebugging.SUCCESS_ACTION);
                    getActivity().setResult(Activity.RESULT_OK, i);
                    getActivity().finish();
                } else if (res.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
                    Intent i = new Intent();
                    i.putExtra(
                            WirelessDebugging.PAIRING_DEVICE_REQUEST_TYPE,
                            WirelessDebugging.FAIL_ACTION);
                    getActivity().setResult(Activity.RESULT_OK, i);
                    getActivity().finish();

                }
            } else if (AdbManager.WIRELESS_DEBUG_ENABLE_DISCOVER_ACTION.equals(action)) {
                Integer res = intent.getIntExtra(
                        AdbManager.WIRELESS_STATUS_EXTRA,
                        AdbManager.WIRELESS_STATUS_FAIL);
                if (res.equals(AdbManager.WIRELESS_STATUS_FAIL)) {
                    Log.e(TAG, "Unable to turn on adb wireless discovery");
                    Intent i = new Intent();
                    i.putExtra(
                            WirelessDebugging.PAIRING_DEVICE_REQUEST_TYPE,
                            WirelessDebugging.DISCOVERY_FAIL_ACTION);
                    getActivity().setResult(Activity.RESULT_OK, i);
                    getActivity().finish();
                }
            }
        }
    };

    public AdbQrcodeScannerFragment() {
        super();

        mIntentFilter = new IntentFilter(AdbManager.WIRELESS_DEBUG_PAIRING_RESULT_ACTION);
        mIntentFilter.addAction(AdbManager.WIRELESS_DEBUG_ENABLE_DISCOVER_ACTION);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView.setSurfaceTextureListener(this);
    }

    protected int getLayout() {
        return R.layout.adb_qrcode_scanner_fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        initView(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Constants.USE_SIMULATION) {
            mAdbManager = WirelessDebuggingManager.getInstance(getContext().getApplicationContext());
        } else {
            mAdbManager = IAdbManager.Stub.asInterface(ServiceManager.getService(Context.ADB_SERVICE));
        }
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        try {
            mAdbManager.setDiscoverable(AdbManager.WIRELESS_DEBUG_PAIR_MODE_QR, true);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to turn on discovery");
            getActivity().finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
        try {
            mAdbManager.setDiscoverable(AdbManager.WIRELESS_DEBUG_PAIR_MODE_QR, false);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to turn off discovery");
        }
    }

    private void initView(View view) {
        mTitle = view.findViewById(R.id.title);
        mDescription = view.findViewById(R.id.description);

        mTextureView = (TextureView) view.findViewById(R.id.preview_view);

        mDecorateView = (QrDecorateView) view.findViewById(R.id.decorate_view);

        mQrCameraView = view.findViewById(R.id.camera_layout);
        mVerifyingView = view.findViewById(R.id.verifying_layout);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }

    public void setDescription(String description) {
        mDescription.setText(description);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        initCamera(surface);
        if (Constants.USE_QRCODE_SIMULATOR) {
            WirelessDebuggingManager m = (WirelessDebuggingManager) mAdbManager;
            m.requestFakeQrcodeResult(getActivity(), this);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        destroyCamera();
        return true;
    }

    @Override
    public Size getViewSize() {
        return new Size(mTextureView.getWidth(), mTextureView.getHeight());
    }

    @Override
    public void setTransform(Matrix transform) {
        mTextureView.setTransform(transform);
    }

    @Override
    public Rect getFramePosition(Size previewSize, int cameraOrientation) {
        return new Rect(0,0, previewSize.getWidth(), previewSize.getHeight());
    }

    @Override
    public boolean isValid(String qrCode) {
        // We won't know if the qr code was valid until AdbManager tells us.
        // So just return false otherwise the camera will close.
        return false;
    }

    @Override
    public void handleSuccessfulResult(String qrCode) {
        destroyCamera();
        mDecorateView.setFocused(true);
        Log.i(TAG, "Got qrcode=" + qrCode);
        // Change to "checking qrcode" progress and wait for result from
        // AdbManager
        mQrCameraView.setVisibility(View.GONE);
        mVerifyingView.setVisibility(View.VISIBLE);
        try {
            mAdbManager.pairDevice(AdbManager.WIRELESS_DEBUG_PAIR_MODE_QR, AdbManager.WIRELESS_DEBUG_DEVICE_GUID_NONE, qrCode);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to request AdbManager to pair by QR code.");
            getActivity().finish();
        }
    }

    @Override
    public void handleCameraFailure() {
        destroyCamera();
    }

    private void initCamera(SurfaceTexture surface) {
        // Check if the camera has alread been created.
        if (mCamera == null) {
            mCamera = new QrCamera(getContext(), this);
            mCamera.start(surface);
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.stop();
            mCamera = null;
        }
    }
}
