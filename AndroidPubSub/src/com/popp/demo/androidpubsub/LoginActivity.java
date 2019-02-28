package com.popp.demo.androidpubsub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.Listener;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ProfileScope;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.workflow.RequestContext;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCognitoIdentityProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.auth.IdentityChangedListener;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.mobileconnectors.cognitoauth.Auth;
import com.amazonaws.mobileconnectors.cognitoauth.AuthUserSession;
import com.amazonaws.mobileconnectors.cognitoauth.handlers.AuthHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import com.amazonaws.services.cognitoidentityprovider.model.ConfirmSignUpResult;
import com.popp.demo.androidpubsub.Res.IDProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.popp.demo.androidpubsub.Res.Statics;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static com.amazonaws.auth.policy.Principal.WebIdentityProviders.Amazon;


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
    private static final Regions MY_REGION = Regions.EU_WEST_1;
    //private static final String APP_CLIENT_ID ="1fjetpf94pgk1lfkrpca5ulqdr";
    private static final String APP_CLIENT_ID ="dgpm1mq6bvg607umsfngitn19";
    //private static final String APP_CLIENT_SECRET ="1b6ugvtiavr0britv54m2epaf2a3g8q6bhi2u8p9j8k5402o9kqc";
    private static final String APP_CLIENT_SECRET ="1k0b37p5p0fcmsnao39b8qvqq5eq1fgipp38j3v2lg6j8e9vlcla";
    private static final String USER_POOL_ID ="eu-west-1_KavBtiUTD";
    private Auth auth;


    AWSIotDataClient iotDataClient;

    private Context mContext;
    private LoginActivity loginContext;

    private RequestContext requestContext;
    public IDProvider idProvider ;

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
        idProvider = new IDProvider(getApplicationContext());
        loginContext=this;
        initCognito();




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
                getAmazonToken();

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
            /*case R.id.google_sign_in_button:
                signIn_Google();
                break;
            case R.id.amazon_sign_in_button:
                signIn_Amazon();
                break;*/
            case R.id.buttonSignin:{
                this.auth.getSession();
                break;
            }
        }

    }

    void initCognito() {
        //  -- Create an instance of Auth --
        Auth.Builder builder = new Auth.Builder().setAppClientId(getString(R.string.cognito_client_id))
                .setAppClientSecret(getString(R.string.cognito_client_secret))
                .setAppCognitoWebDomain(getString(R.string.cognito_web_domain))
                .setApplicationContext(getApplicationContext())
                .setAuthHandler(new callback())
                .setSignInRedirect(getString(R.string.app_redirect))
                .setSignOutRedirect(getString(R.string.app_redirect));
        this.auth = builder.build();
        appRedirect = Uri.parse(getString(R.string.app_redirect));
    }

    private void signIn_Google() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Statics.RC_SIGN_IN);
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
        if (requestCode == Statics.RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {

        @Override
        public void onSuccess(CognitoUserSession userSession, CognitoDevice newDevice) {

            System.out.println("Sign-In Success");

            final Map<String, String> logins = new HashMap<String, String>();
            //logins.put("accounts.google.com", token);
            logins.put("cognito-idp.eu-west-1.amazonaws.com/eu-west-1_KavBtiUTD", userSession.getIdToken().getJWTToken());

            idProvider.setLogins(logins);
            idProvider.authenticateWithLogin(mContext,userSession.getUsername());
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String userId) {
            // The API needs user sign-in credentials to continue
            AuthenticationDetails authenticationDetails = new AuthenticationDetails(userId, "pAssw0rd...", null);

            // Pass the user sign-in credentials to the continuation
            authenticationContinuation.setAuthenticationDetails(authenticationDetails);

            // Allow the sign-in to continue
            authenticationContinuation.continueTask();
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {

        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {

        }

        @Override
        public void onFailure(Exception exception) {
            // Sign-in failed, check exception for the cause
            System.out.println("Sign-In Failure: "+exception);
        }
    };


    private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
        try {
            final GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String token = account.getIdToken();

            //-------------------------------------- Sign into User Pool
            final CognitoUserPool userPool = new CognitoUserPool(mContext,USER_POOL_ID,APP_CLIENT_ID,APP_CLIENT_SECRET,MY_REGION);


            CognitoUserAttributes userAttributes = new CognitoUserAttributes();
            userAttributes.addAttribute("email",account.getEmail());
            userAttributes.addAttribute("given_name",account.getGivenName());
            userAttributes.addAttribute("family_name",account.getFamilyName());

            SignUpHandler signUpCallback = new SignUpHandler() {
                @Override
                public void onSuccess(CognitoUser cognitoUser, boolean signUpConfirmationState, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
                    System.out.println("Sign-Up Success");
                    System.out.println("user: "+cognitoUser);
                    System.out.println("Confirmed: "+signUpConfirmationState);
                    System.out.println("Delivery: "+cognitoUserCodeDeliveryDetails);

                    // Sign in the user
                    cognitoUser.getSessionInBackground(authenticationHandler);
                }

                @Override
                public void onFailure(Exception exception) {
                    System.out.println("Sign-Up Failed");
                    System.out.println("exception: "+exception);
                    if (exception.toString().contains("UsernameExistsException")){
                        CognitoUser user = userPool.getUser(account.getId());
                        user.getSessionInBackground(authenticationHandler);
                    }
                }
            };

            userPool.signUpInBackground(account.getId(),"pAssw0rd...",userAttributes,null,signUpCallback);




            //----------------------------------------------------------------
            /*final Map<String, String> logins = new HashMap<String, String>();
            //logins.put("accounts.google.com", token);
            logins.put("cognito-idp.eu-west-1.amazonaws.com/eu-west-1_KavBtiUTD", account.getIdToken());


            IDProvider idProvider = new IDProvider(getApplicationContext());
            idProvider.setLogins(logins);
            idProvider.setLogincontext(this);
            idProvider.authenticateWithLogin(mContext);*/


            //idProvider.setUserId(account.getId());
            idProvider.setLogincontext(this);


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

    public void getAmazonToken(){
        AuthorizationManager.getToken(this, new Scope[]{ProfileScope.profile()}, new Listener<AuthorizeResult, AuthError>() {
            @Override
            public void onSuccess(AuthorizeResult authorizeResult) {
                String token = authorizeResult.getAccessToken();

                final Map<String, String> logins = new HashMap<String, String>();
                logins.put("www.amazon.com", token);

                //IDProvider idProvider = new IDProvider(getApplicationContext());
                idProvider.setLogins(logins);
                idProvider.setLogincontext(loginContext);
                idProvider.authenticateWithLogin(mContext,"User");
            }

            @Override
            public void onError(AuthError authError) {

            }
        });
    }


}
