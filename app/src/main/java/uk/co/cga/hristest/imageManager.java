package uk.co.cga.hristest;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Howard on 05/02/2016.
 */
public class imageManager {

    static public final String PATH = cGlobal.main_Context.getFilesDir() + "/img/"; //put the downloaded file here

    static public String ImagePath(String fileName)
    {
        String sSafeFileName = fileName.replaceAll("\\W+", ".");
        return PATH + sSafeFileName;
    }

    static public void DownloadFromUrl(String fileName) {  //this is the downloader method
        try {
            String sSafeFileName = fileName.replaceAll("\\W+", ".");
            String sTargetPath = ImagePath(fileName);
            File fTarget = new File(sTargetPath);
            if ( fTarget.exists() )
            {
                // nothing to do
                Log.d("HRISLOG", "IMG already exists "+ fileName);
                return;
            }
            String sTempFile = ImagePath("temp_download.dat");

            URL url = new URL( cGlobal.getString(R.string.imgurl) + sSafeFileName ); //you can write here any link here"
            File fTemp = new File( sTempFile );
            fTemp.mkdirs();
            if ( fTemp.exists())
            {
                if ( !fTemp.delete() ) throw new Exception("Cannot delete temporary file befor download");
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
            while ((len = bis.read(buffer)) != -1) {
                fos.write(buffer,0,len);
            }
            fos.close();

            // if we get here without exception then move from tmp to read
            fTarget = new File(sTargetPath);
            if ( fTarget.exists() )
            {
                if ( !fTarget.delete() )
                    throw new Exception("Cannot delete target file after download");
            }
            if ( !fTemp.renameTo(fTarget) )
                throw new Exception("Cannot rename temporary file to final");

            Log.d("HRISLOG", "IMG download ready in"
                    + ((System.currentTimeMillis() - startTime) / 1000)
                    + " sec");

        } catch ( Exception e) {
            Log.e("HRISLOG", "Error: " + e);
        }

    }
}

