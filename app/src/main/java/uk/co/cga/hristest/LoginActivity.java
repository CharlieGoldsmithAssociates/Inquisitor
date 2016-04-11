package uk.co.cga.hristest;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
// import android.app.LoaderManager.LoaderCallbacks;
// import android.database.Cursor;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import static android.Manifest.permission.INTERNET;

/**
 * A login screen that offers login via email/password.
 * * was implements LoaderCallbacks<Cursor>
 */
public class LoginActivity extends AppCompatActivity  {

    /**
     * Id to identity  permission requests.
     */
    private static final int REQUEST_INTERNET = 0;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView mPwdStatus;
    private LinearLayout mLoStatus;
    private Boolean bShowPwd;
    // create a handler to update the UI from background thread
    private Handler uiProgress   = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg)
        {
            if ( msg != null )
            {
                Bundle b = msg.getData();
                String sMsg=b.getString("MSG");
                if ( sMsg != null )
                {
                    mPwdStatus.setText(sMsg);
                    Log.v("HRISLOG", "status:" + sMsg);
                }
                sMsg=b.getString("SUBMSG");
                if ( sMsg != null )
                {
                    String sTmp = mPwdStatus.getText().toString();
                    int iPos = sTmp.indexOf(':');
                    if ( iPos>0 ) sTmp = sTmp.substring(0,iPos);
                    sTmp += ":"+ sMsg;
                    mPwdStatus.setText(sTmp);
                    Log.v("HRISLOG", "status:" + sTmp);
                }

            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d("HRISLOG", "LoginActivity start");

        cGlobal.main_Context = this;
        cGlobal.sp = getSharedPreferences("HRISLOG", 0);
        cGlobal.InitDatabase();
        cGlobal.packageName= getApplicationContext().getPackageName();

        // Set up the login form.
        mPwdStatus= (TextView)findViewById(R.id.txtPwdStatus);
        mPwdStatus.setText("Starting..");
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        mLoStatus = (LinearLayout) findViewById (R.id.layoutStatus);
        //populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        bShowPwd=false;
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        ImageButton butQuit = (ImageButton) findViewById(R.id.butQuit);
        butQuit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // restore last successful user for speed
        String sLastUser = cGlobal.getPref("lastUSER", "");
        mEmailView.setText(sLastUser);

        if ( BuildConfig.DEBUG ) {
            mPasswordView.setText(cPwd.sDebugPwd );
        }
        TextView tvTmp = (TextView) findViewById(R.id.tvVersion);
        String version = BuildConfig.VERSION_NAME;
        tvTmp.setText( version);

        // with thanks to http://www.codeproject.com/Tips/518641/Show-hide-password-in-a-edit-text-view-password-ty
        ImageButton bShowPwd = (ImageButton) findViewById(R.id.butShowPwd);
        bShowPwd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                togglePwd();
            }
        });

        if ( mAuthTask != null )
        {
            showProgress(true);
        }

    }

    private void togglePwd()
    {

        if (!bShowPwd) {
            // show password
            bShowPwd=true;
            Log.v("HRISLOG", "Show PWD");
            mPasswordView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());

        } else {
            // hide password
            bShowPwd=false;
            Log.v("HRISLOG", "Hide PWD");
            mPasswordView.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
    }

    private void populateAutoComplete() {
        if (!mayContactInternet()) {
            return;
        }

        // loader invoke call getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayContactInternet() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        Log.d("HRISLOG", "Check internet permission");

        if (checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(INTERNET)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
                        }
                    });
        } else {
            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d("HRISLOG", "handle permisison result");

        if (requestCode == REQUEST_INTERNET) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        Log.d("HRISLOG", "LoginActivity attemptLogin");

        if (mAuthTask != null) {
            return;
        }

        // login bypass for debugging
        /*if ( mAuthTask == null) {
            String sUser = cGlobal.getPref("lastUSER");

            String sUserRec = cGlobal.getPref("usr" + sUser);
            cGlobal.doAdjudicatorLogin(sUserRec, sUser);
            // move on to next activity..
            Intent it = new Intent(getApplicationContext(), CandidateActivity.class);
            it.putExtra("UID", cGlobal.rUser[0]);
            startActivity(it);

            return;
        }*/

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String login = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(login)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isLoginValid(login)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(login, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isLoginValid(String sLogin) {

        return sLogin.length()> 4 && sLogin.length() < 32 ;
    }

    private boolean isPasswordValid(String password) {

        return (password.length() > 4 && password.length() < 32) ;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mPwdStatus.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoStatus.setVisibility(show ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    /*  perhaps use loader later but we have simpler string arrays to load
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void addStaffToAutoComplete(List<String> staffCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, staffCollection);

        mEmailView.setAdapter(adapter);
    }*/


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final String mLogin;
        private final String mPassword;
        private String mLastErr;

        UserLoginTask(String login, String password ) {
            mLogin = login;
            mPassword = password;
            mLastErr="";
        }

        public void statusReport ( String sReport )
        {
            Bundle b = new Bundle(1);
            b.putString("MSG",sReport);
            Message msg =new Message();
            msg.setData(b);
            uiProgress.sendMessage( msg );
        }
        @Override
        protected Boolean doInBackground(Void... params)
        {
            Log.v("HRISLOG","doInBackground-login check");
            statusReport("checking: please wait.");
            if ( DoLoginCheck() )
                return true;

            return false;

        }

        private boolean DoLoginCheck (  )
        {
            Boolean bReply = false;
            try {
                Log.v("HRISLOG","Do login check: start");

                cGlobal.setPref("currentUSER", "");

                // is the internet connected.. if so use it.
                Log.v("HRISLOG","Do login check: chk net");
                if ( cUtils.isNetworkAvailable() )
                {
                    Log.v("HRISLOG","Do login check: do API call ");
                    String sReply = cUtils.getAPIResult("LOGIN", "&U="+ URLEncoder.encode(mLogin, "utf-8") +
                            "&P="+URLEncoder.encode(mPassword, "utf-8") );
                    // reply is OK:UID:GEO Permissions:
                    // or ERROR:<reason>
                    if ( cUtils.isAPIResultOK(sReply) )
                    {
                        Log.d("HRISLOG", "Reply is OK "+ sReply );
                        // it's good store for later
                        // reply is UID \t	Name \t	<VIS> \t <MDA> \t <rad session key>
                        // firstly the current user

                        String sUserRec = cUtils.getAPIResulttext(sReply);
                        cGlobal.doAdjudicatorLogin(sUserRec, mLogin);

                        // and store/cache the password for offline login later
                        cGlobal.setPref("pwd"+ mLogin, cUtils.HashString(mPassword) );
                        statusReport(  "Welcome "+mLogin);
                        // and return true = ok
                        bReply = true;
                        Log.v("HRISLOG","Do login check: good reply exit true ");
                        return bReply;
                    }

                    Log.d("HRISLOG", "Reply is error " + sReply.substring(0, Math.min(sReply.length(), 50)));

                    statusReport("Invalid password");

                    mLastErr = cUtils.getAPIResulttext(sReply);
                    if ( mLastErr.length()==0 )
                        mLastErr= getString(R.string.errLogin3);
                    return false;
                }
                else
                {
                    mLastErr= getString(R.string.errLogin1);
                }
                Log.d("HRISLOG", "Not online : check cache ");
                statusReport("checking: network not available");

                // if it gets here we're not online so use cache
                // is the user name in local cache - stored after first succesful login
                String sTestPwd = cGlobal.getPref("pwd"+ mLogin);
                if ( sTestPwd.length()>0)
                {
                    Log.v("HRISLOG", "Cache hit ");
                    String sPwdHash = cUtils.HashString(mPassword);
                    if (sTestPwd.equals(sPwdHash)) {
                        // good to go - recall the last user prefs we have for ths person
                        Log.d("HRISLOG", "Password match ");
                        String sUserRec = cGlobal.getPref("usr"+ mLogin);
                        // and set them as current
                        cGlobal.doAdjudicatorLogin(sUserRec, mLogin);

                        statusReport("Welcome " + cGlobal.curAdjudicatorName() + "\nUsing stored login details");
                        Log.v("HRISLOG","Do login check: good reply exit true ");
                        bReply = true;// hit finally and get staff list
                        return bReply;
                    }
                    else
                    {
                        // wrong password
                        Log.v("HRISLOG", "Cache hit: password missmatch ");

                        statusReport("Invalid password, using stored login details");
                        bReply = false;

                    }
                }
                Log.v("HRISLOG", "Cache miss ");

                // not found in cache
                statusReport( "Cannot check password: please connect to internet");
            }
            catch (Exception e) {
                Log.e("HRISLOG", "Exception checking password:"+ e.getMessage());
                mLastErr= getString(R.string.errLogin2) +"\n"+ e.getMessage();
                bReply= false;
            }

            return bReply;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success)
            {
                // grab the stored session key into global for speed.
                String sUserRec = cGlobal.getPref("currentUSER");
                cGlobal.rUser = sUserRec.split("\t");

                // move on to next activity..
                Intent it = new Intent(getApplicationContext(), loadingActivity.class); //MainActivity.class);
                it.putExtra("UID", cGlobal.rUser[0]);
                startActivity(it);

            } else {
                if ( mLastErr.length()==0 )
                    mLastErr=getString(R.string.error_incorrect_password);

                mPasswordView.setError(mLastErr);
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }


    }
}

