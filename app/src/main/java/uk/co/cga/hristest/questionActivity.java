package uk.co.cga.hristest;

//import android.app.FragmentTransaction;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.os.Bundle;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.view.ViewParent;
import android.widget.CheckBox;
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
    public boolean misTimerRunning=false;
    public TimerTask mTimerTask;
    public TextView mtvTimer;
    static public Handler mMainHandler=null;
    public Timer mTimer = null;
    public FloatingActionButton  fabDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);

        // set in mainactivity..
        // cGlobal.setPref("currQREF", aQuestionnaires.get(position));
        sQR = cGlobal.getPref("currQREF");
        mQ = new cQuestionnaire(sQR);

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
    public void endInquisition(View v)
    {
        if ( misTimerRunning )
        {
            mTimerTask.cancel();
            mTimerTask=null;
            misTimerRunning=false;
        }

        // TODO perhaps one last change to name it/ add a reminder of who took it..
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
                if ( mtvTimer != null  )
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
                            if ( mQ.isStarted() )
                            {
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
                public void onClick(View v) { onPrevClicked(v); } } );

            ImageButton butNext = (ImageButton)rootView.findViewById(R.id.butNext);
            butNext.setOnClickListener(new View.OnClickListener() {
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


            Log.d("HRISLOG", String.format("Set up page %d ,started ", miQuestion));

            //mStatusView =(TextView) rootView.findViewById(R.id.txtTimer);

            // page 1 - title/home
            // pages 2-'n' are questions 1-n
            LinearLayout lv = (LinearLayout)rootView.findViewById(R.id.fragVLayout);
            TextView tvTmp;
            ImageView ivTmp;

            ArrayList<Integer> hidelist = new ArrayList<Integer>();
            if ( miQuestion == 0  )
            {
                Log.d("HRISLOG", String.format("Set up page 0 "));

                hidelist.add(R.id.txtFooter);
                hidelist.add(R.id.img);

                tvTmp = (TextView) rootView.findViewById(R.id.txtTitle);
                tvTmp.setText(mQ.qTitle());


                tvTmp =(TextView) rootView.findViewById(R.id.txtBody);
                String sTmp = mQ.qDescription();
                if ( sTmp.length() >0) {
                    tvTmp.setText(sTmp);
                    tvTmp.setVisibility(View.VISIBLE);
                }
                else
                    tvTmp.setVisibility(View.GONE);

                tvTmp =(TextView) rootView.findViewById(R.id.txtCandidate);
                String sName = mQ.getStaffName();
                if ( sName.length() >0) {
                    tvTmp.setText(sName);
                    tvTmp.setVisibility(View.VISIBLE);
                }
                else
                    tvTmp.setVisibility(View.GONE);

                ImageButton bStart = (ImageButton)rootView.findViewById(R.id.butStart);
                bStart.setVisibility(View.VISIBLE);
                bStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        questionActivity athis = (questionActivity) getActivity();
                        ImageButton ib = (ImageButton)v;
                        if (mQ.isStarted())
                        {
                            Log.d("HRISLOG", String.format("END!"));
                            athis.endInquisition();
                            ib.setVisibility(View.GONE);
                        }
                        else
                        {
                            Log.d("HRISLOG", String.format("START!"));

                            mQ.Start();// start timer and choose which questions we're going for
                            ib.setImageResource(R.drawable.stop);
                            athis.mSectionsPagerAdapter.notifyDataSetChanged();
                            // rebuild - but we're in this list athis.mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
                            athis.mSectionsPagerAdapter.setPage(1);
                        }
                    }
                });

                tvTmp =(TextView) rootView.findViewById(R.id.txtStartPrompt);
                sTmp = "";

                if ( mQ.isStarted() )
                {
                    // IN PROGRESS !
                    // show timer and stop button..
                    bStart.setImageResource(R.drawable.stop);

                    hidelist.add(R.id.butNow);
                    hidelist.add(R.id.txtAddNow);

                    butNext.setVisibility(View.VISIBLE);
                    butPrev.setVisibility(View.GONE);

                    sTmp = getString(R.string.qStop);

                }
                else
                {
                    // not started
                    bStart.setImageResource(R.drawable.start);

                    sTmp = getString(R.string.qStart);

                    butNext.setVisibility(View.GONE);
                    butPrev.setVisibility(View.GONE);
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
                Log.v("HRISLOG", "Set up page " + miQuestion + " q="+iQuestion);

                butNext.setVisibility( ( iQuestion>= iNoQuesiton )? View.GONE: View.VISIBLE );
                butPrev.setVisibility(View.VISIBLE);

                String sTmp;
                tvTmp =(TextView) rootView.findViewById(R.id.txtTitle);
                sTmp= mQ.qTitle(iQuestion);
                Log.v("HRISLOG", "Title " + sTmp);
                if ( sTmp.length()>0 )
                    tvTmp.setText(  sTmp );
                else
                    tvTmp.setVisibility(View.GONE);

                tvTmp =(TextView) rootView.findViewById(R.id.txtBody);
                sTmp= mQ.qText(iQuestion);
                Log.v("HRISLOG", "Q Text " + sTmp);
                if ( sTmp.length()>0 )
                    tvTmp.setText(  sTmp );
                else
                    tvTmp.setVisibility(View.GONE);

                tvTmp =(TextView) rootView.findViewById(R.id.txtFooter);
                sTmp= mQ.qFooter(iQuestion);
                Log.v("HRISLOG", "Q foot " + sTmp);
                if ( sTmp.length()>0 )
                    tvTmp.setText(  sTmp );
                else
                    tvTmp.setVisibility(View.GONE);

                ivTmp = (ImageView)rootView.findViewById( R.id.img);
                sTmp=mQ.qImagePath(iQuestion);
                Log.v("HRISLOG" , "Image "+ sTmp );
                if ( sTmp.length()>0 ) {
                    // belt and braces: get the image now if we don't already have it
                    String sImgPath = imageManager.ImagePath(sTmp );
                    File fTemp = new File( sImgPath );
                    if ( !fTemp.exists())
                        imageManager.DownloadFromUrl(sTmp);
                    Bitmap bmImg = BitmapFactory.decodeFile(sImgPath);
                    ivTmp.setImageBitmap(bmImg);
                }
                else
                    ivTmp.setVisibility(View.GONE);

                // now load dynamic number of answers
                Integer iNoAnswers = mQ.qNoAnswers(iQuestion);
                Integer iChoice = mQ.getChosenAnswer(iQuestion);
                Log.v("HRISLOG", "Answers for q "+iQuestion+ "=" + iNoAnswers);

                CheckBox cbTmp;
                int iA;
                for ( iA =1 ;iA<= iNoAnswers;iA++)
                {
                    cbTmp = new CheckBox(rootView.getContext());
                    cbTmp.setText(mQ.qAnswerPrompt(iQuestion, iA));
                    cbTmp.setTextSize(24);
                    cbTmp.setPadding(0, 6, 0, 6);
                    cbTmp.setVisibility(View.VISIBLE);
                    HashMap<Integer,Object> hm = new HashMap<Integer,Object>();
                    hm.put(0, iA);
                    hm.put(1, iQuestion);
                    hm.put(2, mQ);
                    cbTmp.setTag(hm); // don't use the tag indexes http://stackoverflow.com/questions/2434988/android-view-gettag-settag-and-illegalargumentexception
                    cbTmp.setId( 1000+(10*iQuestion)+ iA );// conflicting view on this one but seems to work
                    cbTmp.setOnClickListener( new View.OnClickListener() {
                        public void onClick(View v) { onAnswerClicked(v); } } );
                    // restore choice if any
                    cbTmp.setChecked ( iA == iChoice );
                    if ( cbTmp.isChecked() )
                        cbTmp.setBackgroundColor(Color.GREEN );
                    else
                        cbTmp.setBackgroundColor(Color.WHITE );
                    lv.addView((View)cbTmp,2+iA);

                }

                // todo put this lot in a single panel we can hide
                hidelist.add (R.id.txtCandidate);
                hidelist.add (R.id.txtCandPrompt);
                hidelist.add ( R.id.llCandidate);
                hidelist.add (R.id.butNow);
                hidelist.add (R.id.txtStartPrompt);
                hidelist.add ( R.id.butStart);

            }
            Log.v("HRISLOG", "hidelist ");
            for( int i: hidelist)
            {
                rootView.findViewById(i).setVisibility(View.GONE);
            }
            Log.v("HRISLOG", "create view done ");
            return rootView;
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


        public static void  onAnswerClicked(View view)
        {
            CheckBox cb = (CheckBox) view;
            HashMap<Integer,Object> hm = (HashMap<Integer,Object>)cb.getTag();
            Integer iAnswer = (Integer)hm.get(0);
            Integer iQuestion = (Integer)hm.get(1);
            cQuestionnaire mQ = (cQuestionnaire)hm.get(2);
            View vParent = cb.getRootView();

            int iNoAnswers = mQ.qNoAnswers(iQuestion);
            Integer iChosen = mQ.getChosenAnswer(iQuestion);
            Log.v("HRISLOG", "OnAnswer clicked Q" + iQuestion + " A" + iAnswer);
            if ( cb.isChecked() )
            {
                // record change in choice
                mQ.setChosenAnswer(iQuestion, iAnswer);
                iChosen = iAnswer;
                // set the colours too for feedback
                int iA;
                for ( iA =1 ;iA<= iNoAnswers;iA++) {
                    CheckBox cbTmp= (CheckBox)vParent.findViewById( 1000+(10*iQuestion) + iA);
                    cbTmp.setChecked ( iA == iChosen );
                    if ( cbTmp.isChecked() )
                        cbTmp.setBackgroundColor(Color.GREEN );
                    else
                        cbTmp.setBackgroundColor(Color.WHITE );
                }
            }
            else
            {
                // de-choose
                if ( iChosen == iAnswer) {
                    mQ.setChosenAnswer(iQuestion, -1);
                    cb.setBackgroundColor(Color.WHITE);
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
