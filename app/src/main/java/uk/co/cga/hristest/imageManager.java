package uk.co.cga.hristest;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

/**
 * Created by Howard on 05/02/2016.
 */
public class imageManager {

    static public final String PATH = cGlobal.main_Context.getFilesDir() + "/img/"; //put the downloaded file here
    static DownloadTask dlTask=null;

    static public String safeFileName ( String fileName )
    {
        return fileName.replaceAll("[^a-zA-Z0-9\\_\\.]", ".");
    }
    static public String ImagePath(String sQRef, String fileName)
    {
        String sPath = PATH + cGlobal.curUID() + "/"+ safeFileName(sQRef) ;
        File fPath = new File(sPath);
        fPath.mkdirs();
        // was fileName.replaceAll("\\W+", ".");
        return sPath +"/" + safeFileName(fileName);
    }
    static public void RemoveImagesForTemplate (String sQRef )
    {
        try {
            String sPath = ImagePath(sQRef,"") ;
            File fPath = new File(sPath);
            File[] fList = fPath.listFiles();
            for ( File f : fList )
            {
                Log.i("HRISLOG", "Remove image" + f.getName());
                f.delete();
            }
            fPath.delete();
            Log.i("HRISLOG", "Remove image folder " + fPath.getName());
        } catch (Exception e) {
            Log.e("HRISLOG","Failed to delete template files " + sQRef );
        }
    }

    public static class DownloadTask extends AsyncTask<Void, Void, Boolean> {
        static ArrayList<String> mBackgroundChecks=null;

        DownloadTask()
        {
            mBackgroundChecks= new ArrayList<String>();
        };

        public static void AddImage (String sURL, String sFileName) {

            if ( mBackgroundChecks == null )
                mBackgroundChecks= new ArrayList<String>();
            mBackgroundChecks.add( sURL + "~~"+ sFileName );

        }


        @Override
        protected Boolean doInBackground(Void... params) {
            Log.v("HRISLOG", "Downloading in background:");
            Integer iDone = 0;
            while ( iDone < 10 ) {
                if (mBackgroundChecks.size() > 0 ) {
                    iDone = 0;
                    String s = mBackgroundChecks.get(0);
                    mBackgroundChecks.remove(0);
                    String aFld[] = s.split("~~");
                    Log.v("HRISLOG", "Download "+s+" as " + aFld[1]);
                    if (aFld.length == 2) {
                        DownloadFromUrl(aFld[0], aFld[1]);
                    }
                }
                else
                {
                    iDone++;
                    try {
                        Thread.sleep(1000);
                        Log.v("HRISLOG", "Downloading done waiting for more " + iDone);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.v("HRISLOG", "Downloading in background:"+ iDone);
            }
            return true;
        }

        @Override
        protected void onPostExecute( Boolean b ) {
            Log.v("HRISLOG","Download complete ");
            dlTask = null;
        }

    }

    static public void DownloadInBackground(String sQRef, String fileName) {
        if ( dlTask == null )
        {
            // this is supposed to be a quick last minute check
            // but creating background tasks for each image is a killer
            // so create one that waits around for a job to do..
            dlTask = new DownloadTask();
            DownloadTask.AddImage(sQRef, fileName);
            dlTask.execute();
        }
        else
        {
            // slight race condition but never happens/ doesn't matter if it does
            DownloadTask.AddImage(sQRef, fileName);
        }
        // and leave it to get on with it..
    }

    static public void DownloadFromUrl(String sQRef, String fileName) {
            try {
            String sSafeQRef = safeFileName(sQRef);
            String sSafeFileName = safeFileName(fileName);
            String sTargetPath = ImagePath(sQRef, fileName);
            File fTarget = new File(sTargetPath);
            if ( fTarget.exists() )
            {
                // nothing to do
                Log.d("HRISLOG", "IMG already exists "+ fileName);
                return;
            }
            String sTempFile = ImagePath(sQRef,"temp_download.dat");

            //https://hrisrss.org/img/q/<questionnaire name>/image name
            URL url = new URL( cGlobal.getString(R.string.imgurl) +sSafeQRef+"/"+ sSafeFileName );
            File fTemp = new File( sTempFile );
            fTemp.mkdirs();
            if ( fTemp.exists())
            {
                if ( !fTemp.delete() ) throw new Exception("Cannot delete temporary file before download");
            }

            long startTime = System.currentTimeMillis();
            Log.d("HRISLOG", "IMG download begining");
            Log.d("HRISLOG", "IMG download url:" + url);
            Log.d("HRISLOG", "IMG downloaded file name:" + fileName);
            /* Open a connection to that URL. */
            URLConnection ucon = url.openConnection();

            /*
             * Define InputStreams to read from the URLConnection.
             */
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            /*
             * Read bytes to the Buffer until there is nothing more to read(-1).
             */
            byte[] buffer = new byte[4 * 1024];
            FileOutputStream fos = new FileOutputStream(fTemp);

            int len;
            int tlen=0;
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer,0,len);
                tlen+=len;
            }
            fos.close();

            // if we get here without exception then move from tmp to read
            if ( tlen > 0) {
                fTarget = new File(sTargetPath);
                if (fTarget.exists()) {
                    if (!fTarget.delete())
                        throw new Exception("Cannot delete target file after download");
                }
                if (!fTemp.renameTo(fTarget))
                    throw new Exception("Cannot rename temporary file to final");

                Log.d("HRISLOG", "IMG download ready in"
                        + ((System.currentTimeMillis() - startTime) / 1000)
                        + " sec");
            }
            else
            {
                Log.d("HRISLOG", "IMG download FAILED, no data received");
            }

        } catch ( Exception e) {
            Log.e("HRISLOG", "Error: " + e);
        }

    }
}

