package uk.co.cga.hristest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Howard on 08/02/2016.
 */
public class cQuestionnaire {
    static public String PATH=cGlobal.main_Context.getFilesDir() + "/questionnaire/"; //put the downloaded file here;
    public static final String TITLE = "TITLE";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String TEXT = "TEXT";
    public static final String FOOT = "FOOT";
    public static final String IMG = "IMG";
    public static final String QUESTION = "QUESTION";
    public static final String NO_QUESTIONS = "NO_QUESTIONS";
    public static final String NO_ANSWERS="NO_ANSWERS";
    public static final String PROMPT="PROMPT";
    public static final String CHOICE="CHOICE";
    public static final String CORRECT="CORRECT";
    public static final String QSTUSED="QSTUSED";
    public static final String STARTTIME= "STARTTIME";
    public static final String START= "START";
    public static final String ENDTIME= "ENDTIME";
    public static final String END= "END";
    public static final String TIMEALLOWED="TIMEALLOWED";
    public static final String STAFFID="STAFFID";
    public static final String STAFFNAME="STAFFNAME";
    public static final String SAVED="SAVED"   ;
    public static final String SAVEFILE="SAVEFILE"   ;
    public static final String UPLOADED="UPLOADED"   ;
    public static final String OPTIONS="OPTIONS"   ;
    public static final String LOADED="LOADED"   ;
    public static final String SUMMARY="SUMMARY";
    public static final String MARK="MARK";
    public static final String UID="UID";
    public static final String ADJUCICATOR="ADJUCICATOR";

    public String msQRef;

    // start new questionnaire from template


    // reload - copy - it's just a wrapper round setpref/getpref
    cQuestionnaire ( String sQRef) {
        if (!isLoaded(sQRef))
            LoadTemplate(sQRef);
        msQRef=sQRef;
    }

    public void setStaff ( String sStaffID, String sName )
    {
        cGlobal.setPref(msQRef + STAFFID, sStaffID);
        cGlobal.setPref(msQRef + STAFFNAME, sName);
    }
    public void setStaffID ( String sStaffID)
    {
        cGlobal.setPref(msQRef + STAFFID, sStaffID);
    }
    public void setStaffName ( String sStaff)
    {
        cGlobal.setPref(msQRef + STAFFNAME, sStaff);
    }
    public String getStaffID ( )
    {
        return cGlobal.getPref(msQRef + STAFFID,"");
    }
    public String getStaffName ( )
    {
        return cGlobal.getPref(msQRef + STAFFNAME,"");
    }

    public void Start()
    {
        long tStart = System.currentTimeMillis()/1000;
        cGlobal.setPref( msQRef + STARTTIME, String.format("%d", tStart));
        String sTS =  cUtils.getTimestamp();
        cGlobal.setPref( msQRef + START,  sTS );
        Log.d("HRISLOG", "Start at " + sTS );

        Random R = new Random();
        for (int iQ=1; iQ < noQuestions(); iQ++)
        {
            // number of options 
            int iOptions = Integer.parseInt(
                    cGlobal.getPref( msQRef+ String.format(OPTIONS+"_%2d",iQ),"0" ) );
            int iChosenQ=1;
            if ( iOptions> 1 )
                iChosenQ = 1+R.nextInt(iOptions);
            // and store chosen
            Log.v("HRISLOG","Choose question "+iQ+" Choices="+iOptions + " Chosen="+ iChosenQ);
            cGlobal.setPref(msQRef + String.format(QSTUSED+"_%02d", iQ), String.format("%02d",iChosenQ) );
        }

        // store adjudicator for reference
        cGlobal.setPref(msQRef + UID , cGlobal.curUID());
        cGlobal.setPref(msQRef + ADJUCICATOR , cGlobal.curAdjudicatorName());
    }

    public void End ()
    {
        long tStart = System.currentTimeMillis()/1000;
        cGlobal.setPref(msQRef + ENDTIME, String.format("%d", tStart));
        String sTS = cUtils.getTimestamp();
        cGlobal.setPref(msQRef + END, sTS);
        Log.d("HRISLOG", "End at " + sTS);

        cGlobal.setPref(msQRef + SAVED, "");
        cGlobal.setPref(msQRef + UPLOADED, "");

        // mark the questions/ build the summary text
        doSummary(null,true); // calls doMark, updates MARK & SUMMARY Tags

        saveQuestionnaire(false);
        // next stage - nect candidate/ back to select questionnaire
    }

    public Boolean isStarted()
    {
        long tStart = (long) Integer.parseInt( cGlobal.getPref( msQRef + STARTTIME, "0"));

        return ( tStart != 0 );
    }

    public long secondsRemaining()
    {

        Integer i = Integer.parseInt( cGlobal.getPref( msQRef + TIMEALLOWED, "0"));
        long tStart = 1000*(long) Integer.parseInt( cGlobal.getPref( msQRef + STARTTIME, "0"));
        if ( tStart==0 || i == 0 ) return 9999;

        long tEnd = tStart + ((long)i * 1000 );
        long tNow = System.currentTimeMillis();
        return (tEnd - tNow)/1000;
    }

    // are all questions answered
    public Boolean IsComplete ()
    {
        Boolean bReply = true;
        int iQ;
        for(iQ=1; iQ <= noQuestions() ; iQ++ )
        {
            if ( getChosenAnswer(iQ) == -1 )
                bReply = false;
        }
        return bReply;
    }

    public boolean hasStaffDetails () {
        return hasStaffDetails( msQRef );
    }
    public boolean hasStaffDetails (String sQRef)
    {
        String sSID = cGlobal.getPref(sQRef + STAFFID,"");
        return ( sSID.length()>0);
    }

    public boolean isLoaded( )
    {
        return isLoaded(msQRef);
    }
    public boolean isLoaded(String sQRef )

    {
        String sL = cGlobal.getPref(sQRef + LOADED,"");
        return ( sL.length()>0);
    }


    // ---------- Generic per-questionnair value recall functions
    public Integer noQuestions()
    {
        return cGlobal.getPref(msQRef+NO_QUESTIONS,0);

    }

    public String qTitle()
    {
        return cGlobal.getPref(msQRef + TITLE, "title not set");
    }

    public String qDescription()
    {
        return cGlobal.getPref(msQRef + DESCRIPTION, "description not set");
    }

    // ---------- Per quesiton data recall function
    // that all use chosequestion ref to disambiguate which quesiton was chosen by start

    private String chosenQuestionRef( int iQuestion )
    {
        String sTmp=String.format(QSTUSED+"_%02d", iQuestion);
        String sSubQ =  cGlobal.getPref(msQRef + sTmp, "01");
        return String.format("%02d_", iQuestion)+sSubQ;
    }

    public String qTitle(int iQuestion )
    {
        return cGlobal.getPref(msQRef+TITLE+"_"+chosenQuestionRef(iQuestion), String.format("Question %d",iQuestion));
    }

    public String qText(int iQuestion )
    {
        return cGlobal.getPref(msQRef + TEXT+"_" +chosenQuestionRef(iQuestion), "");
    }

    public String qFooter(int iQuestion )
    {
        return cGlobal.getPref(msQRef + FOOT+"_" + chosenQuestionRef(iQuestion), "");
    }

    public String qImagePath(int iQuestion )
    {
        return cGlobal.getPref(msQRef + IMG + "_" + chosenQuestionRef(iQuestion), "");
    }

    public Integer qNoAnswers(int iQuestion )
    {
        return Integer.parseInt(cGlobal.getPref(msQRef + NO_ANSWERS + "_" + chosenQuestionRef(iQuestion), "0"));
    }

    public String qAnswerPrompt (int iQuestion, int iAnswer )
    {
        return cGlobal.getPref(msQRef + PROMPT + "_" + iAnswer + "_" + chosenQuestionRef(iQuestion), "");
    }


    //--------------------------------
    // user choice storage - done by sub question for ease of marking
    public int getChosenAnswer (int iQuestion )
    {
        return Integer.parseInt(cGlobal.getPref(msQRef + CHOICE + "_" + chosenQuestionRef(iQuestion), "-1"));
    }

    public void setChosenAnswer (int iQuestion , int iAnswer )
    {
        cGlobal.setPref(msQRef+CHOICE+"_"+chosenQuestionRef(iQuestion),String.format("%d",iAnswer));
    }

    public void setCorrectAnswer (int iQuestion , int iAnswer )
    {
        cGlobal.setPref(msQRef+CORRECT+"_"+chosenQuestionRef(iQuestion),String.format("%d",iAnswer));
    }


    // nb save as completed questionnair in saved/ folder
    // not as a template : use sabeTemplate for that
    public Boolean saveQuestionnaire ( Boolean bOverwrite )
    {
        Boolean bOk = false;
        // get a unique file name to save  qref + timestamp
        try {
            String sFile = qGetFileNameFromTemplate(msQRef);
            Log.i("HRISLOG","Save questionnaire to "+sFile);
            File fTemp = new File(sFile);
            if (fTemp.exists())
            {
                Log.i("HRISLOG","Save questionnaire file already exists "+sFile);
                if ( bOverwrite )
                    fTemp.delete();
                else
                    throw new AssertionError("Cannot save file already exists");
            }

            FileWriter fw = new FileWriter(fTemp);

            // mark as saved..
            cGlobal.setPref( msQRef+SAVED, cUtils.getTimestamp()  );
            cGlobal.setPref( msQRef+SAVEFILE, sFile  );

            //eek must be a better way
            Map<String, ?> cfgAll = cGlobal.sp.getAll();
            for (String sKey : cfgAll.keySet()) {
                if (sKey.startsWith(msQRef)) {
                    String sValue = cfgAll.get(sKey).toString();
                    Log.v("HRISLOG", "Save " + sKey + "=" + sValue);
                    fw.write( sKey+"="+sValue+"\n");
                }
            }
            fw.close();
            bOk=true;
        }
        catch( Exception e )
        {
            Log.e("HRISLOG","Error saving "+e.getMessage());
            bOk = false;
        }

        return bOk;

    }

    public static void saveTemplate(String sQRef, String sQText)
    {
        String sFile =qTemplateFileName(sQRef);

        // extract the image files as a list we can download
        String[] aLines = sQText.split("\n");
        try {
            for (String sLine : aLines)
            {
                int iPos = sLine.indexOf('=');
                if (iPos>0) {
                    String sKey = sLine.substring(0, iPos).toUpperCase().trim();
                    String sValue = sLine.substring(iPos + 1).trim();
                    if (sKey.contains(IMG+"_")) {
                        imageManager.DownloadFromUrl(sValue);
                    }
                }
            }


            File fTemp = new File(sFile);
            if ( fTemp.exists()) fTemp.delete();
            FileWriter fw = new FileWriter(fTemp);
            fw.write(sQText);
            fw.close();

            Log.i("HRISLOG", "File write " + sFile + " data len=" + sQText.length() + " file len=" + fTemp.length());
            // and force reload
            cGlobal.setPref(sQRef + LOADED, "");// might be a reload, but the file has changed so force it
        }
        catch (Exception ex )
        {
            Log.e("HRISLOG","Cannot save questionnaire file " + sFile);
        }
    }

    private  static String qTemplateFileName( String sBase )
    {
        String sSafeFileName = sBase.replaceAll("\\W+", ".");
        String sPath =PATH + "templates/"+ cGlobal.curUID() + "/";
        File fPath = new File(sPath);
        fPath.mkdirs();
        String sFile = sPath + sSafeFileName + ".hrisq";
        return sFile;
    }

    private  static String qGetFileNameFromTemplate( String sQRef ) {
        // reference and end timestamp
        String sEndTime = cGlobal.getPref(sQRef + ENDTIME,
                String.format("%d", System.currentTimeMillis() / 1000)
        );
        String sSafeFileName = sQRef.replaceAll("\\W+", ".") + "_"+ sEndTime;
        return qQuestionnaireFileName( sSafeFileName);
    }

    private  static String qQuestionnaireFileName( String sSavedQRef )
    {
        // filebase saved/ sQRef_1602121032
        String sPath =PATH + "save/";
        File fPath = new File(sPath);
        fPath.mkdirs();
        String sFile = sPath + sSavedQRef + ".hrisq";
        return sFile;
    }


    // remove any answer records etc.  from the memory for this template
    public void resetTemplate()
    {
        cGlobal.unsetPref(msQRef + STARTTIME);
        cGlobal.unsetPref(msQRef + START);
        cGlobal.unsetPref(msQRef + ENDTIME);
        cGlobal.unsetPref(msQRef + END);
        cGlobal.unsetPref(msQRef + SAVED);
        cGlobal.unsetPref(msQRef + MARK);
        cGlobal.unsetPref(msQRef + SUMMARY);
        cGlobal.unsetPref(msQRef + UID);
        cGlobal.unsetPref(msQRef + SAVEFILE );
        cGlobal.unsetPref(msQRef + UPLOADED );
        cGlobal.unsetPref(msQRef + STAFFID );
        cGlobal.unsetPref(msQRef + STAFFNAME );
        int iQ;
        for( iQ=1;iQ<= noQuestions(); iQ++)
        {
            setChosenAnswer(iQ,-1);
            // and reset chose quesiton
            cGlobal.unsetPref(msQRef + String.format(QSTUSED+"_%02d", iQ));
        }

    }

    public static ArrayList<String> listSavedQuestionnaires (Boolean bUploadedOnly )
    {
        String sPath =PATH + "save/";
        File fSpec = new File(sPath);
        ArrayList<String> aReply = new ArrayList<>();

        for ( File f : fSpec.listFiles() )
        {
            if ( f.isFile() )
            {
                String sFile = f.getName();
                if ( sFile.endsWith(".hrisq")) {
                    Boolean bAdd = true;
                    String sSavedQRef = sFile.substring(0, sFile.length() - 6);
                    if ( bUploadedOnly )
                    {
                        // more work, read file and check if uploaded time is set
                        HashMap<String,String> hm = loadSavedQuestionnaire(sSavedQRef);
                        if (hm != null && hm.containsKey(UPLOADED))
                            bAdd = true;
                        else
                            bAdd= false;
                    }
                    if ( bAdd )
                        aReply.add(sSavedQRef);
                }
            }
        }

        return aReply;
    }

    public static int PurgeAllUploaded ()
    {
        int iCount=0;
        ArrayList<String> aQList = listSavedQuestionnaires(true);
        for( String sSavedQRef : aQList )
        {
            if ( DeleteSavedQuestionnaire(sSavedQRef) )
                iCount++;
        }
        return iCount;
    }

    public static Boolean DeleteSavedQuestionnaire ( String sSavedQRef)
    {

        Boolean bReply = false;
        String sFile = qQuestionnaireFileName(sSavedQRef);
        try {

            File fTemp = new File(sFile);
            if (!fTemp.exists())
                return true;

            fTemp.delete();
            bReply = true;
        } catch (Exception e) {
            Log.e("HRISLOG", "Cannot delete saved file " + sFile + " " + e.getMessage());
        }
        return bReply;
    }

    // read saved questionnaire
    public static HashMap<String,String> loadSavedQuestionnaire(String sSavedQRef)
    {
        HashMap<String,String> hReply= new HashMap<String,String>();
        // saved qref = qref_timestamp to be loaded from the saved path

        // load the core details
        String sFile = qQuestionnaireFileName(sSavedQRef);
        try {

            File fTemp = new File(sFile);
            if ( ! fTemp.exists() )
            {
                // can't download it .. we're in main thread..
                // downloadQuestionnaire(sQRef);
                Log.e("HRISLOG","Cannot open saved questionnaire file " + sFile);
                return null;
            }
            FileReader fr = new FileReader(fTemp);
            BufferedReader sr = new BufferedReader(fr);
            String sLine;
            while ( (sLine = sr.readLine()) != null )
            {
                // key = value
                int iPos = sLine.indexOf('=');
                if ( iPos>0) {
                    // don't split we only want first =
                    String sKey = sLine.substring(0, iPos).toUpperCase().trim();
                    String sValue = sLine.substring(iPos + 1).trim();
                    // no need to decode # questions etc
                    hReply.put(sKey,sValue);
                }
            }

            sr.close();
            fr.close();
        }
        catch (Exception ex )
        {
            Log.e("HRISLOG","Cannot open questionnaire file " + sFile);
        }

        return hReply;
    }

    private String doMarking( String sQRef)
    {
        int iTotal=0;
        String sSummary="";
        for ( int iQ=0; iQ < noQuestions();iQ++)
        {
            sSummary += String.format( "Q%d=",iQ);
            int iChosen = cGlobal.getPref(sQRef + CHOICE, -1);
            int iCorrect= cGlobal.getPref(sQRef + CORRECT, -1);

            if ( iChosen >=0  ) {
                if( iChosen==iCorrect ) {
                    iTotal++;
                    sSummary += "Correct,";
                }
                else
                {
                    sSummary += "Wrong,";
                }
            }
            else
            {
                sSummary += "No Answer,";
            }
        }

        cGlobal.setPref(sQRef + MARK, String.format("%d",iTotal));
        return sSummary;
    }

    private String doSummary( String sQRef,  Boolean bForce)
    {
        if ( sQRef == null ) sQRef = msQRef;

        String sSummary = cGlobal.getPref(sQRef + SUMMARY, "");
        if ( sSummary.length()>0 && !bForce) return sSummary;// already in store.

        // summary line for the saved list
        sSummary = cGlobal.getPref(sQRef + STAFFNAME, "");
        if ( sSummary.length()==0) sSummary="No staff member set yet";
        sSummary += "["+ cGlobal.getPref(sQRef + STAFFID, "")+"]";
        sSummary += "\n"+ doMarking(sQRef);
        sSummary += "\n Started "+ cGlobal.getPref(sQRef+ STARTTIME,"");
        long lEnd = cGlobal.getPref(sQRef+ END,0); // millisec/1000 = s
        long lStart = cGlobal.getPref(sQRef+ START,0);
        long lDur = lEnd-lStart;
        long iM = lDur/60;
        long iS = lDur - (iM*60);
        sSummary += "  Time:"+ String.format("02d:%02d",iM,iS);
        sSummary += "\n Adjudicator " + cGlobal.getPref(sQRef + ADJUCICATOR, "");
        sSummary += " ["+ cGlobal.getPref(sQRef + UID, "") + "]";

        cGlobal.setPref(sQRef + SUMMARY, sSummary);
        return sSummary;
    }

    // load a temnplate from file if not already in sharedprefs
    // and clear any operational/ live values
    private void LoadTemplate(String sQRef)
    {
        if ( cGlobal.getPref(sQRef + LOADED, "").length()>0 ) return;// already in store.

        // load the core details
        String sFile = qTemplateFileName(sQRef);
        String sCurrentQRef = "";
        HashMap<Integer,Integer> hmOptionCount = new HashMap<Integer,Integer>();

        try {
            
            File fTemp = new File(sFile);
            if ( ! fTemp.exists() )
            {
                // can't download it .. we're in main thread..
                // downloadQuestionnaire(sQRef);
                Log.e("HRISLOG","Cannot open questionnaire file " + sFile);
                return;
            }
            FileReader fr = new FileReader(fTemp);
            BufferedReader sr = new BufferedReader(fr);
            String sLine;
            while ( (sLine = sr.readLine()) != null )
            {
                // key = value
                int iPos = sLine.indexOf('=');
                if ( iPos>0)
                {
                    // don't split we only want first =
                    String sKey = sLine.substring(0,iPos).toUpperCase().trim();
                    String sValue = sLine.substring(iPos+1).trim();

                    // maintain a running question ref
                    if ( sKey.equals(QUESTION) )
                    {
                        int iQ = Integer.parseInt(sValue);
                        int iSubQ = 1;
                        if( hmOptionCount.containsKey(iQ) )
                            iSubQ = (hmOptionCount.get(iQ) + 1);

                        hmOptionCount.put(iQ, iSubQ);
                        sCurrentQRef = String.format("_%02d_%02d", iQ,iSubQ);
                    }
                    else
                        cGlobal.setPref( sQRef+ sKey + sCurrentQRef , sValue );
                }
            }
            
            sr.close();
            fr.close();
            
            // and store the map of sub quesiton counts as well
            for(Integer iQ: hmOptionCount.keySet())
            {
                cGlobal.setPref( sQRef+ String.format(OPTIONS+"_%02d",iQ), String.format("%d", hmOptionCount.get(iQ)) );
            }

            resetTemplate();
            cGlobal.setPref(sQRef + LOADED, "1");
        }
        catch (Exception ex )
        {
            Log.e("HRISLOG","Cannot open questionnaire file " + sFile);
        }


    }


    static public String downloadQuestionnaire (String sQRef)
    {
        // get a template
        /*
        String sQ = cUtils.getAPIResult("GETQ", "?Q=" + sQRef);
        if ( !cUtils.isAPIResultOK(sQ))
            throw new AssertionError("Error downloading quesionnaire template "+ sQ );*/

        String sTemplate = cUtils.getURL(cGlobal.getString(R.string.qurl) + sQRef + ".hrisq");
        if ( sTemplate.length() >0 )
            cQuestionnaire.saveTemplate(sQRef, sTemplate);
        return sTemplate;
    }

    static public ArrayList<String> listLocalTemplates ( String SUID )
    {
        String sPath =PATH + cGlobal.curUID() ;
        File fSpec = new File(sPath);
        ArrayList<String> aReply = new ArrayList<>();

        for ( File f : fSpec.listFiles() )
        {
            if ( f.isFile() )
            {
                String sFile = f.getName();
                if ( sFile.endsWith(".hrisq"))
                    aReply.add( sFile.substring(0,sFile.length()-6 ));
            }
        }
        return aReply;
    }

    static public ArrayList<String> getServerListQuestionnaires (String sUID)
    {
        // get all templates
        ArrayList<String> aReply = new ArrayList<String>();
        String sList = cUtils.getAPIResult("LISTQ", "&UID=" + sUID + "&S="+cGlobal.sessKey() );
        if ( cUtils.isAPIResultOK(sList)) {

            sList = cUtils.getAPIResulttext(sList);
            for (String s : sList.split("\n")) {
                aReply.add(s);
            }
        }
        return aReply;
    }


    static public int getAllQuestionnaires (String sUID)
    {
        // list all templates
        int iCount = 0;

        ArrayList<String> aList = getServerListQuestionnaires(sUID);
        ArrayList<String> aRemoteFiles = new ArrayList<String>();
        if ( aList.size() > 0 ) {
            // check for new/ changed ones..
            for (String sLine :aList) {
                String[] aFlds = sLine.split("\t");

                String sFile = qTemplateFileName(aFlds[0]);
                aRemoteFiles.add(aFlds[0]);
                File fTst = new File(sFile);
                Boolean bDownload = false;
                if (!fTst.exists())
                    bDownload = true;
                else if (aFlds.length > 1) {
                    long iSize = fTst.length();
                    long iRemoteSize = Integer.parseInt(aFlds[1]);
                    if (iSize != iRemoteSize)
                        bDownload = true;
                }
                if (bDownload)
                    downloadQuestionnaire(aFlds[0]);
                iCount++;
            }
            // remove any that are gone from server
            ArrayList<String>lLocal= listLocalTemplates(sUID);
            for( String sLocal : lLocal)
            {
                if (!aRemoteFiles.contains(sLocal) )
                {
                    Log.d("HRISLOG","TODO rremove local file ");
                    String sFile = qTemplateFileName(sLocal);
                    File fTst = new File(sFile);
                    fTst.delete();
                }
            }
        }
        return iCount;
    }

}
