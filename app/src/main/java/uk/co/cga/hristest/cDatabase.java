package uk.co.cga.hristest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Howard on 06/02/2016.
 */
public class cDatabase  extends SQLiteOpenHelper
{
    private static String DB_NAME ="HRISDB";
    public static final String TABLE_FACILITY = "FACILITY";
    public static final String TABLE_STAFF = "STAFF";
    public static final String TABLE_MDA = "MDA";

    private static final int DATABASE_VERSION = 1;
    private static SQLiteDatabase db;
    private final Context myContext;

    cDatabase(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        this.myContext = context;
        //String dbPath = cGlobal.main_Context.getFilesDir() + "/hristest.db";
        Log.v("HRISLOGdb","Create database version "+DATABASE_VERSION);

    }

    public SQLiteDatabase openDataBase() {
        Log.v("HRISLOGdb", "open writeable database object ");
        if ( db == null )
            db = getWritableDatabase();
        return db;
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        if ( db == null ) openDataBase();;

        /*
        MDA:MDA3	Ministry Of Education
        MDA:MDAMOE	Ministry of Education
        FACILITY:FC01060502	Chukudum County Hosp	2	209	20902	Eastern Equatoria	Budi	Lauro
        FACILITY:SCHZYN	Ungebe Primary School	2	205	20504	Eastern Equatoria	Lafon	Pacidi
        DEBUG: FACILITY COUNT 1090
        DEBUG: STAFF SQL SELECT s.ID,sFName+' '+sMNames+' '+sLName,sDOB,sGender,nrLocalName,f.ID,PID,CID,SID FROM tblStaff s  JOIN tblAssignment a ON aStaff=s.ID AND aEnd IS NULL  JOIN tblNominalRoll n ON aRole=n.ID  JOIN tblFacility f ON nrFacility = f.ID  JOIN tGeoFlat g ON fPayam=PID  WHERE s.DEL=0 AND f.DEL=0 AND aEnd IS NULL  AND PID IN (2,9) OR CID IN (2,9) OR SID IN (2,9)  GROUP BY s.ID
        STAFF:SRCAO0000005	0	1981-01-01 15:13:53	Male	Community Certified Nurse	FC02070702	20707	207	2
        */
        try {
            String sSql = "CREATE TABLE " + TABLE_FACILITY + " (" +
                    "IDX INTEGER PRIMARY KEY ASC " + // row id
                    ",ID TEXT " +
                    ",UID TEXT " +
                    ",NAME TEXT " +
                    ",GEO1 INTEGER " +
                    ",GEO2 INTEGER " +
                    ",GEO3 INTEGER " +
                    ",GNM1 TEXT " +
                    ",GNM2 TEXT " +
                    ",GNM3 TEXT " +
                    ");";
            Log.i("HRISLOGdb", "Create facility database " + sSql);
            db.execSQL(sSql);

            sSql = "CREATE TABLE " + TABLE_STAFF + " (" +
                    "IDX INTEGER PRIMARY KEY ASC " + // row id
                    ",ID TEXT " +
                    ",UID TEXT " +
                    ",NAME TEXT " +
                    ",DOB TEXT " +
                    ",GENDER TEXT " +
                    ",ROLE TEXT " +
                    ",FAC TEXT " +
                    ",GEO3 INTEGER " +
                    ",GEO2 INTEGER " +
                    ",GEO INTEGER " +
                    ");";
            Log.i("HRISLOGdb", "Create staff database " + sSql);
            db.execSQL(sSql);

            sSql = "CREATE TABLE " + TABLE_MDA + " (" +
                    "IDX INTEGER PRIMARY KEY ASC " + // row id
                    ",ID TEXT " +
                    ",UID TEXT " +
                    ",NAME TEXT " +
                    ");";
            Log.i("HRISLOGdb", "Create mda database " + sSql);
            db.execSQL(sSql);
        }
        catch ( Exception e )
        {
            Log.e("HRISLOG"," Error creating database tables " + e.getMessage());
            System.exit(0);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("HRISLOGdb","Update mda from "+oldVersion+" to " + newVersion);
        if ( db == null ) openDataBase();;

        // Drop all tables
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_FACILITY);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_STAFF);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_MDA);

        // create fresh books table
        this.onCreate(db);
    }

    // utility quoting function
    public static String QS( String sFld )
    {
        String sSafe = sFld.replace("\"","'");
        return "\"" + sSafe + "\"";
    }

    public ArrayList<String>  getArray ( String sSql )
    {
        ArrayList<String> aReply = new ArrayList<>();
        if (db == null) openDataBase();

        Log.v("HRISLOGdb", "GetArray "+sSql);
        try
        {
            Cursor cRead = db.rawQuery(sSql, null);
            if ( cRead != null && cRead.moveToFirst() )
            {
                int iLen = cRead.getColumnCount();
                do
                {
                    String sRow="";
                    String sTab="";
                    for(int i=0;i< iLen;i++) {
                        sRow += sTab+cRead.getString(i);
                        sTab = "\t";
                    }
                    aReply.add(sRow);
                } while(cRead.move(1));
                cRead.close();
            }
            Log.v("HRISLOGdb", "GetArray returns "+ aReply.size() + " lines");

        }
        catch(Exception e)
        {
            Log.e("HRISLOGdb", "Error doing array query " + e.getMessage());
        }
        return aReply;
    }

    public boolean startTransaction()
    {
        if ( db == null ) openDataBase();;

        db.beginTransaction();
        return true;
    }
    public void endTransaction()
    {
        if ( db.inTransaction() ) {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }
    public void abandonTransaction()
    {
        if ( db.inTransaction() ) {
            db.endTransaction();
        }
    }

    public boolean  AddFacilityLine ( String sFacilityString, String sUID )
    {
        if ( db == null ) openDataBase();;

        Log.v("HRISLOGdb","Add facility line");
        boolean bReply = false;
        try {
            String[] aFacility = sFacilityString.split("\t");
            if ( aFacility.length < 9 ) throw new AssertionError("Invalid facility line "+ aFacility.length);
            ContentValues cvInsert = new ContentValues();
            cvInsert.put("ID",aFacility[0]);
            cvInsert.put("NAME",aFacility[1]);
            cvInsert.put("GEO1",aFacility[2]);
            cvInsert.put("GEO2",aFacility[3]);
            cvInsert.put("GEO3",aFacility[4]);
            cvInsert.put("GNM1",aFacility[5]);
            cvInsert.put("GNM2",aFacility[6]);
            cvInsert.put("GNM3",aFacility[7]);
            cvInsert.put("UID", sUID);
            if ( db.insert(TABLE_FACILITY,null,cvInsert) == -1 )
                Log.e("HRISLOG","Insert failed ");
            /*
            String sSql = "INSERT INTO "+TABLE_FACILITY+" (ID,NAME,GEO1,GEO2,GEO3,GNM1,GNM2,GNM3) VALUES (" +
                    QS(aFacility[0]) +
                    "," + QS(aFacility[1]) +
                    "," + aFacility[2] +
                    "," + aFacility[3] +
                    "," + aFacility[4] +
                    "," + QS(aFacility[5]) +
                    "," + QS(aFacility[6]) +
                    "," + QS(aFacility[7]) +
                    ")";
            Log.v("HRISLOGdb","Add facility database " + sSql);
            db.execSQL(sSql);*/

            bReply = true;
        }
        catch (Exception e)
        {
            Log.e("HRISLOGdb","Error updating facility database " + e.getMessage());
        }
        return bReply;
    }


    public boolean  AddStaffLine ( String sStaffString, String sUID )
    {
        if ( db == null ) openDataBase();;

        boolean bReply = false;
        try {
            String[] aStaff = sStaffString.split("\t");
            if ( aStaff.length < 9 ) throw new AssertionError("Invalid staff line "+ aStaff.length);
            //s.ID, CONCAT(sFName,' ',sMNames,' ',sLName),sDOB,sGender,nrLocalName,f.ID,PID,CID,SID,nrMDA
            ContentValues cvInsert = new ContentValues();
            cvInsert.put("ID",aStaff[0]);
            cvInsert.put("NAME",aStaff[1]);
            cvInsert.put("DOB",aStaff[2]);
            cvInsert.put("GENDER",aStaff[3]);
            cvInsert.put("ROLE",aStaff[4]);
            cvInsert.put("FAC",aStaff[5]);
            cvInsert.put("GEO3",aStaff[6]);
            cvInsert.put("GEO2",aStaff[7]);
            cvInsert.put("GEO",aStaff[8]);
            cvInsert.put("UID",sUID);
            bReply = true;
            if ( db.insert(TABLE_STAFF,null,cvInsert) == -1 ) {
                bReply = false;
                Log.e("HRISLOG", "Insert failed ");
            }

            /*String sSql = "INSERT INTO ID,NAME,DOB,GENDER,ROLE,FAC,GEO3,GEO2,GEO1,UID"+TABLE_STAFF+" () VALUES (" +
                    QS(aStaff[0]) +
                    "," + QS(aStaff[1]) +
                    "," + QS(aStaff[2]) +
                    "," + QS(aStaff[3]) +
                    "," + QS(aStaff[4]) +
                    "," + QS(aStaff[5]) +
                    "," + aStaff[6] +
                    "," + aStaff[7] +
                    "," + aStaff[8] +
                    "," + QS(sUID) +
                    ")";*/
        }
        catch (Exception e)
        {
            Log.e("HRISLOGdb","Error updating staff database " + e.getMessage());
        }
        return bReply;
    }


    public boolean  AddMDALine ( String sMDAString, String sUID )
    {
        if ( db == null ) openDataBase();;

        boolean bReply = false;
        try {
            String[] aMDA = sMDAString.split("\t");
            if ( aMDA.length < 2 ) throw new AssertionError("Invalid MDA line "+ aMDA.length);

            ContentValues cvInsert = new ContentValues();
            cvInsert.put("ID",aMDA[0]);
            cvInsert.put("NAME",aMDA[1]);
            cvInsert.put("UID",sUID);
            if ( db.insert(TABLE_MDA,null,cvInsert) == -1 ) {
                Log.e("HRISLOGdb", "Error inserting MDA line ");
            }
            else
                bReply = true;
            /*String sSql = "INSERT INTO "+TABLE_MDA+" (ID,NAME) VALUES (" +
                    QS(aMDA[0]) +
                    "," + QS(aMDA[1]) +
                    ")";
            Log.v("HRISLOGdb","Add MDA database " + sSql);
            db.execSQL(sSql);*/

        }
        catch (Exception e)
        {
            Log.e("HRISLOGdb","Error updating MDA database " + e.getMessage());
        }
        return bReply;
    }

    public Boolean userHasData( String sUID)
    {
        Boolean bHasData= false;
        if (db == null) openDataBase();

        Log.v("HRISLOGdb", "check user data "+sUID);
        try {
            Cursor cRead = db.rawQuery("SELECT * FROM " + TABLE_STAFF + " WHERE UID=" + QS(sUID)+ " LIMIT 2", null);
            if (cRead != null) {
                bHasData = cRead.moveToFirst();

                cRead.close();
            }
        }
        catch ( Exception e)
        {
            Log.e("HRISLOGdb","Error counting staff for user database " + e.getMessage());
        }

        return bHasData;
    }

    public void ClearUserTables( String sUID )
    {
        if ( db == null ) openDataBase();;

        try {
            String sQUID = QS(sUID);
            Log.v("HRISLOGdb", "Delete user tables database " +sUID);
            db.execSQL("DELETE FROM "+TABLE_FACILITY + " WHERE UID="+sQUID);
            db.execSQL("DELETE FROM "+TABLE_MDA + " WHERE UID="+sQUID);
            db.execSQL("DELETE FROM "+TABLE_STAFF + " WHERE UID="+sQUID);

        }
        catch (Exception e)
        {
            Log.e("HRISLOGdb","Error updating MDA database " + e.getMessage());
        }

    }
}
