package com.agrostar.deepak.picsshare;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.agrostar.deepak.picsshare.Utils.FirebaseUtility;
import com.agrostar.deepak.picsshare.Utils.GlideUtility;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ProfileActivity";
    private FirebaseAuth firebaseAuth;
    private GoogleApiClient googleApiClient;
    private CallbackManager callbackManager;
    private static final int RC_SIGN_IN = 1001;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.sign_in_ui) ViewGroup mSignInUi;
    @BindView(R.id.profile) ViewGroup mProfileUi;
    @BindView(R.id.profile_user_photo) CircleImageView mProfilePhoto;
    @BindView(R.id.profile_user_name) TextView mProfileUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        FacebookSdk.sdkInitialize(getApplicationContext());
        // Initialize authentication and set up callbacks
        firebaseAuth = FirebaseAuth.getInstance();

        // GoogleApiClient with Sign In
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,
                        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestIdToken(getString(R.string.server_client_id_web))
                                .build())
                .build();

        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                showSnackBar(error.getMessage());
            }
        });
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            hideProgress();
            showSignedInUI(currentUser);
        } else {
            showSignedOutUI();
        }

    }

    @OnClick(R.id.launch_sign_in) void onClickGoogle() {
        launchSignInIntent();
    }
    @OnClick(R.id.fb_sign_in) void onClickFB() {
        signInWithFacebook();
    }
    @OnClick(R.id.sign_out_button) void onClickSignout() {
        handleSignOut();
    }
    @OnClick(R.id.show_feeds_button) void showFeeds() {
        Intent feedsIntent = new Intent(this, FeedsActivity.class);
        startActivity(feedsIntent);
    }

    private void handleSignOut() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                logoutByProvider(providerId, googleApiClient);
            }
        }
    }

    private void logoutByProvider(String providerId, GoogleApiClient mGoogleApiClient) {
        switch (providerId) {
            case GoogleAuthProvider.PROVIDER_ID:
                firebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                break;

            case FacebookAuthProvider.PROVIDER_ID:
                FacebookSdk.sdkInitialize(this);
                LoginManager.getInstance().logOut();
                break;
        }
        showSignedOutUI();
    }

    private void launchSignInIntent() {
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(intent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleGoogleSignInResult(result);
        }
    }

    private void handleGoogleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.getStatus());
        if (result.isSuccess()) {
            // Successful Google sign in, authenticate with Firebase.
            GoogleSignInAccount acct = result.getSignInAccount();
            firebaseAuthWithGoogle(acct);
        } else {
            // Unsuccessful Google Sign In, show signed-out UI
            Log.d(TAG, "Google Sign-In failed.");
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGooogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        showProgress(R.string.profile_progress_message);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        handleFirebaseAuthResult(result);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        FirebaseCrash.logcat(Log.ERROR, TAG, "auth:onFailure:" + e.getMessage());
                        handleFirebaseAuthResult(null);
                    }
                });
    }

    private void handleFirebaseAuthResult(AuthResult result) {
        // TODO: This auth callback isn't being called after orientation change. Investigate.
        hideProgress();
        if (result != null) {
            Log.d(TAG, "handleFirebaseAuthResult:SUCCESS");
            showSignedInUI(result.getUser());
        } else {
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
            showSignedOutUI();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        showProgress();

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        Log.d(TAG, "deep=:" + task.getException());
                        if(task.getException()!=null) {
                            Log.d(TAG, "deep=:" + task.getException().getLocalizedMessage());
                        }
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        hideProgress();
                        if (user != null) {
                            Log.d(TAG, "handleFirebaseAuthResult:SUCCESS");
                            showSignedInUI(user);
                        }else {
                            Toast.makeText(ProfileActivity.this, "Authentication fb failed.", Toast.LENGTH_SHORT).show();
                            showSignedOutUI();
                        }

                    }
                });

    }

    private void signInWithFacebook() {
        if (hasInternetConnection()) {
            LoginManager.getInstance().logInWithReadPermissions(ProfileActivity.this, Arrays.asList("email", "public_profile"));
        } else {
            showSnackBar(R.string.internet_connection_failed);
        }
    }

    private void showSignedInUI(FirebaseUser firebaseUser) {
        Log.d(TAG, "Showing signed in UI");
        mSignInUi.setVisibility(View.GONE);
        mProfileUi.setVisibility(View.VISIBLE);
        mProfileUsername.setVisibility(View.VISIBLE);
        mProfilePhoto.setVisibility(View.VISIBLE);
        if (firebaseUser.getDisplayName() != null) {
            mProfileUsername.setText(firebaseUser.getDisplayName());
        }

        if (firebaseUser.getPhotoUrl() != null) {
            GlideUtility.loadProfileIcon(firebaseUser.getPhotoUrl().toString(), mProfilePhoto);
        }
        Map<String, Object> updateValues = new HashMap<>();
        updateValues.put("displayName", firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Anonymous");
        updateValues.put("photoUrl", firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);

        FirebaseUtility.getPeopleRef().child(firebaseUser.getUid()).updateChildren(
                updateValues,
                new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError firebaseError, DatabaseReference databaseReference) {
                        if (firebaseError != null) {
                            Toast.makeText(ProfileActivity.this,
                                    "Couldn't save user data: " + firebaseError.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showSignedOutUI() {
        Log.d(TAG, "Showing signed out UI");
        mProfileUsername.setText("");
        mSignInUi.setVisibility(View.VISIBLE);
        mProfileUi.setVisibility(View.GONE);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }

}
