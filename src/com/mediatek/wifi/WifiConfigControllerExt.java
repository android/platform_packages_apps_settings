package com.mediatek.wifi;

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import android.app.AlertDialog;
import android.content.Context;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiConfiguration.Protocol;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settings.wifi.WifiConfigController;
import com.android.settings.wifi.WifiConfigUiBase;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class WifiConfigControllerExt {
    private static final String TAG = "WifiConfigControllerExt";

    //sim/aka
    public static final int WIFI_EAP_METHOD_SIM = 4;
    public static final int WIFI_EAP_METHOD_AKA = 5;
    public static final int WIFI_EAP_METHOD_AKA_PLUS = 6;
    private static final String SIM_STRING = "SIM";
    private static final String AKA_STRING = "AKA";
    private static final String AKA_PLUS_STRING = "AKA\'";
    public static final int WIFI_EAP_METHOD_DUAL_SIM = 2;

    //add for EAP_SIM/AKA
    private Spinner mSimSlot;
    private TelephonyManager mTelephonyManager;

    private Context mContext;
    private View mView;
    private WifiConfigUiBase mConfigUi;
    private WifiConfigController mController;

    public WifiConfigControllerExt(WifiConfigController controller,
            WifiConfigUiBase configUi, View view) {
        mController = controller;
        mConfigUi = configUi;
        mContext = mConfigUi.getContext();
        mView = view;
        // get telephonyManager
        mTelephonyManager = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void addViews(WifiConfigUiBase configUi, String security) {
        ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);
        //add security information
        View row = configUi.getLayoutInflater().inflate(
                    R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(
                    configUi.getContext().getString(R.string.wifi_security));
        ((TextView) row.findViewById(R.id.value)).setText(security);
        group.addView(row);
    }

    /**
     *add quote for strings
     * @param string
     * @return add quote to the string
     */
    public static String addQuote(int s) {
          return "\"" + s + "\"";
    }

    public void setConfig(WifiConfiguration config,
            int accessPointSecurity, TextView passwordView,
            Spinner eapMethodSpinner) {
        switch (accessPointSecurity) {
            case AccessPoint.SECURITY_EAP:
                config.simSlot = addQuote(-1);
                Log.d(TAG, "(String) eapMethodSpinner.getSelectedItem()="
                    + (String) eapMethodSpinner.getSelectedItem());
                if (AKA_STRING.equals((String) eapMethodSpinner.getSelectedItem())
                    || SIM_STRING.equals((String) eapMethodSpinner.getSelectedItem())
                    || AKA_PLUS_STRING.equals((String) eapMethodSpinner.getSelectedItem())) {
                    eapSimAkaSimSlotConfig(config, eapMethodSpinner);
                    Log.d(TAG, "eap-sim/aka, config.toString(): "
                        + config.toString());
                }
                break;
            default:
                  break;
        }
    }

    /**
     * Geminu plus
     */
     private void eapSimAkaSimSlotConfig(WifiConfiguration config,
             Spinner eapMethodSpinner) {
       if (mSimSlot == null) {
           Log.d(TAG, "mSimSlot is null");
           mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
       }
       String strSimAka = (String) eapMethodSpinner.getSelectedItem();
       if (TelephonyManager.getDefault().getPhoneCount()
           == WIFI_EAP_METHOD_DUAL_SIM) {
           Log.d(TAG, "((String) mSimSlot.getSelectedItem()) "
               + ((String) mSimSlot.getSelectedItem()));
           simSlotConfig(config, strSimAka);
           Log.d(TAG, "eap-sim, choose sim_slot"
               + (String) mSimSlot.getSelectedItem());
       }
       Log.d(TAG, "eap-sim, config.simSlot: " + config.simSlot);
   }

   /**
    *  Geminu plus
    */
   private void simSlotConfig(WifiConfiguration config, String strSimAka) {
       int simSlot = mSimSlot.getSelectedItemPosition() - 1;
       if (simSlot > -1) {
           config.simSlot = addQuote(simSlot);
           Log.d(TAG, "config.simSlot " + addQuote(simSlot));
       }
   }

   /**
    *  Geminu plus
    */
   public void setGEMINI(int eapMethod) {
       Spinner eapMethodSpinner = (Spinner) mView.findViewById(R.id.method);

       if (eapMethod == WIFI_EAP_METHOD_SIM
               || eapMethod == WIFI_EAP_METHOD_AKA
               || eapMethod == WIFI_EAP_METHOD_AKA_PLUS) {
           if (TelephonyManager.getDefault().getPhoneCount()
               == WIFI_EAP_METHOD_DUAL_SIM) {
               mView.findViewById(R.id.sim_slot_fields).
                   setVisibility(View.VISIBLE);
               mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
               //Geminu plus
               Context context = mConfigUi.getContext();
               String[] tempSimAkaMethods = context.getResources().
                   getStringArray(R.array.sim_slot);
               int sum = mTelephonyManager.getSimCount();
               Log.d(TAG, "the num of sim slot is :" + sum);
               String[] simAkaMethods = new String[sum + 1];
               for (int i = 0; i < (sum + 1); i++) {
                   if (i < tempSimAkaMethods.length) {
                       simAkaMethods[i] = tempSimAkaMethods[i];
                   } else {
                       simAkaMethods[i] = tempSimAkaMethods[1].
                           replaceAll("1", "" + i);
                   }
               }
               final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                   context, android.R.layout.simple_spinner_item, simAkaMethods);
               adapter.setDropDownViewResource(
                   android.R.layout.simple_spinner_dropdown_item);
               mSimSlot.setAdapter(adapter);

               //setting had selected simslot
               if (mController.getAccessPoint() != null
                   && mController.getAccessPoint().isSaved()) {
                   WifiConfiguration config = mController.getAccessPointConfig();
                   if (config != null && config.simSlot != null) {
                       String simSlot = config.simSlot.replace("\"", "");
                       if (!simSlot.isEmpty()) {
                           mSimSlot.setSelection(Integer.parseInt(simSlot) + 1);
                       }
                   }
               }
           }
       } else {
           if (TelephonyManager.getDefault().getPhoneCount()
               == WIFI_EAP_METHOD_DUAL_SIM) {
               mView.findViewById(R.id.sim_slot_fields).setVisibility(
                           View.GONE);
           }
       }
   }
}
