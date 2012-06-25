package com.android.settings;

import com.android.settings.LocaleListFragment.LocaleInfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** @hide */
public final class LocaleAdapter extends BaseAdapter {

    public static final int INVALID_POSITION = -1;

    private final ArrayList<LocaleInfo> mLocales;

    private LayoutInflater mInflater;

    public LocaleAdapter(Context context, List<LocaleInfo> locales) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLocales = new ArrayList<LocaleInfo>();
        for (LocaleInfo l : locales) {
            mLocales.add(l);
        }
    }

    public int getCount() {
        return mLocales.size();
    }

    public LocaleInfo getItem(int position) {
        return mLocales.get(position);
    }

    public Locale getLocale(int position) {
        return getItem(position).getLocale();
    }

    /**
     * Get the position of a Locale
     *
     * @param locale the Locale to get the position of in the array
     * @return the position or INVALID_POSITION if locale is null or
     * locale can not be found.
     */
    public int getPosition(Locale locale) {
        int position = 0;
        if (locale == null) return INVALID_POSITION;
        for (LocaleInfo l : mLocales) {
            if (locale.equals(l.getLocale())) {
                return position;
            }
            position++;
        }
        return INVALID_POSITION;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean areAllItemsEnabled() {
        // The divider item between the "top languages" and the rest of the
        // languages is not clickable, hence return false.
        return false;
    }

    public boolean isEnabled(int position) {
        return mLocales.get(position).getEnabled() ? super.isEnabled(position) : false;
    }

    public int getViewTypeCount() {
        return LocaleInfo.NBR_OF_TYPES;
    }

    public int getItemViewType(int position) {
        return mLocales.get(position).getType();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LocaleInfo record = mLocales.get(position);
        if (convertView == null) {
            convertView = mInflater.inflate(record.getResourceId(), parent, false);
        }
        switch (record.getType()) {
            case LocaleInfo.LOCALE_DIVIDER:
                convertView.setEnabled(isEnabled(position));
                break;
            case LocaleInfo.LOCALE_ITEM:
                CheckedTextView v = (CheckedTextView)convertView;
                v.setText(record.getLabel());
                v.setEnabled(isEnabled(position));
                break;
            default:
        }
        return convertView;
    }
}

