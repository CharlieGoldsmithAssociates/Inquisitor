package uk.co.cga.hristest;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

interface TaskCallbacks {
    void onProgressUpdate(int percent);
    void onProgressUpdate(String sMsg );

    void onCancelled();

    void onListComplete( ArrayList<String> aNewList );
}

// implements uploadActivity.TaskFragment.TaskCallbacks is self referetial
public class uploadActivity extends AppCompatActivity implements TaskCallbacks {
    private ArrayList<String> maQList;
    private ArrayList<Boolean> idxNoStaff;
    private Integer miCurrentSelectPosition;
    private String msCurrentSavedQRef;
    private ImageButton imbAddS;
    private TextView tvStatus;
    private ProgressBar pbProgBar;
    private Handler mhULProgHandler;
    FloatingActionButton fabDeleteSelected;
    FloatingActionButton fabUploadSelected;
    private ProgressDialog pd = null;
    private TaskFragment mRetainedFragment = null;
    final String retainedFragmentTag = "RetainedFragmentTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        miCurrentSelectPosition = -1;
        maQList = null;
        idxNoStaff = null;
        msCurrentSavedQRef = null;

        setContentView(R.layout.activity_upload);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_upload);
        toolbar.setLogo(R.drawable.hrtest_icon);
        toolbar.setLogoDescription(R.string.app_desc);
        setSupportActionBar(toolbar);

        fabUploadSelected = (FloatingActionButton) findViewById(R.id.fabUpload);
        fabUploadSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // UPLOAD SELECTED..
                Log.d("HRISLOG", "Upload selected line");
                DoUpload(msCurrentSavedQRef);
            }
        });


        fabDeleteSelected = (FloatingActionButton) findViewById(R.id.fabDelete);
        fabDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Delete SELECTED..
                Log.d("HRISLOG", "Delete selected line");
                if (msCurrentSavedQRef != null && msCurrentSavedQRef.length() > 0) {
                    // delete line
                    Log.d("HRISLOG", "Delete selected questionnaire");
                    Snackbar sb = Snackbar.make(view, R.string.delSel, Snackbar.LENGTH_LONG);
                    sb.setAction("Delete", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // purge all uploaded questionaires
                            Log.d("HRISLOG", "Delete selected:confirmed");

                            cQuestionnaire.DeleteSavedQuestionnaire(msCurrentSavedQRef);
                            msCurrentSavedQRef = "";
                            miCurrentSelectPosition = -1;
                            fabDeleteSelected.setVisibility(View.INVISIBLE);
                            RefreshQList();
                        }
                    });
                    sb.show();
                }
            }
        });
        fabUploadSelected.setVisibility(View.GONE);
        fabDeleteSelected.setVisibility(View.GONE);

        // start new questionnaire (back to main)
        ImageButton imb = (ImageButton) findViewById(R.id.butNew);
        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // move back to main
                Log.d("HRISLOG", "Start new questionnaire");

                Intent it = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(it);
            }
        });

        imb = (ImageButton) findViewById(R.id.butUpload);
        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // upload all questionnaires.
                Log.d("HRISLOG", "Start upload ALL");
                // upload all..
                DoUpload();
            }
        });

        // purge all uploaded
        imb = (ImageButton) findViewById(R.id.butPurge);
        imb.setVisibility(View.VISIBLE);
        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // purge
                Log.d("HRISLOG", "Delete uploaded: start confirmation");
                Snackbar sb = Snackbar.make(view, "Delete ALL uploaded questionnaires?", Snackbar.LENGTH_LONG);
                sb.setAction("Delete", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // purge all uploaded questionaires
                        Log.d("HRISLOG", "Delete uploaded:confirmed");
                        cQuestionnaire.PurgeAllUploaded();
                        RefreshQList();
                    }
                });
                sb.show();

            }
        });

        imbAddS = (ImageButton) findViewById(R.id.butAddStaff);
        imbAddS.setVisibility(View.INVISIBLE);
        imbAddS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // retrospectively add staff
                Log.d("HRISLOG", "Add staff id for questionnaire");
                if (miCurrentSelectPosition >= 0) {
                    setStaffMember(view);
                    RefreshQList();
                }
            }
        });

        final ListView lvQuestionnaires = (ListView) findViewById(R.id.lvQuestionnaires);
        lvQuestionnaires.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // enable options for this one
                Log.d("HRISLOG", "Show Select questionnaire detail.." + position);

                if (miCurrentSelectPosition != position) {
                    if (miCurrentSelectPosition >= 0)
                        selectedFeedback(miCurrentSelectPosition, false);
                    miCurrentSelectPosition = position;
                    lvQuestionnaires.setSelection(position);
                    selectedFeedback(miCurrentSelectPosition, true);

//                    QListAdapter ad = (QListAdapter)lvQuestionnaires.getAdapter();
//                    ad.notifyDataSetChanged();
                }
                if (maQList != null && maQList.get(position) != null) {
                    msCurrentSavedQRef = maQList.get(position);
                    fabUploadSelected.setVisibility(View.VISIBLE);
                    fabDeleteSelected.setVisibility(View.VISIBLE);


                    if (idxNoStaff != null && !idxNoStaff.get(position))
                        imbAddS.setVisibility(View.VISIBLE);

                } else {
                    fabUploadSelected.setVisibility(View.GONE);
                    fabDeleteSelected.setVisibility(View.GONE);
                    imbAddS.setVisibility(View.INVISIBLE);
                }

            }
        });

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        mhULProgHandler = new Handler(getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                try {
                    Log.v("HRISLOG", "handleMessage called");
                    Bundle b = msg.getData();
                    if (b != null) {
                        pbProgBar.setVisibility(View.VISIBLE);
                        if (b.containsKey("MSG")) ;
                        {
                            if (pd != null)
                                pd.setMessage(b.getString("MSG"));
                            tvStatus.setText(b.getString("MSG"));
                        }
                        if (b.containsKey("MAX")) ;
                        pbProgBar.setMax(b.getInt("MAX"));
                        if (b.containsKey("PROG"))
                            pbProgBar.setProgress(b.getInt("PROG"));
                    } else if (msg.what > 0) {
                        pbProgBar.setProgress(msg.what);
                    }
                    Log.v("HRISLOG", "handleMessage done");
                } catch (Exception e) {
                    Log.e("HRISLOG", "Exception in callback handler " + e.getMessage());
                }
                return true;
            }
        });

        pbProgBar = (ProgressBar) findViewById(R.id.progressBar);
        pbProgBar.setVisibility(View.GONE);


        // if a config change has happened we might have a task already running in the background..
        // Find the RetainedFragment on Activity restarts
        FragmentManager fm = getSupportFragmentManager();

        // The RetainedFragment has no UI so we must
        // reference it with a tag.
        mRetainedFragment = (TaskFragment) fm.findFragmentByTag(retainedFragmentTag);
        // if Retained Fragment doesn't exist create and add it.
        if (mRetainedFragment == null) {
            // Add the fragment
            mRetainedFragment = new TaskFragment();
            fm.beginTransaction().add((Fragment) mRetainedFragment, retainedFragmentTag).commit();
            // rebuild the list
            RefreshQList();

        } else {
            // The Retained Fragment exists - the task is in process, so 'attach' to it and wait for call
            pbProgBar.setVisibility(View.VISIBLE);
            tvStatus.setText("Loading list ...");
            if (pd == null)
                pd = ProgressDialog.show(this, "Working..", getString(R.string.wait1), true, false);

            mRetainedFragment.onAttach(this); // might be redundant
            if ( mRetainedFragment.listTask == null && mRetainedFragment.ulTask == null)
                mRetainedFragment.listAll();
        }

    }

    private void selectedFeedback(int iPos, boolean bSelected) {
        ListView lvQuestionnaires = (ListView) findViewById(R.id.lvQuestionnaires);
        iPos -= lvQuestionnaires.getFirstVisiblePosition();
        if (iPos >= 0 && iPos <= lvQuestionnaires.getLastVisiblePosition()) {
            View view = lvQuestionnaires.getChildAt(iPos);
            if (view != null) {

                //LinearLayout ll = (LinearLayout) view.findViewById(R.id.llText);
                if (bSelected) {
                    view.setPadding(16, 0, 0, 0);
                } else {
                    view.setPadding(4, 0, 0, 0);
                }
            }
        }
    }

    public void RefreshQList() {
        try {
            Log.v("HRISLOG", "Start RefreshList");
            ListView lvQuestionnaires = (ListView) findViewById(R.id.lvQuestionnaires);
            lvQuestionnaires.setVisibility(View.INVISIBLE);
            pbProgBar.setVisibility(View.VISIBLE);
            tvStatus.setText("Loading list ...");

            // Show the ProgressDialog on this thread
            if (pd == null)
                pd = ProgressDialog.show(this, "Working..", getString(R.string.wait1), true, false);
            else {
                pd.show();
                pd.setMessage(getString(R.string.wait1));
            }

            // do list in background - set adapter on return
            mRetainedFragment.listAll();
            Log.v("HRISLOG", "end RefreshList pending postevec");

        } catch (Exception e) {
            Log.e("HRISLOG", "error refresh list " + e.getMessage());
            e.printStackTrace();
        }

    }


    public void DoUpload() {
        Log.d("HRISLOG", "do upload ALL ");

        pbProgBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.uploading);
        if (pd != null)
            pd.dismiss();
        pd = ProgressDialog.show(this, "Uploading..", "Sending complete questionnaires.", true, false);

        tvStatus.setText(R.string.uploading);

        mRetainedFragment.StartUpload(null);
        //ulTask = new UploadTask(null);
        //ulTask.execute();

    }

    public void DoUpload(String sSavedQRef) {
        Log.d("HRISLOG", "do upload " + sSavedQRef);

        pbProgBar.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.uploading);
        if (pd != null)
            pd.dismiss();
        pd = ProgressDialog.show(this, "Uploading..", "Sending selected questionnaire.", true, false);


        try {
//            ulTask = new UploadTask(sSavedQRef);
//            ulTask.execute();
            mRetainedFragment.StartUpload(sSavedQRef);
        } catch (Exception e) {
            e.printStackTrace();
            pd.dismiss();
            pd = null;
            //ulTask.cancel(true);
            //ulTask = null;
        }
        Log.d("HRISLOG", "do upload ending pending postexec");
    }

    public class QListAdapter extends ArrayAdapter {

        private int resource;
        private LayoutInflater inflater;
        private Context context;

        public QListAdapter(Context ctx, int resourceId, List objects) {
            super(ctx, resourceId, objects);
            Log.v("HRISLOG", "New QListAdapter");
            resource = resourceId;
            inflater = LayoutInflater.from(ctx);
            context = ctx;
            Log.v("HRISLOG", "New QListAdapter done ");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            Log.v("HRISLOG", "show list line " + position);
           /* create a new view of my layout and inflate it in the row */
            convertView = (LinearLayout) inflater.inflate(resource, null);
            LinearLayout llT = (LinearLayout) convertView.findViewById(R.id.llText);

            String qRef = (String) getItem(position);
            convertView.setTag(qRef);
            if (qRef == null)
                return convertView;// data set changing behind us

            if (position == miCurrentSelectPosition) {
                // adding a margin on the text layout shows dark background of base view - ie selected bar/border
                convertView.setPadding(16, 0, 0, 0);
            } else {
                convertView.setPadding(4, 0, 0, 0);
            }

            HashMap<String, String> Q = cQuestionnaire.loadSavedQuestionnaire(qRef);// load from saved store
            /* null after delete function */
            if (Q == null) {
                TextView txtTmp = (TextView) convertView.findViewById(R.id.qTitle);
                txtTmp.setText("Questionnaire deleted");
                convertView.setBackgroundColor(Color.rgb(192, 192, 192));
                return convertView;// data set changing behind us
            }

            Boolean bHasStaff = false;
            String sStaff = getString(R.string.nostaff);
            ImageView iv = (ImageView) convertView.findViewById(R.id.imgStaff);
            if (Q.containsKey(cQuestionnaire.STAFFID)) {
                bHasStaff = (Q.get(cQuestionnaire.STAFFID).length() > 0);
                sStaff = safeGet(Q, cQuestionnaire.STAFFNAME);
                iv.setVisibility(View.VISIBLE);
                if ((position % 2) == 0)
                    convertView.setBackgroundColor(Color.WHITE);
                else
                    convertView.setBackgroundColor(Color.rgb(192, 192, 192));
            } else {
                // NO Staff yet
                String sHint = safeGet(Q, cQuestionnaire.STAFFPROMPT);
                if (sHint.length() > 0) {
                    sStaff = sHint;
                }
                iv.setVisibility(View.INVISIBLE);
                convertView.setBackgroundColor(Color.rgb(255, 192, 192));
            }

            idxNoStaff.add(position, bHasStaff);

            TextView txtTmp = (TextView) convertView.findViewById(R.id.qTitle);
            String sTitle = safeGet(Q, cQuestionnaire.TITLE);
            String sTmp = sTitle + ":" + sStaff;
            txtTmp.setText(sTmp);

            String sDescription = safeGet(Q, cQuestionnaire.SUMMARY);
            String[] aDescLines = sDescription.split("~~");
            if (aDescLines.length > 1) {
                txtTmp = (TextView) convertView.findViewById(R.id.qDescription);
                sTmp = aDescLines[0];
                if (sTmp == null || sTmp.length() == 0) sTmp = "Error no title for q ref:" + qRef;
                txtTmp.setText(sTmp);

                txtTmp = (TextView) convertView.findViewById(R.id.qDescription);
                sTmp = "";
                String sNewLine = "";
                for (Integer i = 1; i < aDescLines.length - 1; i++) {
                    sTmp += sNewLine + aDescLines[i];
                    sNewLine = "\n";
                }
                txtTmp.setText(sTmp);

                // store the reference for this line for each of processing later
                txtTmp = (TextView) convertView.findViewById(R.id.tvReference);
                int iLast = aDescLines.length - 1;
                txtTmp.setText(aDescLines[iLast]);
                //txtTmp.setVisibility(View.GONE);
            }

            iv = (ImageView) convertView.findViewById(R.id.imgUL);
            if (Q.containsKey(cQuestionnaire.UPLOADED) &&
                    Q.get(cQuestionnaire.UPLOADED).length() > 1) {
                iv.setVisibility(View.VISIBLE);
                convertView.setBackgroundColor(Color.GRAY);
            } else
                iv.setVisibility(View.INVISIBLE);
            Log.v("HRISLOG", "show list line DONE " + position);
            Q.clear();
            return convertView;
        }
    }

    public String safeGet(HashMap<String, String> Q, String sKey) {
        String sTmp = "";

        if (Q.containsKey(sKey))
            sTmp = Q.get(sKey);
        if (sTmp == null)
            sTmp = "";
        return sTmp;
    }

    public void setStaffMember(View v) {
        String sHint = getString(R.string.setstaff);
        // get the hint if any
        HashMap<String, String> hm = cQuestionnaire.loadSavedQuestionnaire(msCurrentSavedQRef);
        if (hm.containsKey(cQuestionnaire.STAFFPROMPT))
            sHint += ". " + hm.get(cQuestionnaire.STAFFPROMPT);

        Snackbar sb = Snackbar.make(v, sHint, Snackbar.LENGTH_LONG);
        sb.setAction("Choose", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get name
                Intent it = new Intent(getApplicationContext(), CandidateActivity.class);
                startActivityForResult(it, 0);
            }
        });
        sb.show();
        hm.clear();

    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent b) {
        super.onActivityResult(reqCode, resCode, b);

        if (resCode == Activity.RESULT_OK) {
            Log.v("HRISLOG", "get Name returned success - set name for " + msCurrentSavedQRef);
            // read questionnaire as hash,
            HashMap<String, String> Q = cQuestionnaire.loadSavedQuestionnaire(msCurrentSavedQRef);

            String sHRISID = cGlobal.getPref("HRISID");
            String sCandidateName = cGlobal.getPref("CANDIDATE");
            Q.put(cQuestionnaire.STAFFNAME, sCandidateName);
            Q.put(cQuestionnaire.STAFFID, sHRISID);
            cQuestionnaire.updateSavedQuestionnaire(msCurrentSavedQRef, Q);
            RefreshQList();
        }
    }

    // persistent task fragment callback - percent
    // called within background context
    @Override
    public void onProgressUpdate(int percent)
    {
        mhULProgHandler.sendEmptyMessage(percent);
    }
    @Override
    public void onProgressUpdate(String sMsg )
    {
        Bundle b = new Bundle();
        b.putString("MSG", sMsg);
        Message msg = new Message();
        msg.setData(b);
        mhULProgHandler.sendMessage(msg);
    }

    @Override
    public void onCancelled()
    {
        onProgressUpdate("Background activity cancelled");
        if (pd != null)
            pd.dismiss();
        pd = null;
        pbProgBar.setVisibility(View.GONE);
        fabDeleteSelected.setVisibility(View.GONE);
        fabUploadSelected.setVisibility(View.GONE);
        imbAddS.setVisibility(View.INVISIBLE);

    }


    @Override
    public void onListComplete( ArrayList<String> aNewList ) {
        if (pd != null)
            pd.dismiss();
        pd = null;

        ListView lvQuestionnaires = (ListView) findViewById(R.id.lvQuestionnaires);

        QListAdapter ad = (QListAdapter) lvQuestionnaires.getAdapter();
        if (ad == null) {
            Log.v("HRISLOG", "Create new adpater first time");
            idxNoStaff = new ArrayList<Boolean>(aNewList.size());
            maQList = aNewList;
            ad = new QListAdapter(getApplicationContext(), R.layout.layoutsavedq, maQList);
            lvQuestionnaires.setAdapter(ad);
        } else {
            Log.v("HRISLOG", "reset adapter");
            ad.clear();
            maQList.clear();
            idxNoStaff = new ArrayList<Boolean>(aNewList.size());
            ad.addAll(aNewList);
            maQList = aNewList;

            ad.notifyDataSetChanged();
        }
        Log.v("HRISLOG", "onPostExec tidy up");

        pbProgBar.setVisibility(View.GONE);
        lvQuestionnaires.setVisibility(View.VISIBLE);

        if (maQList.size() == 0)
            tvStatus.setText(R.string.upload_noQ);
        else
            tvStatus.setText(R.string.upload_toptext);

        fabDeleteSelected.setVisibility(View.GONE);
        fabUploadSelected.setVisibility(View.GONE);
        imbAddS.setVisibility(View.INVISIBLE);

    }

    /**
     * This Fragment manages a single background task and retains
     * itself across configuration changes.
     */
    public static class TaskFragment extends Fragment {
        /**
         * Callback interface through which the fragment will report the
         * task's progress and results back to the Activity.
         */
        private TaskCallbacks mCallbacks;
        private UploadTask ulTask;
        private ListTask listTask;
        private Handler mFeedbackHandler;

        /**
         * Hold a reference to the parent Activity so we can report the
         * task's current progress and results. The Android framework
         * will pass us a reference to the newly created Activity after
         * each configuration change.
         */
        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Log.v("HRISLOG", "retained frag attach");

            mCallbacks = (TaskCallbacks) activity;
        }

        /**
         * This method will only be called once when the retained
         * Fragment is first created.
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.v("HRISLOG", "retained frag create");

            // Retain this fragment across configuration changes.
            setRetainInstance(true);
            mFeedbackHandler = new Handler( new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg)
                {
                    if (mCallbacks != null)
                    {
                        if ( msg.what > 0 && msg.what <= 100 )
                            mCallbacks.onProgressUpdate(msg.what);
                        else
                        {
                            //extract msg and resend onwards, inefficient but preserves model
                            Bundle b = msg.getData();
                            if ( b != null )
                            {
                                if (b.containsKey("MSG"))
                                {
                                    mCallbacks.onProgressUpdate(b.getString("MSG"));
                                }
                            }
                        }
                    }
                    return true;
                }
            });
        }

        public void StartUpload(String sQRef) {
            // Create and execute the background task.
            Log.v("HRISLOG", "retained frag start upload");
            ulTask = new UploadTask(sQRef);
            ulTask.execute();
            // end of ulTask starts list task
            // so final call back is new list
        }

        public void listAll() {
            // Create and execute the background task.
            Log.v("HRISLOG", "retained frag start list");
            listTask = new ListTask();
            listTask.execute();
            // calls perent (whichever the current one is) onlistcomplete callback
        }


        /**
         * Set the callback to null so we don't accidentally leak the
         * Activity instance.
         */
        @Override
        public void onDetach() {
            super.onDetach();
            Log.v("HRISLOG", "retained fragdetatch");
            mCallbacks = null;
        }


        public class UploadTask extends AsyncTask<Void, Void, Boolean> {
            public String msQRef;
            public String mReply;

            UploadTask(String sQRef) {
                msQRef = sQRef;
                //pbProgBar.setVisibility(View.VISIBLE);
            }

            protected void onProgressUpdate(Integer... percent) {
                if (mCallbacks != null) {
                    mCallbacks.onProgressUpdate(percent[0]);
                }
            }

            protected void onProgressUpdate(String sMsg) {
                if (mCallbacks != null) {
                    mCallbacks.onProgressUpdate(sMsg);
                }
            }

            @Override
            protected void onCancelled() {
                if (mCallbacks != null) {
                    mCallbacks.onCancelled();
                }
            }


            @Override
            protected Boolean doInBackground(Void... params) {
                Log.v("HRISLOG", "doInBackground-upload ");
                ArrayList<String> toSend;
                // how many to send
                if (msQRef == null || msQRef.length() == 0) {
                    // all!
                    Log.v("HRISLOG", "doInBackground-upload ALL ");
                    onProgressUpdate("Checking for completed questionnaires");
                    toSend = cQuestionnaire.listSavedQuestionnaires( mFeedbackHandler );
                } else {
                    toSend = new ArrayList<String>(1);
                    toSend.add(0, msQRef);
                }

                // Start

                int iP = 0;
                int iMax = toSend.size();
                for (String sQRef : toSend) {
                    String sStaffChk = "";
                    // read as hashmap
                    HashMap<String, String> hm = cQuestionnaire.loadSavedQuestionnaire(sQRef);
                    // update status after read
                    onProgressUpdate((int) ((iP * 100) / iMax));

                    if (hm.containsKey(cQuestionnaire.STAFFID))
                        sStaffChk = hm.get(cQuestionnaire.STAFFID);

                    if (sStaffChk.length() > 0) {
                        // set uploaded in memory ready for upload..
                        hm.put(cQuestionnaire.UPLOADED, cUtils.getTimestamp());
                        hm.put("S", cGlobal.sessKey());
                        // do the work..
                        onProgressUpdate("Sending " + sQRef);
                        String sOut = cUtils.postAPIForm("SUBMITQ", mFeedbackHandler, hm);
                        // update the status
                        if (!cUtils.isAPIResultOK(sOut)) {
                            // failed don't save uploaded status change
                            onProgressUpdate("Error uploading " + sQRef);
                        } else {
                            // save uploaded state
                            onProgressUpdate("Uploaded " + sQRef + "ok.. saving");
                            cQuestionnaire.updateSavedQuestionnaire(sQRef, hm);
                            onProgressUpdate("Uploaded " + sQRef + " done");

                        }
                    } else {
                        onProgressUpdate("No staff set for " + sQRef);
                    }

                    // update status after done..
                    iP++;
                    hm.clear();
                }
                toSend.clear();
                if (iP == 0) {
                    onProgressUpdate( "No questionnaires to upload");
                }
                Log.v("HRISLOG", "doInBackground-upload:done " + iP);
                return true;
            }

            @Override
            protected void onPostExecute(final Boolean success) {
                Log.v("HRISLOG", "doInBackground-upload:post exec");
                ulTask = null;
                // kick off a list all
                listAll();
            }

        }


        public class ListTask extends AsyncTask<Void, Void, ArrayList<String>> {

            ListTask() {
            }

            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                Log.v("HRISLOG", "build q list in background");
                return cQuestionnaire.listSavedQuestionnaires();// ALL
            }

            @Override
            protected void onPostExecute( ArrayList<String> aNewList) {
                Log.d("HRISLOG", "List : onPostExecute ");
                listTask = null;

                if (mCallbacks != null) {
                    mCallbacks.onListComplete(aNewList);
                }

                Log.d("HRISLOG", "List : onpostexe done ");
            }

        }


    }
}

