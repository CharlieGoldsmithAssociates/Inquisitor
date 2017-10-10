package uk.co.cga.hristest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.StringReader;

public class loadingActivity extends AppCompatActivity {
    QDownloadTask dl=null;
    TextView mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        mStatus = (TextView) findViewById(R.id.txtDLStatus);
        dl = new QDownloadTask();
        dl.execute();
    }

    // create a handler to update the UI from background thread
    private Handler uiProgress = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null) {
                Bundle b = msg.getData();
                String sMsg = b.getString("MSG");
                if (sMsg != null) {
                    mStatus.setText(sMsg);
                    Log.v("HRISLOG", "status:" + sMsg);
                }

            }
        }
    };

    public class QDownloadTask extends AsyncTask<Void, Void, Boolean> {
        public String mReply;

        QDownloadTask() {

        }

        public void statusReport(String sReport) {
            Bundle b = new Bundle(1);
            b.putString("MSG", sReport);
            Message msg = new Message();
            msg.setData(b);
            uiProgress.sendMessage(msg);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean bAllOk = true;
            int iAdmin =  Integer.parseInt(cGlobal.getPref("ADMINLOGIN","0"));
            Log.v("HRISLOG", "doInBackground-load");

            cQuestionnaire.publishAllQuestionnaires(cGlobal.curUID(), uiProgress);



            if (bAllOk) {
                if (!getStaffList(false)){
                        bAllOk = false;
                }
            }



            if ( bAllOk ) {
                statusReport("Welcome " + cGlobal.curAdjudicatorName() + " : loading");
                cQuestionnaire.getAllQuestionnaires(cGlobal.curUID(), uiProgress);
            }

            return bAllOk;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success)
            {
                // move on to next activity..
                Intent it = new Intent(getApplicationContext(), MainActivity.class);
                it.putExtra("UID", cGlobal.rUser[0]);
                startActivity(it);


            } else {
                Intent it = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(it);
            }
        }


        public boolean getStaffList ( boolean bForce )
        {
            // user rec is UID \t	Name \t	<VIS> \t <MDA> \t <rad session key>
            String sSessKey =  cGlobal.sessKey();
            String sUID =  cGlobal.curUID();

            long lLast = cGlobal.getPref("LASTSTAFFCHK", 0);
            long lNow = System.currentTimeMillis()/1000;
            if ( (lNow-lLast) > (60*60*1) )
                bForce=true;

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
                    cGlobal.cdb.startTransaction();
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
                    cGlobal.cdb.endTransaction();
                    Log.v("HRISLOG", "getStaffList: parse reply: done " + iCount);
                    statusReport("Welcome " + cGlobal.curAdjudicatorName() + "\nStaff Data: complete");
                    // store time of update
                    cGlobal.setPref("LASTSTAFFCHK", String.format("%d",lNow));
                }
                catch ( Exception e )
                {
                    cGlobal.cdb.abandonTransaction();
                    Log.e("HRISLOG","Error reading list-staff reply from host "+ e.getMessage());
                    statusReport("Requesting User Data: Error");

                    return false;
                }
                finally
                {
                    cGlobal.cdb.endTransaction();
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
