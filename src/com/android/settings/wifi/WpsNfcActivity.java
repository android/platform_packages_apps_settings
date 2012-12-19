package com.android.settings.wifi;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.PendingIntent;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.provider.Settings;


import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.util.Log;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.tech.Ndef;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.FormatException;
import java.io.IOException;
import java.nio.charset.Charset;
import android.view.WindowManager;
import android.view.Display;
import android.widget.Toast;

import com.android.settings.R;

public class WpsNfcActivity extends Activity {
    static final String TAG = "WpsNfcActivity";
    private byte[] mPayloadBuffer;
    private WifiManager mWifiManager;
    private NfcAdapter mNfcAdapter;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private PendingIntent mPendingIntent;
    private int mWpsNfcMethod;
    private WpsDialog mDialog;
    public static final String EXTRA_WPS_NFC_METHOD = "android.net.wifi.WpsNfcActivity.NfcMethod";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent launchIntent = getIntent();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!mNfcAdapter.isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
            builder.setTitle(R.string.wps_fail_title)
                   .setMessage(R.string.wps_fail_nfc_is_disabled)
                   .setCancelable(false)
                   .setPositiveButton(R.string.wps_go_to_set_nfc,
                       new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                               WpsNfcActivity.this.finish();
                           }
                       })
                   .setNegativeButton(R.string.dlg_cancel,
                       new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               WpsNfcActivity.this.finish();
                       }
                   });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }

        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        mWpsNfcMethod = launchIntent.getIntExtra(EXTRA_WPS_NFC_METHOD, -1);

        mPendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, this.getClass()), 0);

        mFilter = new IntentFilter();
        mFilter.addAction(WpsDialog.WPS_OOB_READY_ACTION);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WpsDialog.WPS_OOB_READY_ACTION.equals(action)) {
                    /* This intent is disposable, remove it now */
                    context.removeStickyBroadcast(intent);
                    try {
                        mNfcAdapter.enableForegroundDispatch((Activity)context,mPendingIntent,null,null);
                    } catch (IllegalStateException e) {
                        Log.i(TAG, "EnableForegroundDispatch fail! IllegalStateException");
                    }
                }
            }
        };

        WindowManager manager = getWindowManager();
        Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        if (mWpsNfcMethod != -1) {
            mPayloadBuffer = mWifiManager.wpsNfcTokenGen(mWpsNfcMethod);
            if (mPayloadBuffer == null) {
                Log.e(TAG,"Fail to get WPS OOB config!");
                return;
            }
            showDialog(mWpsNfcMethod);
            mDialog.getWindow().setLayout(2*width/3, height/2);
            return;
        }
        handleNewIntent(launchIntent);
    }

    public boolean writeWpsNfcToken(Ndef ndef) {
        if (mPayloadBuffer == null) {
            Log.e(TAG,"Payload is not ready!");
            return false;
        }

        NdefRecord r = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "application/vnd.wfa.wsc".
                getBytes(Charset.forName("US_ASCII")), null, mPayloadBuffer);
        NdefMessage m = new NdefMessage(r);
        if (ndef == null) {
            Log.i(TAG,"No Tag Detected");
            return false;
        }
        if (!ndef.isConnected()) {
            try {
                ndef.connect();
            }catch (IOException e){
                Log.e(TAG, "TAG connect fail!");
            }
        }

        try {
            ndef.writeNdefMessage(m);
        } catch (FormatException e1) {
            Log.e(TAG, "writeNdefMessage fail:FormatException");
            return false;
        } catch (IllegalStateException e2) {
            Log.e(TAG, "writeNdefMessage fail:IllegalStateException");
            return false;
        } catch (TagLostException e3) {
            Log.e(TAG, "writeNdefMessage fail:TagLostException");
            return false;
        } catch (IOException e4) {
            Log.e(TAG, "writeNdefMessage fail:IOException");
            return false;
        }
        return true;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        Intent launchIntent = getIntent();
        handleNewIntent(launchIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.registerReceiver(mReceiver, mFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            mNfcAdapter.disableForegroundDispatch(this);
        } catch (IllegalStateException e) {
            Log.i(TAG, "DisableForegroundDispatch fail: IllegalStateException");
        }
        this.unregisterReceiver(mReceiver);
    }

    private void handleNewIntent(Intent launchIntent) {
        int confirmStartWps = R.string.wifi_wps_tag_not_invalid;

        if (launchIntent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Tag tag = launchIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                Ndef ndef = Ndef.get(tag);
                confirmStartWps = R.string.wifi_wps_tag_not_writeable;
                if (ndef.isWritable()) {
                    if (writeWpsNfcToken(ndef)) {
                        confirmStartWps = R.string.wifi_wps_tag_write_success;
                    } else {
                        confirmStartWps = R.string.wifi_wps_tag_write_fail;
                    }
                }
            }
        }
        Toast.makeText(this, confirmStartWps, Toast.LENGTH_LONG).show();
        if (mWpsNfcMethod == WpsInfo.NFC_PWD &&
                confirmStartWps == R.string.wifi_wps_tag_write_success) {
            mDialog.updateMsg(this.getString(R.string.wifi_wps_oob_pwd_start));
        } else {
            if (mDialog != null) {
                mDialog.dismiss();
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        mDialog = new WpsDialog(this, id);
        return mDialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
            @Override
            public void onDismiss(DialogInterface dialog) {
                WpsNfcActivity.this.finish();
            }
        });
    }
}
