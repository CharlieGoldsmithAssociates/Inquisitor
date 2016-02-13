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

public class loadingActivity extends AppCompatActivity {

    TextView mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        mStatus = (TextView) findViewById(R.id.txtDLStatus);
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

    public class DownloadTask extends AsyncTask<Void, Void, Boolean> {
        public String mUrl;
        public String mReply;

        DownloadTask(String sURL) {
            mUrl = sURL;

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
            Log.v("HRISLOG", "doInBackground-download");
            statusReport("starting download.");

            mReply = cUtils.getURL(mUrl, uiProgress);
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Intent it = new Intent();
                it.putExtra("DLDATA", mReply);


                setResult(RESULT_OK, it);
            } else
                setResult(RESULT_CANCELED);
        }

    }
}
