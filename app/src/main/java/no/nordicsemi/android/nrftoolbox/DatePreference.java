/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.nrftoolbox;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

/**
 * Adapted from: http://stackoverflow.com/a/5533295/4909414
 */
public class DatePreference extends DialogPreference {

    private static final String TAG = DatePreference.class.getName();

    private Calendar currentDate = Calendar.getInstance();

    private DatePicker picker = null;

    public DatePreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);

        setPositiveButtonText("Set");
        setNegativeButtonText("Cancel");
    }

    @Override
    protected View onCreateDialogView() {
        picker=new DatePicker(getContext());
        updatePicker();
        return(picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        updatePicker();
    }

    private void updatePicker() {
        picker.updateDate(currentDate.get(Calendar.YEAR), currentDate
                .get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, picker.getYear());
            cal.set(Calendar.DAY_OF_MONTH, picker.getDayOfMonth());
            cal.set(Calendar.MONTH, picker.getMonth());
            long millis = cal.getTimeInMillis();
            if (callChangeListener(millis)) {

                currentDate.set(Calendar.YEAR, picker.getYear());
                currentDate.set(Calendar.DAY_OF_MONTH, picker.getDayOfMonth());
                currentDate.set(Calendar.MONTH, picker.getMonth());

                // FIXME storing as long failed
                persistString("" + millis);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return(a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            if (defaultValue==null) {
                try {
                    long millis = Long.parseLong(getPersistedString("" + Calendar.getInstance().getTimeInMillis()));
                    currentDate.setTime(new Date(millis));
                }
                catch(NumberFormatException e)
                {
                    e.printStackTrace();
                }
            }
//            else {
//                currentDate.setTime(new Date(getPersistedLong((long) defaultValue)));
//            }
        }
        else {
            currentDate.setTime(new Date());
        }
    }
}