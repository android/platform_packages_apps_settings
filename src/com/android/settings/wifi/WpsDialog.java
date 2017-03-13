/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.settings.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Dialog to show WPS progress.
 */
public class WpsDialog extends AlertDialog {

    private final static String TAG = "WpsDialog";
    private static final String DIALOG_STATE = "android:dialogState";
    private static final String DIALOG_MSG_STRING = "android:dialogMsg";
    private static final String WPS_DIALOG_FRAGMENT = "android:wpsDialog:fragment";

    private View mView;
    private TextView mTextView;
    private ProgressBar mTimeoutBar;
    private ProgressBar mProgressBar;
    private Button mButton;
    private Timer mTimer;

    private static final int WPS_TIMEOUT_S = 120;

    private WifiManager mWifiManager;
    private WpsListener mWpsListener;
    private int mWpsSetup;

    private final IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    private Context mContext;
    private Handler mHandler = new Handler();
    private String mMsgString = "";

    private enum DialogState {
        WPS_INIT,
        WPS_START,
        WPS_COMPLETE,
        CONNECTED, //WPS + IP config is done
        WPS_FAILED
    }

    private WpsDialogFragment mWpsDialogFragment;

    DialogState mDialogState = DialogState.WPS_INIT;

    public static class WpsDialogFragment extends Fragment {
        private WpsListener mWpsListener;

        public void setWpsListener(WpsListener wpsListener) {
            mWpsListener = wpsListener;
        }

        public WpsListener getWpsListener() {
            return mWpsListener;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }

    class WpsListener extends WifiManager.WpsCallback {
        private WpsDialog mWpsDialog;
        private int mSavedReason = -1;

        public void setWpsDialog(WpsDialog wpsDialog) {
            mWpsDialog = wpsDialog;
            if (mWpsDialog != null && mSavedReason != -1) {
                onFailed(mSavedReason);
                mSavedReason = -1;
            }
        }

        @Override
        public void onStarted(String pin) {
            if (mWpsDialog == null) {
                // Unlikely since called right after the first dialog creation
                return;
            }
            // Use context from dialog since this fragment may not be attached yet
            Context context = mWpsDialog.mContext;
            if (pin != null) {
                mWpsDialog.updateDialog(DialogState.WPS_START, String.format(
                            context.getString(R.string.wifi_wps_onstart_pin), pin));
            } else {
                mWpsDialog.updateDialog(DialogState.WPS_START, context.getString(
                            R.string.wifi_wps_onstart_pbc));
            }
        }

        @Override
        public void onSucceeded() {
            if (mWpsDialog == null) {
                // Unlikely since called right after the first dialog creation
                return;
            }
            // Use context from dialog since this fragment may not be attached yet
            Context context = mWpsDialog.mContext;
            mWpsDialog.updateDialog(DialogState.WPS_COMPLETE,
                    context.getString(R.string.wifi_wps_complete));
        }

        @Override
        public void onFailed(int reason) {
            if (mWpsDialog == null) {
                // Save the reason and report when the dialog is shown again
                mSavedReason = reason;
                return;
            }
            // Use context from dialog since this fragment may not be attached yet
            Context context = mWpsDialog.mContext;
            String msg;
            switch (reason) {
                case WifiManager.WPS_TKIP_ONLY_PROHIBITED:
                    msg = context.getString(R.string.wifi_wps_failed_tkip);
                    break;
                case WifiManager.WPS_OVERLAP_ERROR:
                    msg = context.getString(R.string.wifi_wps_failed_overlap);
                    break;
                case WifiManager.WPS_WEP_PROHIBITED:
                    msg = context.getString(R.string.wifi_wps_failed_wep);
                    break;
                case WifiManager.WPS_AUTH_FAILURE:
                    msg = context.getString(R.string.wifi_wps_failed_auth);
                    break;
                case WifiManager.IN_PROGRESS:
                    msg = context.getString(R.string.wifi_wps_in_progress);
                    break;
                case WifiManager.WPS_TIMED_OUT:
                default:
                    msg = context.getString(R.string.wifi_wps_failed_generic);
                    break;
            }
            mWpsDialog.updateDialog(DialogState.WPS_FAILED, msg);
        }
    }

    public WpsDialog(Context context, int wpsSetup) {
        super(context);
        mContext = context;
        mWpsSetup = wpsSetup;

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };
        setCanceledOnTouchOutside(false);
    }

    @Override
    public Bundle onSaveInstanceState () {
        Bundle bundle  = super.onSaveInstanceState();
        bundle.putSerializable(DIALOG_STATE, mDialogState);
        bundle.putString(DIALOG_MSG_STRING, mMsgString.toString());
        ((Activity) mContext).getFragmentManager().putFragment(
                bundle, WPS_DIALOG_FRAGMENT, mWpsDialogFragment);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            super.onRestoreInstanceState(savedInstanceState);
            DialogState dialogState =
                (DialogState) savedInstanceState.getSerializable(DIALOG_STATE);
            String msg = savedInstanceState.getString(DIALOG_MSG_STRING);
            updateDialog(dialogState, msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.wifi_wps_dialog, null);

        mTextView = (TextView) mView.findViewById(R.id.wps_dialog_txt);
        mTextView.setText(R.string.wifi_wps_setup_msg);

        mTimeoutBar = ((ProgressBar) mView.findViewById(R.id.wps_timeout_bar));
        mTimeoutBar.setMax(WPS_TIMEOUT_S);
        mTimeoutBar.setProgress(0);

        mProgressBar = ((ProgressBar) mView.findViewById(R.id.wps_progress_bar));
        mProgressBar.setVisibility(View.GONE);

        mButton = ((Button) mView.findViewById(R.id.wps_dialog_btn));
        mButton.setText(R.string.wifi_cancel);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogState != DialogState.WPS_COMPLETE) {
                    mWifiManager.cancelWps(null);
                }
                if (mWpsDialogFragment != null) {
                    ((Activity) mContext).getFragmentManager().beginTransaction().remove(
                            mWpsDialogFragment).commit();
                }

                dismiss();
            }
        });

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        setView(mView);

        if (null != savedInstanceState
                && savedInstanceState.containsKey(DIALOG_STATE)
                && savedInstanceState.containsKey(DIALOG_MSG_STRING)
                && savedInstanceState.containsKey(WPS_DIALOG_FRAGMENT)) {
            mWpsDialogFragment =
                (WpsDialogFragment) ((Activity) mContext).getFragmentManager().getFragment(
                        savedInstanceState, WPS_DIALOG_FRAGMENT);
            mWpsListener = mWpsDialogFragment.getWpsListener();
            DialogState dialogState =
                (DialogState) savedInstanceState.getSerializable(DIALOG_STATE);
            updateDialog(dialogState, savedInstanceState.getString(DIALOG_MSG_STRING));
        }

        if (mWpsListener == null) {
            mWpsListener = new WpsListener();
            mWpsDialogFragment = new WpsDialogFragment();
            mWpsDialogFragment.setWpsListener(mWpsListener);
            ((Activity) mContext).getFragmentManager().beginTransaction().add(
                    mWpsDialogFragment, WPS_DIALOG_FRAGMENT).commit();
        }

        if (savedInstanceState == null) {
            WpsInfo wpsConfig = new WpsInfo();
            wpsConfig.setup = mWpsSetup;
            mWifiManager.startWps(wpsConfig, mWpsListener);
        }
        super.onCreate(savedInstanceState);
    }

    public void cancel() {
        super.cancel();
        if (mDialogState != DialogState.WPS_COMPLETE) {
            mWifiManager.cancelWps(null);
        }
        if (mWpsDialogFragment != null) {
            ((Activity) mContext).getFragmentManager().beginTransaction().remove(mWpsDialogFragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        if (DialogState.WPS_INIT == mDialogState || DialogState.WPS_START == mDialogState) {
            /*
             * increment timeout bar per second.
             */
            mTimer = new Timer(false);
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mTimeoutBar.incrementProgressBy(1);
                        }
                    });
                }
            }, 1000, 1000);
        }

        mContext.registerReceiver(mReceiver, mFilter);
        mWpsListener.setWpsDialog(this);
    }

    @Override
    protected void onStop() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
        mWpsListener.setWpsDialog(null);
    }

    private void updateDialog(final DialogState state, final String msg) {
        if (mDialogState.ordinal() >= state.ordinal()) {
            //ignore.
            return;
        }
        mDialogState = state;
        mMsgString = msg;

        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch(state) {
                        case WPS_COMPLETE:
                            mTimeoutBar.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.VISIBLE);
                            break;
                        case CONNECTED:
                        case WPS_FAILED:
                            mButton.setText(mContext.getString(R.string.dlg_ok));
                            mTimeoutBar.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.GONE);
                            if (mReceiver != null) {
                                mContext.unregisterReceiver(mReceiver);
                                mReceiver = null;
                            }
                            break;
                    }
                    mTextView.setText(msg);
                }
            });
   }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            final NetworkInfo.DetailedState state = info.getDetailedState();
            if (state == DetailedState.CONNECTED &&
                    mDialogState == DialogState.WPS_COMPLETE) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String msg = String.format(mContext.getString(
                            R.string.wifi_wps_connected), wifiInfo.getSSID());
                    updateDialog(DialogState.CONNECTED, msg);
                }
            }
        }
    }

}
