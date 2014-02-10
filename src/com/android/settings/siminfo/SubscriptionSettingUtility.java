package com.android.settings.siminfo;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SubscriptionController;
import android.telephony.SubscriptionController.SubInfoRecord;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import java.util.List;

public class SubscriptionSettingUtility {
    
    private static final String TAG = "SubscriptionSettingUtility";
    private static final String SELECT_SUB_ACT = "com.android.settings.siminfo.SELECT_SUB";
    private static final int REQUEST_CODE = 0;
    public static final String SUB_ID_STATE = "sub_id_state";
    public static final int INVALID_ID = -1;
    
    private Activity mActivity;
    private int mFragmentId;
    private long mSubId = INVALID_ID;

    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                int simId = intent.getIntExtra(PhoneConstants.SIM_ID_KEY, -1);
                handleSimStateChange(simId, state);
            }
        }
    };

    /**
     * Create A SubscriptionSettingUtility instance by Activity
     * @param Activity The activity to create by.
     * @return Return A SubscriptionSettingUtility instance
     */
    public static SubscriptionSettingUtility createUtilityByActivity(Activity activity) {
        return new SubscriptionSettingUtility(activity);
    }

    /**
     * Create A SubscriptionSettingUtility instance by Fragment 
     * @param Activity The fragment to create by.
     * @return Return A SubscriptionSettingUtility instance
     */
    public static SubscriptionSettingUtility createUtilityByFragment(Fragment fragment) {
        Activity activity = fragment.getActivity();
        int id = fragment.getId();
        return new SubscriptionSettingUtility(activity, id);
    }
    
    public static int getInsertSimNum(Context context) {
        TelephonyManager tele = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simCount = tele.getSimCount();
        int simNum = 0;
        for(int i = 0 ; i < simCount ; i ++ ) {
            if (tele.hasIccCard(i)) {
                simNum ++ ;
            }
        }
        return simNum;
    }
    
    private SubscriptionSettingUtility(Activity activity) {
        this(activity, -1);
    }

    private SubscriptionSettingUtility(Activity activity, int fragmentId) {
        mActivity = activity;
        mFragmentId = fragmentId;
        mIntentFilter = new IntentFilter(
                TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    }
    
    private void handleSimStateChange(int simId, String state) {
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(state) && 
            SubscriptionController.getSimId(mSubId) == simId) {
            mActivity.onBackPressed();
        }
    }

    public void registerReceiver() {
        mActivity.registerReceiver(mReceiver, mIntentFilter);
    }
    
    public void unregisterReceiver() {
        mActivity.unregisterReceiver(mReceiver);
    }

    /**
     * Initialize Sub id in SubscriptionSettingUtility.
     * First judge if it can get a valid Sub id in bundle.
     * It is used to handle activity or fragment recreate.
     * Second judge if it can get a valid Sub id in intent.
     * It is used to handle Sub id transferred by other activity or fragment.
     * Third start SubSelectSettings activity if there is more than one Subscription activated.
     * Use the Sub id if there is only one Subscription inserted.
     * @param bundle The bundle instance when activity or fragment created.
     * @param intent The intent form other activity or fragment. 
     */
    public void initSubscriptionId(Bundle bundle, Intent intent) {
        mSubId = getSubIdByBundle(bundle);
        if (mSubId == INVALID_ID) {
            mSubId = getSubIdByIntent(intent);
            if (mSubId == INVALID_ID) {
                List<SubInfoRecord> subInfoList = SubscriptionController.getActivatedSubInfoList(mActivity);
                if (subInfoList.size() > 1) {
                    launchActivity();
                } else if (subInfoList.size() == 1) {
                    mSubId = subInfoList.get(0).mSubId;
                }
            }
        }
    }

    /**
     * Handle result by activity request REQUEST_CODE. 
     * If a sub id is return set the sub id here, else finish current 
     * activity or fragment.
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult(). 
     * @param data An Intent, which can return result data to the caller
     *             (various data can be attached to Intent "extras"). 
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mSubId = data.getLongExtra(PhoneConstants.SUB_ID_KEY,INVALID_ID);
            } else {
                mActivity.onBackPressed();
            }
        } 
    }

    // Launch an activity to select subscription
    private void launchActivity() {
        Intent intent = new Intent(SELECT_SUB_ACT);
        intent.putExtra(Intent.EXTRA_TITLE, mActivity.getTitle().toString());
        if (mFragmentId == -1) {
            mActivity.startActivityForResult(intent, REQUEST_CODE);
        } else {
            Fragment fragment = mActivity.getFragmentManager().findFragmentById(mFragmentId);
            mActivity.startActivityFromFragment(fragment, intent, REQUEST_CODE);
        }
    }

    /**
     *  Save Subscription id when activity or fragment recreate. 
     *  @param outState The bundle instance for Sub id save. 
     */
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(SUB_ID_STATE, mSubId);
    }

    /**
     *  Set SUB id. 
     *  @param subId The Subscription id to set. 
     */
    public void setSubId(long subId) {
        mSubId = subId;
    }

    /**
     *  Get SUB id. 
     *  @return subId The Subscription id to get. 
     */
    public long getSubId() {
        return mSubId;
    }
    
    /**
     *  Get SIM id. 
     *  @return simId The SIM id to get. 
     */
    public int getSimId() {
        return SubscriptionController.getSimId(mSubId);
    }

    /**
     *  Judge if the Sub id is valid. If the Sub Id is not inserted
     *  then it is a invalid Sub id. 
     *  @return true Sub id is valid. 
     *          false Sub id is invalid. 
     */
    public boolean isValidSubId() {
        return mSubId != INVALID_ID;
    }

    /**
     * Get sub Id from intent with extra @PhoneConstants.SUB_ID_KEY
     * @param intent The intent that Sub id is gotten from
     * @return The Sub Id or -1
     */
    public long getSubIdByIntent(Intent intent) {
        if (intent != null) {
            return intent.getLongExtra(PhoneConstants.SUB_ID_KEY, INVALID_ID);
        }
        return INVALID_ID;
    }

    /**
     * Get the sub Id from bundle
     * @param bundle The bundle that Sub id is gotten from
     * @return Id from bundle or -1
     */
    public long getSubIdByBundle(Bundle bundle) {
        if (bundle != null && bundle.containsKey(SUB_ID_STATE)) {
            return bundle.getLong(SUB_ID_STATE);
        }
        return INVALID_ID;
    }
}
