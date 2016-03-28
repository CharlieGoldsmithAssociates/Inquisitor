package uk.co.cga.hristest;

//import android.app.FragmentTransaction;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.ViewParent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class questionActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    public String sQR;// sCurrentQuestionnaireRef;
    public cQuestionnaire mQ;
    public Boolean mbQuestionsStopped;
    public boolean misTimerRunning=false;
    public TimerTask mTimerTask;
    public TextView mtvTimer;
    static public Handler mMainHandler=null;
    public Timer mTimer = null;
    public FloatingActionButton  fabDone;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        mbQuestionsStopped=false;
        // set in mainactivity..
        // cGlobal.setPref("currQREF", aQuestionnaires.get(position));
        sQR = cGlobal.getPref("currQREF");
        mQ = new cQuestionnaire(sQR);
        mbQuestionsStopped= false;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.main_title);
        toolbar.setLogo(R.drawable.hrtest_icon);
        toolbar.setLogoDescription(R.string.app_desc);
        mtvTimer = new TextView(this);
        mtvTimer.setTextSize(20.0f);
        mtvTimer.setPadding(20, 0, 10, 0);
        mtvTimer.setGravity(Gravity.RIGHT);
        toolbar.addView(mtvTimer);
        setSupportActionBar(toolbar);

        inflater = LayoutInflater.from( getBaseContext() );
        // Create the adapter that will return a fragment for each of the questions
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        fabDone = (FloatingActionButton) findViewById(R.id.butDone);
        fabDone.setVisibility(View.GONE);
        fabDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                endInquisition(view);
            }
        });


        // create the timer
        mbQuestionsStopped=false;
        InitMainHandler(); // se mHandler
        InitTimerTask(); // init timer task
        mTimer = new Timer();
        mTimer.schedule(mTimerTask, 2000, 1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_question, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void endInquisition()
    {
        endInquisition(null);
    }
    public void endInquisition(View v) {
        mbQuestionsStopped = true;
        if (misTimerRunning || mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
            misTimerRunning = false;
        }

        // one last change to name it/ add a reminder of who took it..
        if (!mQ.hasStaffDetails()) {
            doStaffPrompt(v);
        }
        else
        {
            endInquisitionStage2(v);
        }
    }

    public void endInquisitionStage2 (View v )
    {
        // some feedback to save saving...
        if ( v != null )
        {
            Snackbar sb = Snackbar.make( v,
                    getText(R.string.endSession),
                    Snackbar.LENGTH_LONG);

            sb.setAction("SAVE", null);
            sb.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {

                    super.onDismissed(snackbar, event);
                    // save the questionnaire
                    mQ.End();// set end times etc. calls saveQuestionnaire


                    // then go back to choose next
                    Log.d("HRISLOG", "End session");

                    Intent it = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(it);
                }
            });
            sb.show();

        }
        else
        {
            // no view - hard to do feedback from timer/ multiple sources..
            // save the questionnaire
            mQ.End();// set end times etc. calls saveQuestionnaire


            // then go back to choose next
            Log.d("HRISLOG", "End session");

            Intent it = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(it);
        }


    }

    public void doStaffPrompt (View v)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please add a reminder of who took this test.");

// Set up the input
        final EditText input = new EditText(this);
        final View vParent = v;

        input.setInputType(InputType.TYPE_CLASS_TEXT );
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mQ.setStaffPrompt( input.getText().toString() );
                endInquisitionStage2(vParent);
            }
        });
        /* cancel meaningless builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                endInquisitionStage2(vParent);
            }
        });*/

        builder.show();
    }

    public void InitTimerTask ()
    {
        mTimerTask = new TimerTask() {
                //this method is called every 1000 ms
                @Override
                public void run() {
                    //Log.v("HRISLOG", "timer tick ");
                    mMainHandler.sendEmptyMessage(0);
                }
            };
    }

    // Activity level background status handler..
    public void InitMainHandler ()
    {
        mMainHandler   = new Handler(Looper.getMainLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                if ( mtvTimer != null && ! mbQuestionsStopped )// belt and braces
                {
                    if ( msg != null ) {
                        Bundle b = msg.getData();
                        // if we're called with a message show it..
                        // otherwise just update the count down timer
                        String sMsg = b.getString("MSG");
                        if (sMsg != null) {
                            mtvTimer.setText(sMsg);
                            Log.v("HRISLOG", "frag :" + sMsg);
                        }
                    }
                    // time updates on a timer through the handler
                    if ( mQ.isStarted()) {
                        int iSec = (int) mQ.secondsRemaining();
                        if (iSec > 0) {
                            int iM = (iSec / 60);
                            int iS = iSec - (iM * 60);
                            // timer is in the title bar..
                            String sTime = String.format("%02d:%02d", iM, iS);
                            mtvTimer.setText(sTime);
                            if ( iSec < 30 )
                            {
                                mtvTimer.setBackgroundColor( Color.rgb(255,192,192));
                            }
                        } else {
                            mtvTimer.setText(R.string.timerExpired);
                            mtvTimer.setBackgroundColor( Color.rgb(255,192,192));
                            if ( mQ.isStarted()  )
                            {
                                mbQuestionsStopped=true;
                                endInquisition();

                            }
                        }
                    }
                    else
                    {
                        mtvTimer.setText("");
                    }
                }
            }
        };

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class QuestionFragment extends Fragment {
        private String sQR ;
        private ImageButton mbStart;
        private cQuestionnaire mQ;
        private int miQuestion;
//        private static Handler mHandler;
//        public static TextView mStatusView;
        public boolean mbPaused;
//        private TimerTask mTimerTask;
//        private Timer mTimer;

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public QuestionFragment( ) {
            Log.v("HRISLOG", "newInstance creating ");
            sQR = cGlobal.getPref("currQREF");
            mQ = new cQuestionnaire(sQR);
            miQuestion = -1;
        }

        // thread safe update the status text..
        // called from timer etc. to update time.
        public void statusReport ( String sReport )
        {
            Log.v("HRISLOG", "statusReport");
            Bundle b = new Bundle(1);
            b.putString("MSG", sReport);
            Message msg =new Message();
            msg.setData(b);
            questionActivity aThis = (questionActivity) getActivity();
            aThis.mMainHandler.sendMessage(msg);
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static QuestionFragment newInstance(int iQuestion) {
            Log.v("HRISLOG", "newInstance " + iQuestion);
            QuestionFragment fragment = new QuestionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, iQuestion);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate (Bundle savedInstanceState)
        {
            Bundle a = getArguments();
            if ( a!= null )
                miQuestion = a.getInt(ARG_SECTION_NUMBER);
            Log.v("HRISLOG", "Oncreate frag " + miQuestion);
            super.onCreate(savedInstanceState);
        }
        @Override
        public void onPause ()
        {
            mbPaused=true;
            Log.v("HRISLOG", "OnPause frag " + miQuestion);
            super.onPause();
        }
        @Override
        public void onResume ()
        {
            mbPaused=false;
            Log.v("HRISLOG", "onresume frag " + miQuestion);
            super.onResume();
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.v("HRISLOG", "OncreateView frag " + miQuestion);
            final View rootView = inflater.inflate(R.layout.fragment_question, container, false);
            Bundle a = getArguments();
            miQuestion = a.getInt(ARG_SECTION_NUMBER);
            //int iPage = getArguments().getInt(ARG_SECTION_NUMBER);
            // set tag for this view
            rootView.setTag(miQuestion);

            // grab the activity for later use
            questionActivity athis =(questionActivity) getActivity();

            ImageButton butPrev = (ImageButton)rootView.findViewById(R.id.butPrev);
            butPrev.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onPrevClicked(v);
                }
            });
            ImageButton butPrevBot = (ImageButton)rootView.findViewById(R.id.butPrevBot);
            butPrevBot.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onPrevClicked(v);
                }
            });
            ImageButton butNext = (ImageButton)rootView.findViewById(R.id.butNext);
            butNext.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onNextClicked(v);
                }
            });
            ImageButton butNextBot = (ImageButton)rootView.findViewById(R.id.butNextBot);
            butNextBot.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onNextClicked(v);
                }
            });


            ImageButton butNow = (ImageButton) rootView.findViewById(R.id.butNow);
            butNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callStaffChooser();
                }
            });
            TextView txtAddNow = (TextView)rootView.findViewById(R.id.txtAddNow);
            txtAddNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callStaffChooser();
                }
            });

            Log.d("HRISLOG", String.format("Set up page %d ,started ", miQuestion));

            //mStatusView =(TextView) rootView.findViewById(R.id.txtTimer);

            // page 1 - title/home
            // pages 2-'n' are questions 1-n
            LinearLayout llSelCand = (LinearLayout) rootView.findViewById(R.id.llSelectCad) ;
            LinearLayout lv = (LinearLayout)rootView.findViewById(R.id.fragVLayout);
            LinearLayout llFooter = (LinearLayout)rootView.findViewById(R.id.llFooter);
            TextView tvTmp;
            ImageView ivTmp;
            String sTmp="";
            ArrayList<Integer> hidelist = new ArrayList<Integer>();
            if ( miQuestion == 0  )
            {
                Log.d("HRISLOG", String.format("Set up page 0 "));

                hidelist.add(R.id.txtFooter);
                hidelist.add(R.id.img);

                tvTmp = (TextView) rootView.findViewById(R.id.txtTitle);

                tvTmp.setText( convertText(mQ.qTitle()) );

                setTextOrGone(rootView, R.id.txtBody, mQ.qDescription());

                setTextOrGone(rootView, R.id.txtCandidate, mQ.getStaffName());
                llSelCand.setVisibility(View.VISIBLE);

                mbStart = (ImageButton)rootView.findViewById(R.id.butStart);
                mbStart.setVisibility(View.VISIBLE);
                mbStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doStartStopClick(v);
                    }
                });

                tvTmp =(TextView) rootView.findViewById(R.id.txtStartPrompt);
                tvTmp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doStartStopClick(v);
                    }
                });

                sTmp = "";

                if ( mQ.isStarted() )
                {
                    // IN PROGRESS !
                    // show timer and stop button..
                    mbStart.setImageResource(R.drawable.stop);
                    hidelist.add(R.id.llCandidate);
                    hidelist.add(R.id.butNow);
                    hidelist.add(R.id.txtAddNow);

                    llFooter.setVisibility(View.VISIBLE);
                    butNext.setVisibility(View.VISIBLE);
                    butPrev.setVisibility(View.GONE);
                    butNextBot.setVisibility(View.VISIBLE);
                    butPrevBot.setVisibility(View.GONE);

                    sTmp = getString(R.string.qStop);

                } else {
                    // not started
                    mbStart.setImageResource(R.drawable.start);

                    sTmp = getString(R.string.qStart);

                    butNext.setVisibility(View.GONE);
                    butPrev.setVisibility(View.GONE);
                    llFooter.setVisibility(View.GONE);
                }

                if ( sTmp.length() >0) {
                    tvTmp.setText(sTmp);
                    tvTmp.setVisibility(View.VISIBLE);
                }
                else
                    tvTmp.setVisibility(View.GONE);

            }
            else
            {
                int iQuestion = miQuestion;
                int iNoQuesiton = mQ.noQuestions();
                Log.v("HRISLOG", "Set up page " + miQuestion + " q=" + iQuestion);

                llFooter.setVisibility(View.VISIBLE);
                butNext.setVisibility((iQuestion >= iNoQuesiton) ? View.GONE : View.VISIBLE);
                butPrev.setVisibility(View.VISIBLE);
                butNextBot.setVisibility((iQuestion >= iNoQuesiton) ? View.GONE : View.VISIBLE);
                butPrevBot.setVisibility(View.VISIBLE);

                setTextOrGone(rootView, R.id.txtTitle, mQ.qTitle(iQuestion));

                setTextOrGone(rootView, R.id.txtBody, mQ.qText(iQuestion));

                setTextOrGone(rootView,R.id.txtFooter, mQ.qFooter(iQuestion));

                ivTmp = (ImageView)rootView.findViewById( R.id.img);
                sTmp=mQ.qImagePath(iQuestion);
                Log.v("HRISLOG" , "Image "+ sTmp );
                if ( sTmp.length()>0 ) {
                    try {
                        String sImgPath = imageManager.ImagePath(mQ.msQRef, sTmp );
                        File fTemp = new File( sImgPath );
                        // belt and braces: get the image now if we don't already have it
                        if ( !fTemp.exists())
                            imageManager.DownloadFromUrl(mQ.msQRef,sTmp);
                        Bitmap bmImg;
                        Bitmap bmSrc = BitmapFactory.decodeFile(sImgPath);
                        if ( bmSrc == null )
                        {
                            setTextOrGone(rootView, R.id.txtBody, "Failed to load image\n"+sTmp);
                            Log.e("HRISLOG","Failed to load "+sImgPath);

                        }
                        else {
                            int iNewW = bmSrc.getWidth();
                            if (bmSrc.getHeight() < 256) {
                                //. nothin is laid out yet so we can't use getHeight on any parents etc.
                                // make it 'big enough it can be scaled back
                                iNewW *= 256;
                                iNewW /= bmSrc.getHeight();
                                bmImg = Bitmap.createScaledBitmap(bmSrc, iNewW, 256, false);
                            } else
                                bmImg = bmSrc;
                            ivTmp.setImageBitmap(bmImg);
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                        setTextOrGone(rootView, R.id.txtBody, "Failed to load image\n"+sTmp);
                        Log.e("HRISLOG", "Failed to load " + sTmp + " "+ e.getMessage());
                    }
                }
                else
                    ivTmp.setVisibility(View.GONE);

                // now load dynamic number of answers
                Integer iNoAnswers = mQ.qNoAnswers(iQuestion);
                Integer iChoice = mQ.getChosenAnswer(iQuestion);
                Log.v("HRISLOG", "Answers for q "+iQuestion+ "=" + iNoAnswers);

                CheckBox cbTmp;
                TextView tbTmp;

                int iA;
                for ( iA =1 ;iA<= iNoAnswers;iA++)
                {
                    LinearLayout llPrompt = (LinearLayout) inflater.inflate(R.layout.layoutanswerline, null);
                    llPrompt.setId(1000 + (10 * iQuestion) + iA);// conflicting view on this one but seems to work

                    HashMap<Integer,Object> hm = new HashMap<Integer,Object>();
                    hm.put(0, iA);
                    hm.put(1, iQuestion);
                    hm.put(2, mQ);
                    llPrompt.setTag(hm);
                    llPrompt.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            onAnswerClicked(v,true);
                        }
                    });
                    cbTmp = (CheckBox) llPrompt.findViewById(R.id.cbAnswer);
                    tbTmp = (TextView) llPrompt.findViewById(R.id.txAnswer);
                    ivTmp = (ImageView) llPrompt.findViewById(R.id.imAnswer);

                    cbTmp.setText("");
                    cbTmp.setTextSize(24);
                    cbTmp.setPadding(0, 6, 0, 6);
                    cbTmp.setVisibility(View.VISIBLE);
                    // set the tag
                    cbTmp.setTag(hm); // don't use the tag indexes http://stackoverflow.com/questions/2434988/android-view-gettag-settag-and-illegalargumentexception
                    cbTmp.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            onAnswerClicked(v,false);
                        }
                    });

                    // prompt is text or image
                    String sPrompt = mQ.qAnswerPrompt(iQuestion, iA);

                    Boolean bUseImage = false;
                    Bitmap bmImg =null;
                    if ( cQuestionnaire.isStringImageFile(sPrompt) ) {

                        try {
                            String sImgPath = imageManager.ImagePath(mQ.msQRef, sPrompt);
                            File fTemp = new File(sImgPath);
                            if (fTemp.exists()) {
                                Bitmap bmSrc = BitmapFactory.decodeFile(sImgPath);
                                int iNewW = bmSrc.getWidth();
                                if ( bmSrc.getHeight() < 180 ) {
                                    //. nothin is laid out yet so we can't use getHeight on any parents etc.
                                    // make it 'big enough it can be scaled back
                                    iNewW *= 180;
                                    iNewW /= bmSrc.getHeight();
                                    bmImg = Bitmap.createScaledBitmap(bmSrc, iNewW,180, false);
                                }
                                else
                                    bmImg = bmSrc;

                                bUseImage = true;
                            }
                            // else file does not exists - use string as question
                        } catch (Exception e) {
                            //e.printStackTrace();
                            Log.e("HRISLOG","Error loading image "+ sPrompt + " " + e.getMessage());
                            sPrompt = getString(R.string.errImg) +" "+ sPrompt;
                            bUseImage = false;
                        }
                    }

                    if ( bUseImage && bmImg != null )
                    {
                        ivTmp.setImageBitmap(bmImg);
                        ivTmp.setTag(hm);
                        ivTmp.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                onAnswerClicked(v,false);
                            }
                        });
                        tbTmp.setVisibility(View.GONE);
                    }
                    else
                    {
                        ivTmp.setVisibility(View.GONE);
                        tbTmp.setText(convertText(sPrompt));
                        tbTmp.setTag(hm);
                        tbTmp.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                onAnswerClicked(v, false);
                            }
                        });
                    }
                    // restore choice if any
                    cbTmp.setChecked ( iA == iChoice );
                    if ( cbTmp.isChecked() )
                        llPrompt.setBackgroundColor(Color.GREEN );
                    else
                        llPrompt.setBackgroundColor(Color.WHITE );

                    lv.addView((View)llPrompt,2+iA);

                }

                // hide the first page working panels
                hidelist.add (R.id.llSelectCad);
                //hidelist.add (R.id.txtCandidate);
                //hidelist.add (R.id.txtCandPrompt);
                //hidelist.add ( R.id.llCandidate);
                hidelist.add(R.id.llStartStop);
                //hidelist.add (R.id.butNow);
                //hidelist.add (R.id.txtStartPrompt);
                //hidelist.add ( R.id.butStart);

            }
            Log.v("HRISLOG", "hidelist ");
            for( int i: hidelist)
            {
                rootView.findViewById(i).setVisibility(View.GONE);
            }
            Log.v("HRISLOG", "create view done ");
            return rootView;
        }

        public Spanned convertText ( String sIn )
        {
            sIn = sIn.replace("~~","<br/>");

            return Html.fromHtml(sIn, null, null);
        }
        public void doStartStopClick(View v )
        {
            questionActivity athis = (questionActivity) getActivity();

            if (mQ.isStarted())
            {
                mbStart.setVisibility(View.INVISIBLE);
                Log.d("HRISLOG", String.format("END!"));
                athis.endInquisition();

            }
            else
            {
                Log.d("HRISLOG", String.format("START!"));
                mbStart.setImageResource(R.drawable.stop);
                mQ.Start();// start timer and choose which questions we're going for


                athis.mSectionsPagerAdapter.notifyDataSetChanged();
                // rebuild - but we're in this list athis.mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
                athis.mSectionsPagerAdapter.setPage(1);
            }
        }

        public void setTextOrGone ( View rootView, int ires, String sVal )
        {
            TextView tvTmp =(TextView) rootView.findViewById(ires);
            // allow some mark up in the body/footer texts

            if ( sVal != null && sVal.length() >0) {
                //tvTmp.setText(sVal);

                tvTmp.setText(convertText(sVal));
                tvTmp.setVisibility(View.VISIBLE);
            }
            else
                tvTmp.setVisibility(View.GONE);
        }

        public void  onPrevClicked(View v) {
            Log.v("HRISLOG", "prev clicked ");
            if ( miQuestion > 0 )
            {
                // select next frag
                questionActivity athis =(questionActivity) getActivity();
                //athis.mSectionsPagerAdapter.setPage(miQuestion-1);
                athis.mViewPager.setCurrentItem(miQuestion-1,true);
            }

        }

        public void  onNextClicked(View v) {
            Log.v("HRISLOG", "next clicked ");
            if ( miQuestion < mQ.noQuestions() )
            {
                // select next frag
                questionActivity athis =(questionActivity) getActivity();
                //athis.mSectionsPagerAdapter.setPage(miQuestion + 1);
                athis.mViewPager.setCurrentItem(miQuestion+1,true);
            }

        }

        public void callStaffChooser()
        {
            // then go back to choose next
            Log.d("HRISLOG", "Choose staff");

            Intent it = new Intent( getContext(), CandidateActivity.class);
            startActivityForResult(it, 0);

        }


        public static void  onAnswerClicked(View view, Boolean bIsContainer )
        {
            //CheckBox cb = (CheckBox) view;

            HashMap<Integer,Object> hm = (HashMap<Integer,Object>)view.getTag();
            Integer iAnswer = (Integer)hm.get(0);
            Integer iQuestion = (Integer)hm.get(1);
            cQuestionnaire mQ = (cQuestionnaire)hm.get(2);// this may cause laeakage - todo
            LinearLayout llPrompt;
            if ( !(view instanceof LinearLayout  ))
                llPrompt = (LinearLayout)view.getParent();
            else
                llPrompt=(LinearLayout)view;
            View vParent = view.getRootView();

            int iNoAnswers = mQ.qNoAnswers(iQuestion);
            Integer iChosen = mQ.getChosenAnswer(iQuestion);
            Log.v("HRISLOG", "OnAnswer clicked Q" + iQuestion + " A" + iAnswer);

            CheckBox cbTmp = (CheckBox) llPrompt.findViewById(R.id.cbAnswer);
            if ( !(view instanceof CheckBox  ))
                cbTmp.setChecked(!cbTmp.isChecked());
            //TextView tbTmp = (TextView) llPrompt.findViewById(R.id.txAnswer);
            //ImageView ivTmp = (ImageView) llPrompt.findViewById(R.id.imAnswer);

            if ( cbTmp.isChecked() )
            {
                // record change in choice
                mQ.setChosenAnswer(iQuestion, iAnswer);
                iChosen = iAnswer;
                // set the colours too for feedback
                int iA;
                for ( iA =1 ;iA<= iNoAnswers;iA++) {
                    LinearLayout llP = (LinearLayout)vParent.findViewById( 1000+(10*iQuestion) + iA);
                    cbTmp = (CheckBox) llP.findViewById(R.id.cbAnswer);
                    cbTmp.setChecked ( iA == iChosen );
                    if ( cbTmp.isChecked() )
                        llP.setBackgroundColor(Color.GREEN );
                    else
                        llP.setBackgroundColor(Color.WHITE );
                }
            }
            else
            {
                // de-choose
                if ( iChosen == iAnswer) {
                    mQ.setChosenAnswer(iQuestion, -1);
                    llPrompt.setBackgroundColor(Color.WHITE);
                }
            }

            FloatingActionButton fabDone = (FloatingActionButton)vParent.findViewById(R.id.butDone);

            if ( mQ.IsComplete() )
                fabDone.setVisibility(View.VISIBLE);
            else
                fabDone.setVisibility(View.GONE);


        }

        @Override
        public void onActivityResult(int reqCode,int resCode, Intent b )
        {
            super.onActivityResult(reqCode,resCode,b);

            if ( resCode == Activity.RESULT_OK)
            {
                String sHRISID = cGlobal.getPref("HRISID");
                String sCandidateName = cGlobal.getPref("CANDIDATE");
                mQ.setStaff(sHRISID, sCandidateName);

                questionActivity athis =(questionActivity) getActivity();
                athis.mSectionsPagerAdapter.notifyDataSetChanged();

                View rootView = athis.mViewPager.getRootView();

                TextView tvTmp =(TextView) rootView.findViewById(R.id.txtCandidate);
                if ( tvTmp != null ) {
                    String sName = mQ.getStaffName();
                    tvTmp.setText( String.format( getString(R.string.staffSelect), sName,sHRISID ));
                    tvTmp.setVisibility(View.VISIBLE);

                    LinearLayout llC = (LinearLayout) rootView.findViewById(R.id.llCandidate);
                    llC.setVisibility(View.GONE);
                }
            }
        }

    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
       // tested storing them but it fails..  public HashMap<Integer, Fragment> mMap;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            // create all the fragments
            /*Log.d("HRISLOG", "Adapter create fragments");

            mMap = new HashMap<Integer, Fragment>();
            for (int iQ = 0; iQ < mQ.noQuestions() + 1; iQ++) {
                Fragment f = QuestionFragment.newInstance(iQ);
                mMap.put(iQ, f);
            }
            Log.d("HRISLOG", "Adapter create fragments done");*/

        }

        public void setPage(int iQuestion)
        {
            Log.v("HRISLOG", "setPage");
            /*
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.replace(R.id.container, getItem(iQuestion));
            fragmentTransaction.addToBackStack(null);

            fragmentTransaction.commit();
            //setPrimaryItem(mViewPager,iQuestion, getItem(iQuestion));*/
            mViewPager.setCurrentItem(iQuestion, true);

        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.

            /*// Return a QuestionFragment (defined as a static inner class below).
            if ( mMap.containsKey(position) ) {
                Log.d("HRISLOG", "Adapter getitem "+position + " from store");
                return mMap.get(position);
            }*/
            Log.d("HRISLOG", "Adapter getitem " + position + " needs creation");
            Fragment f =QuestionFragment.newInstance(position);
            //mMap.put(position, f);
            return f;
        }

        @Override
        public int getCount() {
            // total pages. start + questions
            int i= 1;

            if ( mQ.isStarted()) {
                i= mQ.noQuestions() + 1;
            }
            //Log.v("HRISLOG", "getCount "+ i);
            return i;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Log.v("HRISLOG", "Adapter gettitle "+position );
            if ( position == 0 )
                return "Start Page";
            return mQ.qTitle(position);

        }
    }
}
