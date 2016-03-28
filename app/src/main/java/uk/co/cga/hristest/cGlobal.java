package uk.co.cga.hristest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * Created by Howard on 05/02/2016.
 */
public final class cGlobal {
    public static Context main_Context = null;
    // storage modes - shared for prefs and sqllit for data
    public static SharedPreferences sp;
    public static cDatabase cdb=null;

    public static String packageName;
    public static Context getContext()
    {
        return main_Context;
    }
    public static String[] rUser; // user record for current user also in get/setPref "currentUSER"
    private static SharedPreferences.Editor ed;
    public static cQuestionnaire cQActive; // the active questionnaire

    public static String curUID ()
    {
        if ( rUser == null ) throw new AssertionError("Calling curUID when not logged in.");
        if ( rUser.length < 3) throw new AssertionError("Calling curUID when not logged in ");
        return rUser[0];
    }

    public static String curAdjudicatorName ()
    {
        if ( rUser == null ) throw new AssertionError("Calling curUID when not logged in.");
        if ( rUser.length < 3) throw new AssertionError("Calling curUID when not logged in ");
        return rUser[1];
    }

    public static String sessKey ()
    {
        if ( rUser == null ) throw new AssertionError("Calling sesskey when not logged in.");
        if ( rUser.length < 3) throw new AssertionError("Calling sessKey when not logged in ");
        return rUser[4];
    }

    public static boolean isAdjudicatorLoggedIn ()
    {
        String sUserRec = cGlobal.getPref("currentUSER");
        return ( sUserRec.length() > 0 );
    }

    public static void doAdjudicatorLogin ( String sUserRec, String mLogin )
    {
        cGlobal.setPref("currentUSER", sUserRec);
        cGlobal.setPref("usr"+ mLogin, sUserRec);
        cGlobal.setPref("lastUSER", mLogin);
        cGlobal.rUser = sUserRec.split("\t");

    }
    public static void logAdjudicatorOut ()
    {
        logAdjudicatorOut( main_Context);
    }

    public static void logAdjudicatorOut (Context ctx )
    {
        Log.i("HRISLOG", "Logout");
        cGlobal.setPref("currentUSER","");
        rUser = null;

        // move on to next activity..
        Intent it = new Intent( ctx,
                LoginActivity.class);
        main_Context.startActivity(it);
    }

    public static Integer getPref( String sKey, Integer iDef ) {
        try {
            return Integer.parseInt( getPref(sKey,String.format("%d",iDef)));
        } catch (NumberFormatException e) {
            Log.v("HRISLOG", e.getMessage());
        }
        return iDef;
    }
    public static String getPref( String sKey ) {
        return getPref(sKey,"");
    }
    public static String getPref( String sKey, String sDef )
    {
        return sp.getString(sKey, sDef);
    }

    public static void unsetPref ( String sKey )
    {
        try {
            if ( ed == null )
                ed = sp.edit();
            ed.remove(sKey);
            ed.apply();
        } catch (Exception e) {
            e.printStackTrace();
            ed=null;
        }
    }
    public static void setPref( String sKey, String sVal )
    {
        try {
            if ( ed == null )
                ed = sp.edit();

            ed.putString(sKey, sVal);
            ed.apply();
        } catch (Exception e) {
            e.printStackTrace();
            ed=null;
        }

    }

    public static void startSetPrefSess()
    {
        if ( ed == null )
            sp.edit();
    }
    public static void endSetPrefSess()
    {
        try {
            ed.apply();
        } catch (Exception e) {
            e.printStackTrace();
            ed=null;
        }
    }
    public static void setPrefDelayed(  String sKey, String sVal )
    {
        ed.putString(sKey, sVal);
    }

    public static String getString ( int iResId )
    {
        return main_Context.getString(iResId);
    }

    public static void InitDatabase( )
    {
        if( main_Context == null) throw new AssertionError("Set context before calling open");
        if ( cdb == null ) {
            cdb = new cDatabase(main_Context);
            cdb.openDataBase();
        }
    }
}
