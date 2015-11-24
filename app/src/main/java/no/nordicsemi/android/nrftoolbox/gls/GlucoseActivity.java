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
package no.nordicsemi.android.nrftoolbox.gls;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileExpandableListActivity;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

// TODO The GlucoseActivity should be rewritten to use the service approach, like other do.
public class GlucoseActivity extends BleProfileExpandableListActivity implements PopupMenu.OnMenuItemClickListener, GlucoseManagerCallbacks {
    @SuppressWarnings("unused")
    private static final String TAG = "GlucoseActivity";

    private BaseExpandableListAdapter mAdapter;
    private GlucoseManager mGlucoseManager;

    private View mRootLayout;
    private View mControlPanelStd;
    private View mControlPanelAbort;
    private TextView mUnitView;
    private FHIRService fhir;

    private Button btnFhir;

    @Override
    protected void onCreateView(final Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            Long.parseLong(prefs.getString(getString(R.string.setting_date_of_birth), "" + new Date().getTime()));
        }
        catch(NumberFormatException e) {
            prefs.edit().putString(getString(R.string.setting_date_of_birth), "" + Calendar.getInstance().getTimeInMillis());
            prefs.edit().apply();
        }
            // FEATURE_INDETERMINATE_PROGRESS notifies the system, that we are going to show indeterminate progress bar in the ActionBar (during device scan)
        // requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); // <- Deprecated
        setContentView(R.layout.activity_feature_gls);
        setGUI();
    }

    private void setGUI() {
        mRootLayout = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        mUnitView = (TextView) findViewById(R.id.unit);
        mControlPanelStd = findViewById(R.id.gls_control_std);
        mControlPanelAbort = findViewById(R.id.gls_control_abort);
        btnFhir = (Button) findViewById(R.id.action_fhir);

        btnFhir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFHIRResources();
            }
        });

        findViewById(R.id.action_last).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGlucoseManager.getLastRecord();
            }
        });
        findViewById(R.id.action_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGlucoseManager.getAllRecords();
            }
        });
        findViewById(R.id.action_abort).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGlucoseManager.abort();
            }
        });

        setListAdapter(mAdapter = new ExpandableRecordAdapter(this, mGlucoseManager));
    }

    @Override
    protected BleManager<GlucoseManagerCallbacks> initializeManager() {
        GlucoseManager manager = mGlucoseManager = GlucoseManager.getGlucoseManager(getApplicationContext());
        manager.setGattCallbacks(this);
        return manager;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mGlucoseManager.refreshRecords();
                break;
            case R.id.action_first:
                mGlucoseManager.getFirstRecord();
                break;
            case R.id.action_clear:
                mGlucoseManager.clear();
                break;
            case R.id.action_delete_all:
                mGlucoseManager.deleteAllRecords();
                break;
            case R.id.action_fhir:
                sendFHIRResources();
                break;
        }
        return true;
    }

    private void sendFHIRResources() {


        final ProgressDialog progress = ProgressDialog.show(this, "Please wait...", "while I'm fhiring your data.", false);
//        if (fhir == null) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        fhir = new FHIRService(prefs.getString(this.getString(R.string.setting_sever_address), FHIRService.SERVER_URL), this, mGlucoseManager.getmDeviceInformation(), Patient.newInstance(this)) {
            @Override
            protected void onPostExecute(Throwable throwable) {
                super.onPostExecute(throwable);

                progress.dismiss();
                //Snackbar.make(mRootLayout, "Data was sent successfully.", Snackbar.LENGTH_LONG).show();

                AlertDialog.Builder builder =
                        new AlertDialog.Builder(GlucoseActivity.this, R.style.AppCompatAlertDialogStyle);
                if (throwable == null) {
                    builder.setTitle("Success");
                    builder.setMessage("Data was fhired successfully.");
                    builder.setPositiveButton("Great", null);
                } else {
                    builder.setTitle("Failure");
                    builder.setMessage("Could not fhir you data:\n\n" + throwable.getMessage());
                    throwable.printStackTrace();
                    builder.setPositiveButton("Damn it", null);
                }
                builder.show();

            }
        };
//        }
//        if (fhir.getStatus() == AsyncTask.Status.RUNNING)
//            fhir.cancel(false);

        fhir.execute(mGlucoseManager.getRecords());

    }

    @Override
    protected int getLoggerProfileTitle() {
        return R.string.gls_feature_title;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.gls_about_text;
    }

    @Override
    protected int getDefaultDeviceName() {
        return R.string.gls_default_name;
    }

    @Override
    protected UUID getFilterUUID() {
        return GlucoseManager.GLS_SERVICE_UUID;
    }

    @Override
    protected void setDefaultUI() {
        mGlucoseManager.clear();
    }

    private void setOperationInProgress(final boolean progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // setSupportProgressBarIndeterminateVisibility(progress);
                mControlPanelStd.setVisibility(!progress ? View.VISIBLE : View.GONE);
                mControlPanelAbort.setVisibility(progress ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onDeviceDisconnected() {
        super.onDeviceDisconnected();
        setOperationInProgress(false);
    }

    @Override
    public void onOperationStarted() {
        setOperationInProgress(true);
    }

    @Override
    public void onOperationCompleted() {
        setOperationInProgress(false);
    }

    @Override
    public void onOperationAborted() {
        setOperationInProgress(false);
    }

    @Override
    public void onOperationNotSupported() {
        setOperationInProgress(false);
        showToast(R.string.gls_operation_not_supported);
    }

    @Override
    public void onOperationFailed() {
        setOperationInProgress(false);
        showToast(R.string.gls_operation_failed);
    }

    @Override
    public void onError(final String message, final int errorCode) {
        super.onError(message, errorCode);
        onOperationFailed();
    }

    @Override
    public void onDatasetChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final SparseArray<GlucoseRecord> records = mGlucoseManager.getRecords();
                if (records.size() > 0) {
                    final int unit = records.valueAt(0).unit;
                    mUnitView.setVisibility(View.VISIBLE);
                    mUnitView.setText(unit == GlucoseRecord.UNIT_kgpl ? R.string.gls_unit_mgpdl : R.string.gls_unit_mmolpl);
                } else
                    mUnitView.setVisibility(View.GONE);

                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onNumberOfRecordsRequested(final int value) {
        showToast(getString(R.string.gls_progress, value));
    }
}
