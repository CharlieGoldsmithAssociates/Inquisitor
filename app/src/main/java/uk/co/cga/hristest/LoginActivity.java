package uk.co.cga.hristest;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // restore last successful user for speed
        String sLastUser = cGlobal.getPref("lastUSER", "");
        mEmailView.setText(sLastUser);

        mPasswordView.setText(cPwd.sDebugPwd );

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

        // TODO remove login bypass
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

        UserLoginTask(String login, String password ) {
            mLogin = login;
            mPassword = password;

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
                if ( getStaffList(false) )
                {
                    // download and read all avail questionnaires for this user
                    cQuestionnaire.getAllQuestionnaires( cGlobal.curUID() );
                    return true;
                }

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
                    String sReply = cUtils.getAPIResult("LOGIN", "&U="+mLogin + "&P="+mPassword );
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

                    statusReport( "Invalid password");
                    return false;
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

                        statusReport( "Invalid password, using stored login details");
                        bReply = false;
                    }
                }
                Log.v("HRISLOG", "Cache miss ");

                // not found in cache
                statusReport( "Cannot check password: please connect to internet");
            }
            catch (Exception e) {
                Log.e("HRISLOG", "Exception checking password:"+ e.getMessage());

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
                Intent it = new Intent(getApplicationContext(), MainActivity.class);
                it.putExtra("UID", cGlobal.rUser[0]);
                startActivity(it);

            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }


        public boolean getStaffList ( boolean bForce )
        {
            // user rec is UID \t	Name \t	<VIS> \t <MDA> \t <rad session key>
            String sSessKey =  cGlobal.sessKey();
            String sUID =  cGlobal.curUID();

            statusReport("Welcome " + cGlobal.curAdjudicatorName());
            if ( !cUtils.isNetworkAvailable() )
            {
                if ( !cGlobal.cdb.userHasData(sUID) )
                    statusReport("Welcome " + cGlobal.curAdjudicatorName()+"\nYou need to be online to download Staff Data: ");
                return  cGlobal.cdb.userHasData(sUID);
            }
            if ( !bForce && cGlobal.cdb.userHasData(sUID) ) return true;

            statusReport("Welcome " + cGlobal.curAdjudicatorName()+"\nRequesting Staff Data: ");
            // https://hrisrss.org/cgi-bin/api.pl?R=5506&K=DJKDUa38e327wsuwiosuqw&F=LISTSTAFF&UID=USXAD0000003&S=57680
            String sReply = cUtils.getAPIResult("LISTSTAFF", "&UID="+sUID + "&S="+sSessKey );
            // reply is OK: and a series of records:
            // or ERROR:<reason>
            if ( cUtils.isAPIResultOK(sReply) )
            {
                // parse reply line by line and replace staff database
                /*
                MDA:MDA3	Ministry Of Education
                MDA:MDAMOE	Ministry of Education
                FACILITY:FC01060502	Chukudum County Hosp	2	209	20902	Eastern Equatoria	Budi	Lauro
                FACILITY:SCHZYN	Ungebe Primary School	2	205	20504	Eastern Equatoria	Lafon	Pacidi
                DEBUG: FACILITY COUNT 1090
                DEBUG: STAFF SQL SELECT s.ID,sFName+' '+sMNames+' '+sLName,sDOB,sGender,nrLocalName,f.ID,PID,CID,SID FROM tblStaff s  JOIN tblAssignment a ON aStaff=s.ID AND aEnd IS NULL  JOIN tblNominalRoll n ON aRole=n.ID  JOIN tblFacility f ON nrFacility = f.ID  JOIN tGeoFlat g ON fPayam=PID  WHERE s.DEL=0 AND f.DEL=0 AND aEnd IS NULL  AND PID IN (2,9) OR CID IN (2,9) OR SID IN (2,9)  GROUP BY s.ID
                STAFF:SRCAO0000005	Name here	1981-01-01 15:13:53	Male	Community Certified Nurse	FC02070702	20707	207	2
                */
                cGlobal.cdb.ClearUserTables(sUID);
                Log.v("HRISLOG", "getStaffList: parse reply " + sReply.length());
                StringReader sSR=new StringReader(cUtils.getAPIResulttext(sReply));
                BufferedReader reader = new BufferedReader(sSR);
                try
                {
                    String sWelcome = "Welcome " + cGlobal.curAdjudicatorName() +"\n";
                    String sLine;
                    String sVerb = "";
                    String sOldVerb = "";
                    int iCount=0;
                    while ((sLine = reader.readLine()) != null)
                    {
                        Log.v("HRISLOG", iCount + "=" + sLine );

                        iCount++;
                        if ( (iCount%100)==0 || !sVerb.equals(sOldVerb) ) {
                            if (!sVerb.equals(sOldVerb)) iCount=1;
                            statusReport(sWelcome + "Loading " + sVerb + " Data: " + iCount);
                        }
                        sOldVerb=sVerb;
                        if ( sLine.startsWith("FACILITY:") ) {
                            sVerb = "Facility";
                            if (!cGlobal.cdb.AddFacilityLine(sLine.substring(9), sUID))
                                return false;
                        }
                        else if ( sLine.startsWith("STAFF:") ) {
                            sVerb = "Staff";
                            if (!cGlobal.cdb.AddStaffLine(sLine.substring(6), sUID)) return false;
                        }
                        else if ( sLine.startsWith("MDA:") ) {
                            sVerb = "MDA";
                            if (!cGlobal.cdb.AddMDALine(sLine.substring(4), sUID)) return false;
                        }
                        else if ( sLine.startsWith("DEBUG:") )
                                Log.d("HRISLOG", "HOST DEBUG "+ sLine);
                        else
                            Log.v("HRISLOG","Unhandled line in reply "+ sLine );

                    }
                    Log.v("HRISLOG", "getStaffList: parse reply: done " + iCount);
                    statusReport("Welcome " + cGlobal.curAdjudicatorName()+"\nStaff Data: complete");

                }
                catch ( Exception e )
                {
                    Log.e("HRISLOG","Error reading list-staff reply from host "+ e.getMessage());
                    statusReport("Requesting User Data: Error");

                    return false;
                }
                finally
                {
                    try {
                        reader.close();
                        sSR.close();
                    }
                    catch ( Exception e2 )
                    {
                        Log.e("HRISLOG","Error closing after read list staff reply from host" + e2.getMessage());
                    }
                }
            }
            else
            {
                // it happens - perhaps we're not online.. carry on with what we have in db
                Log.v("HRISLOG","getStaffList: not a valid reply" );
                statusReport("Requesting User Data: invalid reply");

            }
            return true;
        }
    }
}

