/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.network;

import static android.net.ConnectivityManager.PRIVATE_DNS_DEFAULT_MODE_FALLBACK;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PREDEFINED_PROVIDER;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.PrivateDnsProvider;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.CustomDialogPreferenceCompat;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.common.net.InternetDomainName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dialog to set the Private DNS
 */
public class PrivateDnsModeDialogPreference extends CustomDialogPreferenceCompat implements
        DialogInterface.OnClickListener, RadioGroup.OnCheckedChangeListener, TextWatcher {

    public static final String ANNOTATION_URL = "url";

    private static final String TAG = "PrivateDnsModeDialog";
    // DNS_MODE -> RadioButton id
    private static final Map<String, Integer> PRIVATE_DNS_MAP;

    static {
        PRIVATE_DNS_MAP = new HashMap<>();
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OFF, R.id.private_dns_mode_off);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_OPPORTUNISTIC, R.id.private_dns_mode_opportunistic);
        // TODO(huangluke): Have a proper mode to represent the customize option instead of keeping
        // current PRIVATE_DNS_MODE_PROVIDER_HOSTNAME since now the customize option might be a
        // https URL not only a hostname.
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME, R.id.private_dns_mode_provider);
        PRIVATE_DNS_MAP.put(PRIVATE_DNS_MODE_PREDEFINED_PROVIDER,
                R.id.private_dns_mode_predefined_provider);
    }

    @VisibleForTesting
    static final String MODE_KEY = Settings.Global.PRIVATE_DNS_MODE;
    @VisibleForTesting
    static final String CUSTOMIZATION_KEY = Settings.Global.PRIVATE_DNS_SPECIFIER;
    @VisibleForTesting
    static final String PREDEFINED_PROVIDER_KEY = Settings.Global.PRIVATE_DNS_PREDEFINED_PROVIDER;

    public static String getModeFromSettings(ContentResolver cr) {
        String mode = Settings.Global.getString(cr, MODE_KEY);
        if (!PRIVATE_DNS_MAP.containsKey(mode)) {
            mode = Settings.Global.getString(cr, Settings.Global.PRIVATE_DNS_DEFAULT_MODE);
        }
        return PRIVATE_DNS_MAP.containsKey(mode) ? mode : PRIVATE_DNS_DEFAULT_MODE_FALLBACK;
    }

    /**
     * Get the stored customization private DNS specifier from Settings.
     */
    public static String getCustomizationFromSettings(ContentResolver cr) {
        return Settings.Global.getString(cr, CUSTOMIZATION_KEY);
    }

    /**
     * Get the stored predefined private DNS provider from Settings.
     */
    public static String getPredefinedProviderFromSettings(ContentResolver cr) {
        return Settings.Global.getString(cr, PREDEFINED_PROVIDER_KEY);
    }

    @VisibleForTesting
    EditText mEditText;
    @VisibleForTesting
    RadioGroup mRadioGroup;
    @VisibleForTesting
    String mMode;
    @VisibleForTesting
    Spinner mSp;

    ConnectivityManager mCM;

    public PrivateDnsModeDialogPreference(Context context) {
        super(context);
        initialize(context);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public PrivateDnsModeDialogPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private final AnnotationSpan.LinkInfo mUrlLinkInfo = new AnnotationSpan.LinkInfo(
            ANNOTATION_URL, (widget) -> {
        final Context context = widget.getContext();
        final Intent intent = HelpUtils.getHelpIntent(context,
                context.getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        if (intent != null) {
            try {
                widget.startActivityForResult(intent, 0);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + intent.toString());
            }
        }
    });

    private void initialize(Context context) {
        // Add the "Restricted" icon resource so that if the preference is disabled by the
        // admin, an information button will be shown.
        setWidgetLayoutResource(R.layout.restricted_icon);
        mCM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (isDisabledByAdmin()) {
            // If the preference is disabled by the admin, set the inner item as enabled so
            // it could act as a click target. The preference itself will have been disabled
            // by the controller.
            holder.itemView.setEnabled(true);
        }

        final View restrictedIcon = holder.findViewById(R.id.restricted_icon);
        if (restrictedIcon != null) {
            // Show the "Restricted" icon if, and only if, the preference was disabled by
            // the admin.
            restrictedIcon.setVisibility(isDisabledByAdmin() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        final Context context = getContext();
        final ContentResolver contentResolver = context.getContentResolver();

        mMode = getModeFromSettings(context.getContentResolver());

        mEditText = view.findViewById(R.id.private_dns_mode_provider_hostname);
        mEditText.addTextChangedListener(this);
        mEditText.setText(getCustomizationFromSettings(contentResolver));

        mSp = view.findViewById(R.id.private_dns_provider_spinner);

        mRadioGroup = view.findViewById(R.id.private_dns_radio_group);
        mRadioGroup.setOnCheckedChangeListener(this);
        mRadioGroup.check(PRIVATE_DNS_MAP.getOrDefault(mMode, R.id.private_dns_mode_opportunistic));

        // Initial radio button text
        final RadioButton offRadioButton = view.findViewById(R.id.private_dns_mode_off);
        offRadioButton.setText(R.string.private_dns_mode_off);
        final RadioButton opportunisticRadioButton =
                view.findViewById(R.id.private_dns_mode_opportunistic);
        opportunisticRadioButton.setText(R.string.private_dns_mode_opportunistic);
        final RadioButton providerRadioButton = view.findViewById(R.id.private_dns_mode_provider);
        providerRadioButton.setText(R.string.private_dns_mode_provider);
        final RadioButton providersRadioButton =
                view.findViewById(R.id.private_dns_mode_predefined_provider);
        providersRadioButton.setText(R.string.private_dns_mode_predefined_provider);

        final TextView helpTextView = view.findViewById(R.id.private_dns_help_info);
        helpTextView.setMovementMethod(LinkMovementMethod.getInstance());
        final Intent helpIntent = HelpUtils.getHelpIntent(context,
                context.getString(R.string.help_uri_private_dns),
                context.getClass().getName());
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(context,
                ANNOTATION_URL, helpIntent);
        if (linkInfo.isActionable()) {
            helpTextView.setText(AnnotationSpan.linkify(
                    context.getText(R.string.private_dns_help_message), linkInfo));
        }
        // Get the predefined private DNS provider list and setup the spinner.
        final List<PrivateDnsProvider> privateDnsProviders = mCM.getPrivateDnsProviders();
        final String storedProvider = getPredefinedProviderFromSettings(contentResolver);
        // If there are no available predefined providers, and no stored provider,
        // the spinner shouldn't be visible.
        if (privateDnsProviders.isEmpty() && TextUtils.isEmpty(storedProvider)) {
            return;
        }
        final List<String> list =
                privateDnsProviders.stream().map(s -> s.name).collect(Collectors.toList());

        // If the system provider list doesn't contain the stored provider, just add it back.
        if (!list.contains(storedProvider)) {
            list.add(storedProvider);
        }
        providersRadioButton.setVisibility(View.VISIBLE);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(), android.R.layout.simple_spinner_item,
                list.toArray(new String[0]));
        mSp.setVisibility(View.VISIBLE);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSp.setAdapter(adapter);
        // Restore the selected item.
        mSp.setSelection(adapter.getPosition(storedProvider));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final Context context = getContext();
            // TODO(huangluke): Have a real time validation for the customize input to
            // ensure that the broken server won't be stored into Settings.
            if (mMode.equals(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME)) {
                // Only clickable if url/hostname is valid, so we could save it safely
                Settings.Global.putString(context.getContentResolver(), CUSTOMIZATION_KEY,
                        mEditText.getText().toString());
            }
            if (mMode.equals(PRIVATE_DNS_MODE_PREDEFINED_PROVIDER)) {
                Settings.Global.putString(context.getContentResolver(), PREDEFINED_PROVIDER_KEY,
                        mSp.getSelectedItem().toString());
            }

            FeatureFactory.getFactory(context).getMetricsFeatureProvider().action(context,
                    SettingsEnums.ACTION_PRIVATE_DNS_MODE, mMode);
            Settings.Global.putString(context.getContentResolver(), MODE_KEY, mMode);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.private_dns_mode_off) {
            mMode = PRIVATE_DNS_MODE_OFF;
        } else if (checkedId == R.id.private_dns_mode_opportunistic) {
            mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
        } else if (checkedId == R.id.private_dns_mode_provider) {
            mMode = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
        } else if (checkedId == R.id.private_dns_mode_predefined_provider) {
            mMode = PRIVATE_DNS_MODE_PREDEFINED_PROVIDER;
        }
        updateDialogInfo();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateDialogInfo();
    }

    @Override
    public void performClick() {
        EnforcedAdmin enforcedAdmin = getEnforcedAdmin();

        if (enforcedAdmin == null) {
            // If the restriction is not restricted by admin, continue as usual.
            super.performClick();
        } else {
            // Show a dialog explaining to the user why they cannot change the preference.
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), enforcedAdmin);
        }
    }

    private EnforcedAdmin getEnforcedAdmin() {
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getContext(), UserManager.DISALLOW_CONFIG_PRIVATE_DNS, UserHandle.myUserId());
    }

    private boolean isDisabledByAdmin() {
        return getEnforcedAdmin() != null;
    }

    private Button getSaveButton() {
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) {
            return null;
        }
        return dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    }

    private void updateDialogInfo() {
        final boolean modeProvider = PRIVATE_DNS_MODE_PROVIDER_HOSTNAME.equals(mMode);
        if (mEditText != null) {
            mEditText.setEnabled(modeProvider);
        }
        final boolean modeSpinner = PRIVATE_DNS_MODE_PREDEFINED_PROVIDER.equals(mMode);
        if (mSp != null) {
            mSp.setEnabled(modeSpinner);
        }
        final Button saveButton = getSaveButton();
        if (saveButton != null) {
            final String input = mEditText.getText().toString();
            saveButton.setEnabled(modeProvider
                    ? isValidHttpsUrl(input) || InternetDomainName.isValid(input)
                    : true);
        }
    }

    private boolean isValidHttpsUrl(String input) {
        return URLUtil.isHttpsUrl(input) && Patterns.WEB_URL.matcher(input).matches();
    }
}
