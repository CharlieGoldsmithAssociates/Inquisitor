package uk.co.cga.hristest;
// todo add search / filter by geo
// todo add logout
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
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
    private TextView tvFeedback;
    private ListView lvStaff;
    private FloatingActionButton fabChoose;// TODO hide until selection made
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

        //fabChoose = (FloatingActionButton) findViewById(R.id.fab);
        //fabChoose.setOnClickListener(selectCandidateListener);
        // gone until staff selected
        //fabChoose.setVisibility(View.GONE);

        tvName = (TextView) findViewById(R.id.tvName );
        tvName.addTextChangedListener(nameSearchListener);

        lvStaff = (ListView) findViewById(R.id.lvStaff );
        map_positionToId = new HashMap<Integer, String>();
        //lvStaff.addHeaderView(new TextView(this,));
        lvStaff.setOnItemClickListener(staffClickListener);

        tvFeedback = (TextView) findViewById(R.id.tvFeedback );

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

                    Intent it = new Intent();// todo make this a return value activity
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
                msCandidateName = tv.getText().toString();

                // show the floating choose button
                Log.d("HRISLOG","set "+msCandidateName );

                Intent it = new Intent();// todo make this a return value activity
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
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                Log.d("HRISLOG","before text chg");

            }
            @Override
            public void afterTextChanged(Editable ed) {
                Log.d("HRISLOG","after text chg");
                ListStaff(tvName.getText().toString());
            }
        };


    }


    private boolean ListStaff(String sSearch )
    {
        sSearch = sSearch.toString().replace('.', ' ');
        sSearch = sSearch.replaceAll("[,:;]", " ");

        if (mSrchTask != null) {
            return false;
        }
        mSrchTask = new StaffSearchTask(this,sSearch);
        mSrchTask.execute((Void) null);

        return true;
    }

    public class StaffSearchTask extends AsyncTask<Void, Void, Integer>
    {
        private Context cParent;
        private String sSearch;
        private String sErrorText;

        StaffSearchTask( Context cP, String sSr ) {
            cParent = cP;
            sSearch = sSr;
            sErrorText="";
        }

        @Override
        protected Integer doInBackground(Void... params)
        {
            Log.v("HRISLOGbgs","Start BG Staff search");
            String sWhere = "";
            String[] aWords = sSearch.split(" ");
            String sWildcard = "";
            String sWCspace = "";
            sErrorText="";
            for (String sWord : aWords) {
                // possible HRIS ID ? start HR
                // HRabcNNNNNNN up to 12 in length
                if (sWord.startsWith("HR")) {
                    if (sWord.length() > 12) {
                        // error
                        sErrorText = getString(R.string.candHRIDerr);
                        sWord=sWord.substring(0, 12);
                    }

                    sWhere += " AND ID LIKE " + cDatabase.QS(sWord + "%");
                } else {
                    sWildcard += sWCspace + sWord + "%";
                    sWCspace = " ";
                }
            }
            if (sWildcard.length() > 0)
                sWhere += " AND NAME LIKE " + cDatabase.QS(sWildcard);
            Log.v("HRISLOGbgs","Search cond is "+ sWhere );

            // arraylist with ID and Name
            // horribly inefficient TODO Improve search efficiency
            // todo add search / filter by geo
            ArrayList tmpStaff = cGlobal.cdb.getArray("SELECT ID,NAME||', '||ROLE||' ['||ID||']' FROM " +
                            cDatabase.TABLE_STAFF + " WHERE UID=" +
                            cDatabase.QS(cGlobal.curUID()) +
                            sWhere +
                            "ORDER BY NAME LIMIT "+ MAXSTAFFROWS );
            if (tmpStaff.isEmpty()) {
                sErrorText = getString(R.string.candNotFound);
                return 0;
            }

            int iPos = 0;
            // rebuild aStaff
            Log.v("HRISLOGbgs","Build array and pos map "+ tmpStaff.size() );
            aStaff = new String[tmpStaff.size()];
            map_positionToId.clear();
            for (Object oRow : tmpStaff) {
                String sRow = (String) oRow;
                String[] aFlds = sRow.split("\t");
                // update the map position too
                map_positionToId.put(iPos, aFlds[0]);
                aStaff[iPos] = aFlds[1];
                iPos++;
            }
            sErrorText = "";
            return iPos;
        }// end do in bg

        @Override
        protected void onPostExecute(final Integer iMatchCount)
        {
            mSrchTask = null;

            Log.v("HRISLOGbgs", "Search done " + iMatchCount + ":" + sErrorText);
            //tvName.setError(sErrorText);
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
