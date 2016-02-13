package uk.co.cga.hristest;

import java.sql.Ref;
import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class uploadActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_upload);
        toolbar.setLogo(R.drawable.hrtest_icon);
        toolbar.setLogoDescription(R.string.app_desc);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // UPLOAD SELECTED..
                Log.d("HRISLOG", "Upload selected line");
                // ToDO upload selected line
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });

        ImageButton imb = (ImageButton)findViewById(R.id.butNew);
        imb.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // move back to main
                Log.d("HRISLOG", "Start new questionnaire");

                Intent it = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(it);
            }
        });

        imb = (ImageButton)findViewById(R.id.butUpload);
        imb.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // move baxk to main
                Log.d("HRISLOG", "Start upload");

                DoUpload();
            }
        });

        imb = (ImageButton)findViewById(R.id.butPurge);
        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // move baxk to main
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

        imb = (ImageButton)findViewById(R.id.butAddStaff);
        imb.setVisibility(View.INVISIBLE);
        imb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // retrospectively add staff
                Log.d("HRISLOG", "Add staff id for questionnaire");

            }
        });

        ListView lvQuestionnaires = (ListView)findViewById(R.id.lvQuestionnaires);
        lvQuestionnaires.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id)
            {
                // save the questionnaire
                Log.d("HRISLOG", "Show Select questionnaire detail.."+position );

            }
        });

        // rebuild the list
        RefreshQList();


    }

    public void RefreshQList()
    {
        try {
            ListView lvQuestionnaires = (ListView)findViewById(R.id.lvQuestionnaires);
            ArrayList<String> aQList = cQuestionnaire.listSavedQuestionnaires(false);
            // android.R.layout.simple_list_item_1
            QListAdapter ad = new QListAdapter(this, R.layout.layoutsavedq, aQList);
            lvQuestionnaires.setAdapter(ad);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void DoUpload()
    {
        Log.d("HRISLOG","do upload ");
        // ToDO upload selected line

    }

    public class QListAdapter extends ArrayAdapter{

        private int resource;
        private LayoutInflater inflater;
        private Context context;

        public QListAdapter ( Context ctx, int resourceId, List objects) {

            super( ctx, resourceId, objects );
            resource = resourceId;
            inflater = LayoutInflater.from( ctx );
            context=ctx;
        }

        @Override
        public View getView ( int position, View convertView, ViewGroup parent ) {

            /* create a new view of my layout and inflate it in the row */
            convertView = (LinearLayout) inflater.inflate( resource, null );

            /* Extract the city's object to show */
            String qRef = (String)getItem(position);

            HashMap<String,String> Q = cQuestionnaire.loadSavedQuestionnaire(qRef);// load from saved store
            /* Take the TextView from layout and set the city's name */
            TextView txtTmp = (TextView) convertView.findViewById(R.id.qTitle);
            String sTmp =Q.get(cQuestionnaire.TITLE);
            if ( sTmp == null || sTmp.length()>0 )sTmp ="Error no title for q ref:"+qRef;
            txtTmp.setText(sTmp);

            String sDescription = Q.get( cQuestionnaire.SUMMARY);
            if ( sDescription==null || sDescription.length()==0 ) sDescription = "old questionnaire no summary info ";
            txtTmp = (TextView) convertView.findViewById(R.id.qDescription);
            txtTmp.setText(sDescription);

            // store the reference for this line for each of processing later
            txtTmp = (TextView) convertView.findViewById(R.id.tvReference);
            txtTmp.setText(qRef);
            txtTmp.setVisibility(View.GONE);

            if (Q.containsKey( cQuestionnaire.UPLOADED) &&
                    Q.get(cQuestionnaire.UPLOADED).length()>1)
            {
                convertView.setBackgroundColor(Color.GREEN);
            }

            return convertView;
        }
    }
}
