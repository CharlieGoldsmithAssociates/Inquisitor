package uk.co.cga.hristest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;


/**
 * Created by Howard on 05/02/2016.
 */
public class cUtils {

    static public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) cGlobal.main_Context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    static public String HashString ( String sKey )
    {
        try {
            String sTmp = cPwd.sSalt1 +sKey;
            final MessageDigest digest = MessageDigest.getInstance("SHA256");
            byte[] result = digest.digest( sTmp.getBytes());
            StringBuilder sb = new StringBuilder();

            for (byte b : result) // This is your byte[] result..
            {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }
        catch ( Exception ex )
        {
            Log.e("HRISLOG","HASH Failed "+ ex.getMessage());
        }

        return null;
    }

    static public boolean isAPIResultOK ( String sResult)
    {
        return ( sResult.startsWith("OK:") );
    }
    static public String getAPIResulttext ( String sResult)
    {
        if ( sResult.startsWith("OK:") ) return sResult.substring(3);
        if ( sResult.startsWith("ERR:") ) return sResult.substring(4);
        return "";
    }

    static public String getAPIResult ( String sAPIInstruction)
    {
        return getAPIResult(sAPIInstruction, "");
    }

    static public String getAPIResult ( String sAPIInstruction, String sArgs )
    {
        Random r = new Random();
        String sR = String.format("%d",r.nextInt(10000) );// random length inclusion to make ssl decod a modicum harder
        return getURL(cGlobal.getString(R.string.apiurl) + "?R=" + sR + "&K=" + cPwd.sAPIKey + "&F=" + sAPIInstruction + sArgs);

    }

    static public String postAPIForm ( String sAPIInstruction, Handler uiProgress, HashMap<String,String>hm )
    {
        Random r = new Random();
        String sR = String.format("%d", r.nextInt(10000));// random length inclusion to make ssl decod a modicum harder
        hm.put("R" , sR );
        hm.put("K" , cPwd.sAPIKey );
        hm.put("F", sAPIInstruction);
        hm.put("UID", cGlobal.curUID());
        String sReply = postURL(cGlobal.getString(R.string.apiurl),uiProgress, hm);
        hm.remove("R");
        hm.remove("K");
        hm.remove("F");
        hm.remove("UID");
        return sReply;
    }

    static public String getURL ( String sURL)
    {
        return getURL(sURL,null);
    }

    static public void statusReport(String sReport, Handler uiProgress ) {
        Bundle b = new Bundle(1);
        b.putString("MSG", sReport);
        Message msg = new Message();
        msg.setData(b);
        uiProgress.sendMessage(msg);
    }

    static public String getURL ( String sURL, Handler uiProgress)
    {
        //Log.d("HRISLOG", "getURL begining");
        // todo remove log that exposes pwds
        Log.d("HRISLOG", "getURL url:" + sURL);

        StringBuilder sOut= new StringBuilder();
        if( isNetworkAvailable()) {
            try {

                URL url = new URL(sURL);

                long startTime = System.currentTimeMillis();

            /* Open a connection to that URL. */
                if (uiProgress != null) statusReport("Connecting..", uiProgress);

                HttpURLConnection ucon = (HttpURLConnection) url.openConnection();

                if (ucon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                    InputStreamReader isr = new InputStreamReader(ucon.getInputStream());
                    BufferedReader br = new BufferedReader(isr);
                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */

                    String sLine;
                    int iCount = 0;
                    int iLen = 0;
                    while ((sLine = br.readLine()) != null) {
                        iLen += sLine.length();
                        sOut.append(sLine + "\n");
                        iCount++;
                        if ((iCount % 10) == 0 &&
                                uiProgress != null)
                            statusReport(String.format("Downloading  %d KB", (iLen / 1000)), uiProgress);
                    }


                    Log.d("HRISLOG", "getURL download ready in"
                            + ((System.currentTimeMillis() - startTime) / 1000)
                            + " sec, size=" + String.format("%d", sOut.length()));

                    if (uiProgress != null)
                        statusReport(String.format("Download done in %d sec, size %d", ((System.currentTimeMillis() - startTime) / 1000)
                                , sOut.length()), uiProgress);
                } else {
                    if (uiProgress != null)
                        statusReport("Error Connecting  %d" + ucon.getResponseCode(), uiProgress);

                    Log.d("HRISLOG", String.format("getURL download error %d after %d s ", ucon.getResponseCode(),
                            (System.currentTimeMillis() - startTime) / 1000));
                }

            } catch (Exception e) {
                Log.e("HRISLOG", "GetURL Exception: " + e.getMessage());
                sOut = new StringBuilder();
            }
        }

        return sOut.toString();
    }

    static public String postURL ( String sURL, Handler uiProgress, HashMap<String,String> hm )
    {
        Log.d("HRISLOG", "putURL begining");
        //Log.d("HRISLOG", "getURL url:" + sURL);

        StringBuilder sOut= new StringBuilder();
        String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

        if( isNetworkAvailable()) {
            try {
                long startTime = System.currentTimeMillis();
                /* Open a connection to that URL. */
                if (uiProgress != null) statusReport("Connecting..", uiProgress);
                URL url = new URL(sURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true); // Triggers POST.
                connection.setRequestProperty("Accept-Charset", charset);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

                String sAmp="";
                String sForm="";
                for( String sKey: hm.keySet())
                {
                    sForm += sAmp+sKey+"="+ URLEncoder.encode( hm.get(sKey), charset);
                    sAmp="&";
                }
                OutputStream output = connection.getOutputStream();
                output.write( sForm.getBytes() );

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                /*
                 * Define InputStreams to read from the URLConnection.
                 */
                    InputStreamReader isr = new InputStreamReader(connection.getInputStream());
                    BufferedReader br = new BufferedReader(isr);
                /*
                 * Read bytes to the Buffer until there is nothing more to read(-1).
                 */

                    String sLine;
                    int iCount = 0;
                    int iLen = 0;
                    while ((sLine = br.readLine()) != null) {
                        iLen += sLine.length();
                        sOut.append(sLine + "\n");
                        iCount++;
                        if ((iCount % 10) == 0 &&
                                uiProgress != null)
                            statusReport(String.format("Downloading  %d KB", (iLen / 1000)), uiProgress);
                    }

                    Log.d("HRISLOG", "getURL download ready in"
                            + ((System.currentTimeMillis() - startTime) / 1000)
                            + " sec, size=" + String.format("%d", sOut.length()));

                    if (uiProgress != null)
                        statusReport(String.format("Form sent OK, %d sec", ((System.currentTimeMillis() - startTime) / 1000))
                                , uiProgress);


                }
                else
                {
                    if (uiProgress != null)
                        statusReport("Error Connecting  %d" + connection.getResponseCode(), uiProgress);

                    Log.d("HRISLOG", String.format("getURL download error %d after %d s ", connection.getResponseCode(),
                            (System.currentTimeMillis() - startTime) / 1000));
                }

            }
            catch (Exception e) {
                Log.e("HRISLOG", "GetURL Exception: " + e.getMessage());
                sOut = new StringBuilder(); // reply blank
            }
        }

        return sOut.toString();
    }

    public static String getTimestamp ()
    {
        Date dt = new Date( System.currentTimeMillis() );
        SimpleDateFormat sdf= new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
        return  sdf.format(dt);
    }
}
