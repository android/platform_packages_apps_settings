
package com.android.settings;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.SELinux;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class SELinuxManageBooleans extends ListFragment {
    static final String TAG = "SELinuxManageBooleans";

    private myBooleanAdapter mAdapter;

    private class myBooleanAdapter extends ArrayAdapter<String> {
        private final LayoutInflater mInflater;
        private String[] mBooleans;

        public myBooleanAdapter(Context context, int textViewResourceId, String[] items) {
            super(context, textViewResourceId, items);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mBooleans = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView  == null) {
                convertView = mInflater.inflate(R.layout.selinux_manage_booleans_item, parent, 
                                                false);
                holder = new ViewHolder();
                holder.tx = (TextView) convertView.findViewById(R.id.text);
                holder.cb = (CheckBox) convertView.findViewById(R.id.checkbox);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String name = mBooleans[position];
            holder.tx.setText(name);
            holder.cb.setChecked(SELinux.getBooleanValue(name));
            holder.cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        String name = (String)holder.tx.getText();
                        SELinux.setBooleanValue(name, isChecked);
                        holder.cb.setChecked(SELinux.getBooleanValue(name));
                    }
                });
            return convertView;
        }

        class ViewHolder {
            TextView tx;
            CheckBox cb;
        }

        private void SetSELinuxBoolean(String name) {
            Boolean value = SELinux.getBooleanValue(name);
            SELinux.setBooleanValue(name, !value);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new myBooleanAdapter(getActivity(), R.layout.selinux_manage_booleans, 
                                        SELinux.getBooleanNames());
        setListAdapter(mAdapter);
    }
}
