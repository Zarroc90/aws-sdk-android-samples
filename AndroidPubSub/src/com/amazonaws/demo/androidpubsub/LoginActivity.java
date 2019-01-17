package com.amazonaws.demo.androidpubsub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ProfileScope;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.demo.androidpubsub.Res.IDProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static com.amazonaws.demo.androidpubsub.Res.Statics.RC_SIGN_IN;


public class LoginActivity extends Activity implements View.OnClickListener{
    private GoogleSignInClient mGoogleSignInClient;
    CognitoCachingCredentialsProvider credentialsProvider;
    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "acqx6akcdcn9n-ats.iot.eu-central-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-central-1:4b38968f-863d-4286-a967-8dfd62fb754c";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_CENTRAL_1;

    AWSIotDataClient iotDataClient;

    private Context mContext;

    private RequestContext requestContext;

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setContext(getApplicationContext());


        findViewById(R.id.google_sign_in_button).setOnClickListener(this);
        findViewById(R.id.amazon_sign_in_button).setOnClickListener(this);


        //---------------------------------Amazon

        requestContext = RequestContext.create(mContext);

        requestContext.registerListener(new AuthorizeListener() {

            // Authorization was completed successfully.
            @Override
            public void onSuccess(AuthorizeResult result) {
                // Your app is now authorized for the requested scopes
                System.out.println("Success " + result);
            }

            // There was an error during the attempt to authorize the
            //application.
            @Override
            public void onError(AuthError ae) {
                System.out.println("Error " + ae);
                // Inform the user of the error
            }

            // Authorization was cancelled before it could be completed.
            @Override
            public void onCancel(AuthCancellation cancellation) {
                System.out.println("Cancel " + cancellation);
                // Reset the UI to a ready-to-login state
            }


        });

        //---------------------------------Google

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.google_sign_in_button:
                signIn_Google();
                break;
            case R.id.amazon_sign_in_button:
                signIn_Amazon();
                break;
        }

    }

    private void signIn_Google() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signIn_Amazon() {

        AuthorizationManager.authorize(new AuthorizeRequest
                .Builder(requestContext)
                .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                .build());
    }

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

    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String token = account.getIdToken();

            final Map<String, String> logins = new HashMap<String, String>();
            logins.put("accounts.google.com", token);

            IDProvider idProvider = new IDProvider(getApplicationContext());
            idProvider.setLogins(logins);
            idProvider.setLogincontext(this);
            idProvider.authenticateWithLogin(mContext);


        } catch (ApiException e) {
            Log.w(TAG, "handleSignInResult:error", e);

        }

    }


    public void loginSuccesful(){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("return",mGoogleSignInClient.toString());
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }



}
