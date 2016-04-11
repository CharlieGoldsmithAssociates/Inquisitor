package uk.co.cga.hristest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
    public static final String STAFFPROMPT="STAFFPROMPT";
    public static final String SAVED="SAVED"   ;
    public static final String SAVEFILE="SAVEFILE"   ;
    public static final String UPLOADED="UPLOADED"   ;
    public static final String OPTIONS="OPTIONS"   ;
    public static final String LOADED="LOADED"   ;
    public static final String SUMMARY="SUMMARY";
    public static final String MARK="MARK";
    public static final String UID="UID";
    public static final String ADJUDICATOR="ADJUDICATOR";

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
    public void setStaffPrompt( String sPrompt )
    {
        cGlobal.setPref(msQRef + STAFFPROMPT, sPrompt);
    }
    public String getStaffPrompt( String sPrompt )
    {
        return cGlobal.getPref(msQRef + STAFFPROMPT, "");
    }

    public void Start()
    {
        long tStart = System.currentTimeMillis()/1000;
        cGlobal.setPref( msQRef + STARTTIME, String.format("%d", tStart));
        String sTS =  cUtils.getTimestamp();
        cGlobal.setPref( msQRef + START,  sTS );
        Log.d("HRISLOG", "Start at " + sTS );

        Random R = new Random();
        for (int iQ=1; iQ <= noQuestions(); iQ++)
        {
            // number of options 
            int iOptions = Integer.parseInt(
                    cGlobal.getPref( msQRef+ String.format(OPTIONS+"_%02d",iQ),"0" ) );
            int iChosenQ=1;
            if ( iOptions> 1 )
                iChosenQ = 1+R.nextInt(iOptions);
            // and store chosen
            Log.v("HRISLOG","Choose question "+iQ+" Choices="+iOptions + " Chosen="+ iChosenQ);
            cGlobal.setPref(msQRef + String.format(QSTUSED+"_%02d", iQ), String.format("%02d",iChosenQ) );
        }

        // store adjudicator for reference
        cGlobal.setPref(msQRef + UID , cGlobal.curUID());
        cGlobal.setPref(msQRef + ADJUDICATOR , cGlobal.curAdjudicatorName());
    }

    public void End ()
    {
        long tStart = System.currentTimeMillis()/1000;
        cGlobal.setPref(msQRef + ENDTIME, String.format("%d", tStart));
        String sTS = cUtils.getTimestamp();
        cGlobal.setPref(msQRef + END, sTS);
        Log.d("HRISLOG", "End at " + sTS);

        cGlobal.unsetPref(msQRef + SAVED);
        cGlobal.unsetPref(msQRef + UPLOADED);

        // mark the questions/ build the summary text
        doSummary(null, true); // calls doMark, updates MARK & SUMMARY Tags

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
            // 1.07 allow zero questions
            if( qNoAnswers(iQ )>0 && getChosenAnswer(iQ) == -1 )
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

    public static Boolean isStringImageFile( String sText)
    {
        if ( sText == null ) return false;
        if ( sText.length()< 2 || sText.length()>20 ) return false;
        String sTmp = sText.toLowerCase();
        if (sTmp.endsWith(".png") ||
                sTmp.endsWith(".jpg") ||
                sTmp.endsWith(".jpeg") ||
                sTmp.endsWith(".gif") ||
                sTmp.endsWith(".png")
                ) {

            return true;
        }
        return false;
    }

    public Boolean isAnswerImageFile( int iQ, int iA )
    {
        return cQuestionnaire.isStringImageFile(qAnswerPrompt(iQ, iQ));
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
    public int getCorrectAnswer (int iQuestion )
    {
        return cGlobal.getPref(msQRef+CORRECT+"_"+chosenQuestionRef(iQuestion),-1);
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

    public static void saveTemplate(String sQRef, String sQText, Handler uiProgress )
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
                    if (sKey.equals(IMG) && sValue.length()>0 ) {
                        statusReport(uiProgress,"Checking image "+sValue);
                        imageManager.DownloadFromUrl(sQRef,sValue);

                    }
                    if (sKey.contains(PROMPT + "_") && isStringImageFile(sValue) ) {
                        statusReport(uiProgress,"Checking image "+sValue);
                        imageManager.DownloadFromUrl(sQRef,sValue);
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
        statusReport(uiProgress,"SavingTemplate "+sQRef+":done");
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
        cGlobal.unsetPref(msQRef + STAFFNAME);
        int iQ;
        for( iQ=1;iQ<= noQuestions(); iQ++)
        {
            setChosenAnswer(iQ,-1);
            // and reset chose quesiton
            cGlobal.unsetPref(msQRef + String.format(QSTUSED+"_%02d", iQ));

        }

    }


    public static ArrayList<String> listSavedQuestionnaires ( Handler uiProgress )
    {
        return listSavedQuestionnaires(0,uiProgress);
    }
    public static ArrayList<String> listSavedQuestionnaires ( )
    {
        return listSavedQuestionnaires( 0,null);
    }
    public static ArrayList<String> listSavedQuestionnaires (Boolean bUploadedState )
    {
        if ( bUploadedState)
            return listSavedQuestionnaires( 1,null);
        return listSavedQuestionnaires( 2,null);
    }

    public static ArrayList<String> listSavedQuestionnaires (int iUploadedMode, Handler uiProgress )
    {
        Log.v("HRISLOG", "List saved questionnaires "+ iUploadedMode);
        String sPath =PATH + "save/";
        File fSpec = new File(sPath);
        ArrayList<String> aReply = new ArrayList<>();
        File[] fList = fSpec.listFiles();
        int iCount=0;

        if ( uiProgress != null )
        {
            Log.v("HRISLOG", "List saved questionnaires : init status");

            Bundle b = new Bundle();
            b.putInt("MAX", fList.length );
            b.putInt("PROG", 0 );
            Message msg = new Message();
        }


        for ( File f : fList  )
        {
            iCount++;
            if ( uiProgress != null ) uiProgress.sendEmptyMessage(iCount);

            if ( f.isFile() )
            {
                String sFile = f.getName();
                if ( sFile.endsWith(".hrisq")) {
                    Boolean bAdd = true;
                    String sSavedQRef = sFile.substring(0, sFile.length() - 6);
                    if ( iUploadedMode>0 )
                    {
                        // more work, read file and check if uploaded time is set
                        Log.v("HRISLOG", "List saved questionnaires : loading "+sSavedQRef);
                        HashMap<String,String> hm = loadSavedQuestionnaire(sSavedQRef);
                        if (hm != null )
                        {
                            if ( hm.containsKey(UPLOADED) )
                            {
                                // uploaded - is that what we want ?
                                if ( iUploadedMode == 1)
                                    bAdd = true;
                                else
                                    bAdd=false;
                            }
                            else
                            {
                                // not uploaded is that what we want
                                if ( iUploadedMode == 0)
                                    bAdd = true;
                                else
                                    bAdd=false;
                            }
                        }
                        else
                            bAdd= false;// err
                    }
                    if ( bAdd )
                        aReply.add(sSavedQRef);
                }
            }
        }
        Log.v("HRISLOG", "List saved questionnaires : done "+aReply.size());
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
        int iPos = 0;
        int iNextPos;
        // last index of _ in name
        while( (iNextPos= sSavedQRef.indexOf('_',iPos+1 ) )>iPos ) iPos = iNextPos;
        String sTemplate= sSavedQRef.substring(0,iPos).toUpperCase();

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
            // first entry is tremplate name...
            hReply.put("TEMPLATE",sTemplate);
            hReply.put("SAVEREF",sSavedQRef);

            FileReader fr = new FileReader(fTemp);
            BufferedReader sr = new BufferedReader(fr);
            String sLine;
            while ( (sLine = sr.readLine()) != null )
            {
                // key = value
                iPos = sLine.indexOf('=');
                if ( iPos>0) {
                    // don't split we only want first =
                    String sKey = sLine.substring(0, iPos).toUpperCase().trim();
                    String sValue = sLine.substring(iPos + 1).trim();
                    String sRawKey = sKey;
                    if ( sKey.startsWith(sTemplate))
                        sRawKey= sKey.substring(sTemplate.length());
                    // no need to decode # questions etc
                    hReply.put(sRawKey,sValue);
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

    public static Boolean updateSavedQuestionnaire(String sSavedQRef, HashMap<String,String> hm)
    {
        Boolean bOk = false;
        // load the core details
        String sFile = qQuestionnaireFileName(sSavedQRef);
        try {
            String sTemplate = hm.get("TEMPLATE");
            File fTemp = new File(sFile);
            if ( fTemp.exists() )
            {
                fTemp.delete();
            }
            FileWriter fw = new FileWriter(fTemp);
            for (String sKey: hm.keySet())
            {
                // key = value
                fw.write( sTemplate+sKey+"="+ hm.get(sKey)+"\n");
            }

            fw.close();
            bOk = true;
        }
        catch (Exception ex )
        {
            Log.e("HRISLOG","Cannot save  questionnaire file " + sFile);
        }

        return bOk;
    }


    private String doMarking( String sQRef)
    {
        int iTotal=0;
        String sSummary="";
        for ( int iQ=1; iQ <= noQuestions();iQ++)
        {
            sSummary += String.format("Q%d=", iQ);
            String sQuestionOption = chosenQuestionRef(iQ);

            int iChosen = getChosenAnswer(iQ);
            // so which is the correct answer in question iQ chosen option
            int iCorrect= getCorrectAnswer(iQ);
            Log.v("HRISLOG","Mark Q:"+iQ+" Answer:"+iChosen + " Correct:"+iCorrect );
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
        sSummary += doMarking(sQRef);
        sSummary += "~~ Started "+ cGlobal.getPref(sQRef+ START,"");
        
        long lEnd = cGlobal.getPref(sQRef+ ENDTIME,0); // millisec/1000 = s
        long lStart = cGlobal.getPref(sQRef+ STARTTIME,0);
        long lDur = lEnd-lStart;
        long iM = lDur/60;
        long iS = lDur - (iM*60);
        sSummary += "  Time:"+ String.format("%02d:%02d",iM,iS);
        sSummary += "~~ Adjudicator " + cGlobal.getPref(sQRef + ADJUDICATOR, "");
        sSummary += " ["+ cGlobal.getPref(sQRef + UID, "") + "]";

        cGlobal.setPref(sQRef + SUMMARY, sSummary);
        return sSummary;
    }

    // load a template from file if not already in sharedprefs
    // and clear any operational/ live values
    private void LoadTemplate(String sQRef)
    {
        if ( cGlobal.getPref(sQRef + LOADED, "").length()>0 ) return;// already in store.
        long tStart = System.currentTimeMillis();
        // load the core details
        String sFile = qTemplateFileName(sQRef);
        String sCurrentQRef = "";
        HashMap<Integer,Integer> hmOptionCount = new HashMap<Integer,Integer>();

        try {

            long tNow = System.currentTimeMillis();
            Log.v("HRISLOG", String.format("Load template started in %d ms", (tNow - tStart)));
            File fTemp = new File(sFile);
            if ( ! fTemp.exists() )
            {
                // can't download it .. we're in main thread..
                // downloadQuestionnaire(sQRef);
                Log.e("HRISLOG","Cannot open questionnaire file " + sFile);
                return;
            }
            cGlobal.startSetPrefSess();

            FileReader fr = new FileReader(fTemp);
            BufferedReader sr = new BufferedReader(fr);
            String sLine;
            Integer iLine=0;
            while ( (sLine = sr.readLine()) != null )
            {
                iLine++;
                // key = value
                int iPos = sLine.indexOf('=');
                if ( iPos>0)
                {
                    tNow = System.currentTimeMillis();
                    Log.v("HRISLOG", String.format("read line %d %d ms",iLine, (tNow - tStart)));
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
                        cGlobal.setPrefDelayed( sQRef+ sKey + sCurrentQRef , sValue );

                    if (sKey.equals(IMG) && sValue.length()>0 ) {
                        // in foreground now so just kick off a background check for the images..
                        imageManager.DownloadInBackground(sQRef, sValue);
                        tNow = System.currentTimeMillis();
                        Log.v ( "HRISLOG", String.format("Started image bg check "+sValue+" after %d ms" , (tNow-tStart )) );
                    }
                    if (sKey.contains(PROMPT + "_") && isStringImageFile(sValue) ) {
                        // in foreground now so just kick off a background check for the images..
                        imageManager.DownloadInBackground(sQRef, sValue);
                        tNow = System.currentTimeMillis();
                        Log.v("HRISLOG", String.format("Started image bg check " + sValue + " after %d ms", (tNow - tStart)));

                    }
                }
            }
            
            sr.close();
            fr.close();
            tNow = System.currentTimeMillis();
            Log.v("HRISLOG", String.format("Finished file read after %d ms", (tNow - tStart)));

            // and store the map of sub quesiton counts as well
            for(Integer iQ: hmOptionCount.keySet())
            {
                String sTmpV = String.format("%d", hmOptionCount.get(iQ));
                String sTmpK = sQRef + String.format(OPTIONS + "_%02d", iQ);
                cGlobal.setPrefDelayed(sTmpK,sTmpV );
            }

            cGlobal.endSetPrefSess(); // write the data back..

            tNow = System.currentTimeMillis();
            Log.v("HRISLOG", String.format("Start reset template %d ms", (tNow - tStart)));
            resetTemplate();
            cGlobal.setPref(sQRef + LOADED, "1");

            tNow = System.currentTimeMillis();
            Log.v("HRISLOG", String.format("Load template done in %d ms", (tNow - tStart)));
        }
        catch (Exception ex )
        {
            Log.e("HRISLOG","Cannot open questionnaire file " + sFile);
        }


    }


    static public String downloadQuestionnaire (String sQRef, Handler uiProgress )
    {
        // get a template
        /*
        String sQ = cUtils.getAPIResult("GETQ", "?Q=" + sQRef);
        if ( !cUtils.isAPIResultOK(sQ))
            throw new AssertionError("Error downloading quesionnaire template "+ sQ );*/
        Log.i("HRISLOG","download template  file "+sQRef);
        statusReport(uiProgress,"Downloading:"+sQRef);
        String sTemplate = cUtils.getURL(cGlobal.getString(R.string.qurl) + sQRef + ".hrisq",uiProgress);
        if ( sTemplate.length() >0 )
            cQuestionnaire.saveTemplate(sQRef, sTemplate, uiProgress);
        return sTemplate;
    }

    static private String templatePath (String sUID)
    {
        return PATH + "templates/"+ sUID ;
    }
    static public String templateRefFromPath ( String sTemplate)
    {
        String sPath = templatePath(cGlobal.curUID()) ;
        if ( ! sTemplate.startsWith(sPath )) throw new AssertionError("Invalid template given");
        return sTemplate.substring( sPath.length() );
    }

    static public ArrayList<String> listLocalTemplates ( String sUID )
    {
        String sPath = templatePath(sUID);
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

    public static void statusReport ( Handler uiProgress, String sReport )
    {
        if ( uiProgress== null )return;
        Bundle b = new Bundle(1);
        b.putString("SUBMSG",sReport);
        Message msg =new Message();
        msg.setData(b);
        uiProgress.sendMessage( msg );
    }

    static public int getAllQuestionnaires (String sUID, Handler uiProgress)
    {
        // list all templates
        int iCount = 0;

        statusReport(uiProgress,"Checking for new templates");
        ArrayList<String> aList = getServerListQuestionnaires(sUID);
        ArrayList<String> aRemoteFiles = new ArrayList<String>();
        if ( aList.size() > 0 ) {
            // check for new/ changed ones..
            for (String sLine :aList) {
                Log.i("HRISLOG","Check received template  file "+sLine);
                String[] aFlds = sLine.split("\t");

                String sFile = qTemplateFileName(aFlds[0]);
                aRemoteFiles.add(aFlds[0]);
                File fTst = new File(sFile);
                Boolean bDownload = false;
                if (!fTst.exists()) {
                    bDownload = true;
                    statusReport(uiProgress,"New Template:"+aFlds[0]);
                }
                else if (aFlds.length > 1) {
                    long iSize = fTst.length();
                    long iRemoteSize = Integer.parseInt(aFlds[1]);

                    Log.i("HRISLOG","Check received template sizes "+ iSize + " remote="+ iRemoteSize);
                    if (iSize != iRemoteSize)
                    {
                        statusReport(uiProgress,"Template changed:"+aFlds[0]);
                        bDownload = true;
                    }

                }
                if (bDownload)
                    downloadQuestionnaire(aFlds[0], uiProgress);
                iCount++;
            }
            // remove any that are gone from server
            ArrayList<String>lLocal= listLocalTemplates(sUID);
            for( String sLocal : lLocal)
            {
                if (!aRemoteFiles.contains(sLocal) )
                {
                    Log.i("HRISLOG", "remove local template  file " + sLocal);
                    String sFile = qTemplateFileName(sLocal);
                    // and the image files stored with this template
                    imageManager.RemoveImagesForTemplate(sLocal);
                    File fTst = new File(sFile);
                    fTst.delete();
                    statusReport(uiProgress, "Template removed:" + sLocal);

                }
            }
        }

        statusReport(uiProgress, String.format("%d Templates checked:",iCount));

        return iCount;
    }

}
