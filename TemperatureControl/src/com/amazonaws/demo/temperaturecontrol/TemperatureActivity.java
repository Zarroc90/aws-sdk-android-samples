/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.demo.temperaturecontrol;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class TemperatureActivity extends Activity implements View.OnClickListener {

    private static final String LOG_TAG = TemperatureActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "acqx6akcdcn9n-ats.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-central-1:4b38968f-863d-4286-a967-8dfd62fb754c";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_CENTRAL_1;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    CognitoCachingCredentialsProvider credentialsProvider;

    AWSIotDataClient iotDataClient;
    ToggleButton toggle;
    Boolean plugStatus;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    @Override
    public void onLocalVoiceInteractionStarted() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toggle = (ToggleButton)findViewById(R.id.plugButton);
        //Register Sign in Button Listener
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.plugButton).setOnClickListener(this);


         //Initialize the Amazon Cognito credentials provider
        /*credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );*/

        //----------------Google Sign in---------------------------------------------------

        String token = null;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);



        //GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
       /* AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String token = null;
        try {
            token = GoogleAuthUtil.getToken(getApplicationContext(), accounts[0].name,
                    "audience:server:client_id:121076335235-c6hhje1e9jf44kuficba2a6b6mjj06bq.apps.googleusercontent.com");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GoogleAuthException e) {
            e.printStackTrace();
        }*/
        //Map<String, String> logins = new HashMap<String, String>();
        //logins.put("accounts.google.com", token);

        //----------------Google Sign in---------------------------------------------------

        /*credentialsProvider.setLogins(logins);

        iotDataClient = new AWSIotDataClient(credentialsProvider);
        String iotDataEndpoint = CUSTOMER_SPECIFIC_ENDPOINT;
        iotDataClient.setEndpoint(iotDataEndpoint);

        NumberPicker np = (NumberPicker) findViewById(R.id.setpoint);
        np.setMinValue(60);
        np.setMaxValue(80);
        np.setWrapSelectorWheel(false);

        getShadows();*/
    }



    public void temperatureStatusUpdated(String temperatureStatusState) {
        Gson gson = new Gson();
        TemperatureStatus ts = gson.fromJson(temperatureStatusState, TemperatureStatus.class);

        Log.i(LOG_TAG, String.format("intTemp:  %d", ts.state.desired.intTemp));
        Log.i(LOG_TAG, String.format("extTemp:  %d", ts.state.desired.extTemp));
        Log.i(LOG_TAG, String.format("curState: %s", ts.state.desired.curState));

        TextView intTempText = (TextView) findViewById(R.id.intTemp);
        intTempText.setText(ts.state.desired.intTemp.toString());
        if ("stopped".equals(ts.state.desired.curState)) {
            intTempText.setTextColor(getResources().getColor(R.color.colorOff));
        } else if ("heating".equals(ts.state.desired.curState)) {
            intTempText.setTextColor(getResources().getColor(R.color.colorHeating));
        } else if ("cooling".equals(ts.state.desired.curState)) {
            intTempText.setTextColor(getResources().getColor(R.color.colorCooling));
        }

        TextView extTempText = (TextView) findViewById(R.id.extTemp);
        extTempText.setText(ts.state.desired.extTemp.toString());
    }

    public void temperatureControlUpdated(String temperatureControlState) {
        Gson gson = new Gson();
        TemperatureControl tc = gson.fromJson(temperatureControlState, TemperatureControl.class);

        Log.i(LOG_TAG, String.format("setPoint: %d", tc.state.desired.setPoint));
        Log.i(LOG_TAG, String.format("enabled: %b", tc.state.desired.enabled));

        NumberPicker np = (NumberPicker) findViewById(R.id.setpoint);
        np.setValue(tc.state.desired.setPoint);

        ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);
        tb.setChecked(tc.state.desired.enabled);
    }

    public void PLugUpdated (String plugstatus){

        if(plugstatus.contains("on")){
            plugStatus = TRUE;
            toggle.setTextOn("ON");
        }else if (plugstatus.contains("off")){
            plugStatus = FALSE;
            toggle.setTextOff("OFF");
        }
        toggle.setChecked(plugStatus);
    }

    public void getShadow(View view) {
        getShadows();
    }

    public void getShadows() {
        GetShadowTask getStatusShadowTask = new GetShadowTask("TemperatureStatus");
        getStatusShadowTask.execute();

        GetShadowTask getControlShadowTask = new GetShadowTask("TemperatureControl");
        getControlShadowTask.execute();

        GetShadowTask getPlugShadowTask = new GetShadowTask("IP_44_Plug");
        getPlugShadowTask.execute();
        Log.i(LOG_TAG,"This was the IP44 PLug");
    }

    public void enableDisableClicked(View view) {
        ToggleButton tb = (ToggleButton) findViewById(R.id.enableButton);

        Log.i(LOG_TAG, String.format("System %s", tb.isChecked() ? "enabled" : "disabled"));
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName("TemperatureControl");
        String newState = String.format("{\"state\":{\"desired\":{\"enabled\":%s}}}",
                tb.isChecked() ? "true" : "false");
        Log.i(LOG_TAG, newState);
        updateShadowTask.setState(newState);
        updateShadowTask.execute();
    }

    public void updateSetpoint(View view) {
        NumberPicker np = (NumberPicker) findViewById(R.id.setpoint);
        Integer newSetpoint = np.getValue();
        Log.i(LOG_TAG, "New setpoint:" + newSetpoint);
        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName("TemperatureControl");
        String newState = String.format("{\"state\":{\"desired\":{\"setPoint\":%d}}}", newSetpoint);
        Log.i(LOG_TAG, newState);
        updateShadowTask.setState(newState);
        updateShadowTask.execute();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.plugButton:
                plugAction();
                break;
            // ...
        }
    }

    private void plugAction() {

        String newState;
        if(plugStatus){
            newState= ("{\"state\":{\"desired\":{\"status\":\"on\"}}}");
        }else{
            newState= ("{\"state\":{\"desired\":{\"status\":\"off\"}}}");
        }

        UpdateShadowTask updateShadowTask = new UpdateShadowTask();
        updateShadowTask.setThingName("IP_44_Plug");
        Log.i(LOG_TAG, newState);
        updateShadowTask.setState(newState);
        updateShadowTask.execute();


    }



    private class GetShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private final String thingName;

        public GetShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                GetThingShadowRequest getThingShadowRequest = new GetThingShadowRequest()
                        .withThingName(thingName);
                GetThingShadowResult result = iotDataClient.getThingShadow(getThingShadowRequest);
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                Log.e("E", "getShadowTask", e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(GetShadowTask.class.getCanonicalName(), result.getResult());
                if ("TemperatureControl".equals(thingName)) {
                    temperatureControlUpdated(result.getResult());
                } else if ("TemperatureStatus".equals(thingName)) {
                    temperatureStatusUpdated(result.getResult());
                }else if ("IP_44_Plug".equals(thingName)) {
                    PLugUpdated(result.getResult());
                }
            } else {
                Log.e(GetShadowTask.class.getCanonicalName(), "getShadowTask", result.getError());
            }
        }
    }

    private class UpdateShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private String thingName;
        private String updateState;

        public void setThingName(String name) {
            thingName = name;
        }

        public void setState(String state) {
            updateState = state;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                UpdateThingShadowRequest request = new UpdateThingShadowRequest();
                request.setThingName(thingName);

                ByteBuffer payloadBuffer = ByteBuffer.wrap(updateState.getBytes());
                request.setPayload(payloadBuffer);

                UpdateThingShadowResult result = iotDataClient.updateThingShadow(request);

                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "updateShadowTask", e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                Log.i(UpdateShadowTask.class.getCanonicalName(), result.getResult());
            } else {
                Log.e(UpdateShadowTask.class.getCanonicalName(), "Error in Update Shadow",
                        result.getError());
            }
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String photourl = account.getPhotoUrl().toString();
            String token = account.getIdToken();

            final Map<String, String> logins = new HashMap<String, String>();
            logins.put("accounts.google.com", token);


        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    credentialsProvider.setLogins(logins);
                    credentialsProvider.refresh();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    iotDataClient = new AWSIotDataClient(credentialsProvider);
                    String iotDataEndpoint = CUSTOMER_SPECIFIC_ENDPOINT;
                    iotDataClient.setEndpoint(iotDataEndpoint);

                    NumberPicker np = (NumberPicker) findViewById(R.id.setpoint);
                    np.setMinValue(60);
                    np.setMaxValue(80);
                    np.setWrapSelectorWheel(false);

                    getShadows();
                }
            }.execute();

        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);

        }




    }
}
