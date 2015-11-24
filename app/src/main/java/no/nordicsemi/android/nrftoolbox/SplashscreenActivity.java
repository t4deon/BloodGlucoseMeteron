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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import no.nordicsemi.android.nrftoolbox.gls.FHIRService;
import no.nordicsemi.android.nrftoolbox.gls.GlucoseActivity;
import no.nordicsemi.android.nrftoolbox.gls.GlucoseRecord;

import java.util.Calendar;

public class SplashscreenActivity extends Activity {
    /**
     * Splash screen duration time in milliseconds
     */
    private static final int DELAY = 1000;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

//        FHIRService s = new FHIRService(this);
//
//        GlucoseRecord gr1 = new GlucoseRecord();
//        gr1.setUnit(GlucoseRecord.UNIT_kgpl);
//        gr1.setTime(Calendar.getInstance());
//        gr1.setGlucoseConcentration(10.2f);
//
//        GlucoseRecord gr2 = new GlucoseRecord();
//        gr2.setUnit(GlucoseRecord.UNIT_molpl);
//        gr2.setGlucoseConcentration(105.7f);
//        gr2.setTime(Calendar.getInstance());
//
//        SparseArray<GlucoseRecord> arrr = new SparseArray<>();
//        arrr.put(0, gr1);
//        arrr.put(1, gr2);
//
//        s.execute(arrr);

//		// Jump to SensorsActivity after DELAY milliseconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                final Intent intent = new Intent(SplashscreenActivity.this, GlucoseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                finish();
            }
        }, DELAY);
    }

    @Override
    public void onBackPressed() {
        // do nothing. Protect from exiting the application when splash screen is shown
    }

}
