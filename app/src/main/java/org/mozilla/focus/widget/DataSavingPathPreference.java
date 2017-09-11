/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.NoRemovableStorageException;
import org.mozilla.focus.utils.StorageUtils;

public class DataSavingPathPreference extends ListPreference {
    private static final String LOG_TAG = "DataSavingPathPreference";

    private boolean hasRemovableStorage = false;

    public DataSavingPathPreference(Context context) {
        this(context, null);
    }

    public DataSavingPathPreference(Context context, AttributeSet attributes) {
        super(context, attributes);

    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();

        buildList();
        pingRemovableStorage();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // The superclass will take care of persistence.
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            persistString(getValue());
        }

    }

    @Override
    public CharSequence getSummary() {
        // design's spec, always show 'save to internal' if there is no removable storage
        if (!hasRemovableStorage) {
            return getContext().getResources().getString(R.string.setting_dialog_internal_storage);
        }

        if (TextUtils.isEmpty(getEntry())) {
            final String[] entries = getContext().getResources().getStringArray(R.array.data_saving_path_entries);
            setValueIndex(0);
            return entries[0];
        }

        return getEntry();
    }

    private void buildList() {
        final String[] entries = getContext().getResources().getStringArray(R.array.data_saving_path_entries);
        final String[] values = getContext().getResources().getStringArray(R.array.data_saving_path_values);

        setEntries(entries);
        setEntryValues(values);
    }

    private void pingRemovableStorage() {
        try {
            StorageUtils.getAppMediaDirOnRemovableStorage(getContext());
        } catch (NoRemovableStorageException e) {
            hasRemovableStorage = false;
        }

        super.setEnabled(hasRemovableStorage);

        // ensure Summary sync to current state
        super.notifyChanged();
    }
}
