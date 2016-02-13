package uk.co.cga.hristest;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private ListView lvQuestionnaires;
    private ArrayList<String> aQuestionnaires;
    private AdapterView.OnItemClickListener lvClickListener;
    //todo add mapping to file name from full title private HashMap<Integer, String> map_positionToId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createListenerClasses();
        lvQuestionnaires = (ListView) findViewById(R.id.lvQuestionnaires);


        aQuestionnaires = cQuestionnaire.listLocalTemplates(cGlobal.curUID());
        ArrayList<String> mDisplay = new ArrayList<String>();
        int iPos=0;
        for( String sqRef : aQuestionnaires )
        {
            // note this loads all wustionairs into memory
            cQuestionnaire cQ = new cQuestionnaire(sqRef);
            mDisplay.add(iPos, cQ.qTitle());
            iPos++;
        }
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDisplay);
        lvQuestionnaires.setAdapter(ad);
        lvQuestionnaires.setOnItemClickListener(lvClickListener);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.main_title);
        toolbar.setLogo(R.drawable.hrtest_icon);
        toolbar.setLogoDescription(R.string.app_desc);

        setSupportActionBar(toolbar);

        // log out button
        ImageButton ib = (ImageButton) findViewById(R.id.butLogout);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cGlobal.logAdjudicatorOut(getApplicationContext());
            }
        });

        // go to uploads screen
        ib = (ImageButton) findViewById(R.id.butUpload);
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(getApplicationContext(),uploadActivity.class);
                startActivity(it);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        /*
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent(getApplicationContext(),LoginActivity.class);
                // intent gets lost in the tabbed wrapper classes somewhere
                cGlobal.setPref("currQREF", "");
                startActivity(it);
             }
        });*/
    }

    private void createListenerClasses ()
    {
        lvClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Log.d("HRISLOG", "questionnaire chosen " + position);

                if (aQuestionnaires==null)
                    throw (new AssertionError("Error with mapping to questionnaire for position"));
                if (aQuestionnaires.size() < position )
                    throw (new AssertionError("Error with mapping to questionnaire for position"));
                Intent it = new Intent(getApplicationContext(), questionActivity.class);
                // intent gets lost in the tabbed wrapper classes somewhere
                String sRef = aQuestionnaires.get(position);
                cGlobal.setPref("currQREF", sRef);
                cQuestionnaire cQ = new cQuestionnaire(sRef);
                cQ.resetTemplate();

                startActivity(it);
            }
        };
    }

}
