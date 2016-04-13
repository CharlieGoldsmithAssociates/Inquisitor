package uk.co.cga.hristest;
// todo add search / filter by geo
// todo add logout
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class CandidateActivity extends AppCompatActivity {
    private static final Integer MAXSTAFFROWS=100 ;
    public  String msHRISID;
    public  String msCandidateName;
    private String[] aStaff;
    private HashMap<Integer, String> map_positionToId;
    private TextView tvName;
    private TextView tvLoc;
    private TextView tvFeedback;
    private ListView lvStaff;
   // private FloatingActionButton fabChoose;
    private StaffSearchTask mSrchTask = null;
    private View.OnClickListener logoutListener;
    private View.OnClickListener selectCandidateListener;
    private AdapterView.OnItemClickListener staffClickListener;
    private TextWatcher nameSearchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate);

        CreateListenerClasses();
        
        msHRISID = "";
        msCandidateName="";

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_candidate);
        toolbar.setLogo(R.drawable.hrtest_icon);
        toolbar.setLogoDescription(R.string.app_desc);
        toolbar.setNavigationIcon(R.drawable.logout);
        toolbar.setNavigationContentDescription(R.string.logout_title);
        toolbar.setNavigationOnClickListener(logoutListener);
        
        setSupportActionBar(toolbar);


        tvFeedback = (TextView) findViewById(R.id.tvFeedback );
        tvFeedback.setVisibility(View.GONE);

        tvName = (TextView) findViewById(R.id.tvName );
        tvLoc = (TextView) findViewById(R.id.tvLocation);
        lvStaff = (ListView) findViewById(R.id.lvStaff );
        map_positionToId = new HashMap<Integer, String>();
        //lvStaff.addHeaderView(new TextView(this,));

        String sTmp = cGlobal.getPref("HRISID");
        if ( sTmp.length() > 0) {
            tvName.setText(sTmp); // calls ListStaff via ontextchange
        }
        sTmp = cGlobal.getPref("LASTLOCSTRING");
        if ( sTmp.length() > 0) {
            tvLoc.setTextColor(Color.YELLOW);
            tvLoc.setText(sTmp); // calls ListStaff via ontextchange
        }
        tvName.addTextChangedListener(nameSearchListener);
        tvLoc.addTextChangedListener(nameSearchListener);
        lvStaff.setOnItemClickListener(staffClickListener);
        ListStaff();
    }

    public void CreateListenerClasses () 
    {
        logoutListener = new View.OnClickListener() {
            public void onClick(View view) {
                // clear the current user and go back to login
                Log.d("HRISLOG","logout click");
                cGlobal.logAdjudicatorOut(view.getContext());
            }
        };
        
        /*selectCandidateListener= new View.OnClickListener() {
            public void onClick(View view) {
                Log.d("HRISLOG","selectCandidate button click");
                if (msHRISID.length() > 1) {

                    Intent it = new Intent();
                    // intent gets lost in the tabbed wrapper classes somewhere
                    it.putExtra("HRISID", msHRISID);
                    it.putExtra("CANDIDATE", msCandidateName);

                    cGlobal.setPref("HRISID", msHRISID);
                    cGlobal.setPref("CANDIDATE", msCandidateName);
                    setResult(Activity.RESULT_OK,it);
                    finish();
                    //startActivity(it);
                } else {
                    // error - should not be possible
                    // we should not be shown until it is set
                    throw new AssertionError("Error click without ID Set: button should be hidden");
                }


            }
        };*/

        // Create a message handling object as an anonymous class.
        staffClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Log.d("HRISLOG","staff item click "+ position);

                if (!map_positionToId.containsKey(position))
                    throw (new AssertionError("Error with mapping to hris id for position"));

                msHRISID = map_positionToId.get(position);
                TextView tv = (TextView) v;
                String sTmp = tv.getText().toString();
                String[] sLines = sTmp.split("\n");
                msCandidateName = sLines[0];


                Log.d("HRISLOG","set "+msCandidateName );

                Intent it = new Intent();
                // intent gets lost in the tabbed wrapper classes somewhere
                it.putExtra("HRISID", msHRISID);
                it.putExtra("CANDIDATE", msCandidateName);

                cGlobal.setPref("HRISID", msHRISID);
                cGlobal.setPref("CANDIDATE", msCandidateName);
                setResult(Activity.RESULT_OK,it);
                finish();

                //fabChoose.setVisibility(View.VISIBLE);
                // show a cutsy prompt to say click the button
               /*Snackbar.make(parent,
                        "Click button to start assessment for " + msCandidateName,
                        Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        };

        nameSearchListener = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("HRISLOG","on txt chg");
                tvFeedback.setVisibility(View.GONE);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                Log.d("HRISLOG","before text chg");

            }
            @Override
            public void afterTextChanged(Editable ed) {
                Log.d("HRISLOG","after text chg");
                ListStaff();
            }
        };


    }


    private boolean ListStaff()
    {
        if ( tvName == null || tvLoc == null || mSrchTask != null ) return false;

        String sStaff = tvName.getText().toString();
        sStaff = sStaff.replace('.', ' ');
        sStaff = sStaff.replaceAll("[,:;]", " ");

        String sLoc = tvLoc.getText().toString();
        sLoc = sLoc.replace('.', ' ');
        sLoc = sLoc.replaceAll("[,:;]", " ");

        // store last used location string so we can preserve it
        // for next call which is most likely the same loc
        cGlobal.setPref("LASTLOCSTRING",sLoc);

        if (mSrchTask != null) {
            return false;
        }
        mSrchTask = new StaffSearchTask(this,sStaff,sLoc);
        mSrchTask.execute((Void) null);

        return true;
    }

    public class StaffSearchTask extends AsyncTask<Void, Void, Integer>
    {
        private Context cParent;
        private String sSearchStaff;
        private String sSearchLoc;
        private String sErrorText;

        StaffSearchTask( Context cP, String sStaff, String sLoc ) {
            cParent = cP;
            sSearchStaff = sStaff;
            sSearchLoc = sLoc;
            sErrorText="";
        }

        @Override
        protected Integer doInBackground(Void... params)
        {
            int iPos = 0;

            Log.v("HRISLOGbgs","Start BG Staff search");
            try {
                String sWhere = "";
                String[] aWords = sSearchStaff.split(" ");
                String sWildcard = "";
                String sWCspace = "";

                Boolean bIsID=false;

                sErrorText="";
                for (String sWord : aWords) {
                    // possible HRIS ID ? start SR
                    // SRabcNNNNNNN up to 12 in length
                    if (sWord.startsWith("SR") && sWord.length() < 13 )
                    {

                        sWhere += " AND S.ID LIKE " + cDatabase.QS(sWord + "%");
                        bIsID=(sWord.length()==12);
                    }
                    else
                    {
                        sWildcard += sWCspace + sWord + "%";
                        sWCspace = " ";
                        bIsID=false;
                    }
                }
                if (sWildcard.length() > 1)
                    sWhere += " AND S.NAME LIKE " + cDatabase.QS(sWildcard);

                if ( sSearchLoc.length() > 0 && ! bIsID )
                {
                    sWildcard =cDatabase.QS( sSearchLoc + "%");
                    sWhere += " AND ( "+
                            "F.NAME LIKE " + sWildcard+
                            "OR F.GNM1 LIKE " + sWildcard+
                            "OR F.GNM2 LIKE " + sWildcard+
                            "OR F.GNM3 LIKE " + sWildcard +
                            ") ";
                }
                Log.v("HRISLOGbgs","Search cond is "+ sWhere );

                // arraylist with ID and Name
                String sQUID =cDatabase.QS(cGlobal.curUID());
                ArrayList tmpStaff = cGlobal.cdb.getArray("SELECT S.ID,S.NAME||', '||S.ROLE||' ['||S.ID||']',F.NAME,F.GNM1,F.GNM2,F.GNM3 FROM " +
                                cDatabase.TABLE_STAFF + " AS S "+
                                " INNER JOIN "+ cDatabase.TABLE_FACILITY + " AS F ON S.FAC = F.ID "+
                                " WHERE S.UID=" + sQUID + " AND F.UID=" + sQUID +
                                sWhere +
                                "ORDER BY S.NAME LIMIT "+ MAXSTAFFROWS );
                if (tmpStaff.isEmpty()) {
                    sErrorText = getString(R.string.candNotFound);
                    return 0;
                }

                // rebuild aStaff
                Log.v("HRISLOGbgs","Build array and pos map "+ tmpStaff.size() );
                aStaff = new String[tmpStaff.size()];
                map_positionToId.clear();
                for (Object oRow : tmpStaff) {
                    String sRow = (String) oRow;
                    String[] aFlds = sRow.split("\t");
                    // update the map position too
                    map_positionToId.put(iPos, aFlds[0]);
                    aStaff[iPos] = aFlds[1] +
                            "\n" + aFlds[2] +
                            ", " + aFlds[3] +
                            ", " + aFlds[4] +
                            ", " + aFlds[5] ;
                    iPos++;
                }
                sErrorText = "";
                Log.v("HRISLOGbgs","Build array and pos map Done " );
            } catch (Exception e) {
                sErrorText = "An error occurred during search." + e.getMessage();

            }
            return iPos;
        }// end do in bg

        @Override
        protected void onPostExecute(final Integer iMatchCount)
        {
            mSrchTask = null;

            Log.v("HRISLOGbgs", "Search done " + iMatchCount + ":" + sErrorText);
            //tvName.setError(sErrorText);
            tvFeedback.setVisibility(View.VISIBLE);
            if ( iMatchCount == 0 )
            {
                tvFeedback.setText(sErrorText);
            }
            else
            {
                if ( iMatchCount == MAXSTAFFROWS)
                {
                    tvFeedback.setText("more than 100 staff match the query");
                }
                else
                {
                    tvFeedback.setText(String.format(getString(R.string.candFound), iMatchCount));
                }
                // create the new adapter and set the list
                ArrayAdapter<String> adNew = new ArrayAdapter<>(cParent,
                        android.R.layout.simple_list_item_1, aStaff);
                lvStaff.setAdapter(adNew);

                lvStaff.setVisibility(View.VISIBLE);

            }

        }// end onpostexec

    }// end staffsearch class

}
