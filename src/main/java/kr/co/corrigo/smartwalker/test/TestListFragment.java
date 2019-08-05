/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package kr.co.corrigo.smartwalker.test;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import net.dinglisch.android.tasker.TaskerIntent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import kr.co.corrigo.smartwalker.R;
import kr.co.corrigo.smartwalker.StepCounter.StepDetector;
import kr.co.corrigo.smartwalker.StepCounter.StepListener;
import kr.co.corrigo.smartwalker.beacon.MonalisaFragment;
import kr.co.corrigo.smartwalker.beacon.adapter.BeaconAdapter;
import kr.co.corrigo.smartwalker.common.RequestQueueSingleton;
import kr.co.corrigo.smartwalker.database.BeaconContract;
import kr.co.corrigo.smartwalker.database.DatabaseHelper;
import no.nordicsemi.android.beacon.Beacon;
import no.nordicsemi.android.beacon.BeaconRegion;
import no.nordicsemi.android.beacon.BeaconServiceConnection;
import no.nordicsemi.android.beacon.Proximity;

public class TestListFragment extends ListFragment implements BeaconServiceConnection.BeaconsListener, BeaconServiceConnection.RegionListener, SensorEventListener, StepListener, LocationListener {
    private static final int BASE_RESOLUSION_WIDTH = 1440;
    private static final int BASE_RESOLUSION_HEIGHT = 2530;
    private static final int LEFT_PRESSURE_THRESHOLD = 60;
    private static final int RIGHT_PRESSURE_THRESHOLD = 60;
    private static final int STEP_TIME_DIFF_THRESHOLD = 100;
    private static final int CONTINUOUS_PRESSURE_THRESHOLD = 1;
    private static final int POINT_DEDUCT_UNIT = 2;
    static final int INTERNET_REQ = 23;
    static final String REQ_TAG = "TEST";
    private static final int CMD_TEST_RESULT = 1;
    private static final int NUM_DATA_TYPES = 12;

    private TestFragment mParentFragment;
    private DatabaseHelper mDatabaseHelper;
    private BeaconAdapter mAdapter;

    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;
    private TextView mTvMessage, mTvHealTimeLabel, mTvStrength, mTvHealingSeconds, mTvCurSpeed, mTvCurSpeedLabel, mTvCurSpeedUnitLabel, mTvAvgSpeed, mTvAvgSpeedLabel, mTvAvgSpeedUnitLabel, mTvHealDistLabel, mTvHealDist, mTvCurHeightLabel, mTvCurHeight, mTvStepValueLabel, mTvSteps, mTvLeftSymmetryLabel, mTvLeftSymmetry, mTvStepTimeLabel, mTvStepTime, mTvLeftTimeLabel, mTvLeftTime;
    private TextView mTvCalConsumLabel, mTvCalConsum, mTvPointLabel, mTvPoint, mTvPointDeducLabel, mTvPointDeduc, mTvRightSymmetryLabel, mTvRightSymmetry, mTvRightTimeLabel, mTvRightTime, mTvFatigueLabel, mTvFatigue, mTvConPressLabel, mTvConPress;
    private ImageView mIvLeftPressureState, mIvRightPressureState;
    private TextView mTvRightPressure, mTvLeftPressure;

    private ImageButton btnStart, btnPause, btnStop;

    private static final int MAX_SAMPLES = 200;
    private long referenceTime;
    private int sampleCount;
    private int leftSampleCount;
    private int rightSampleCount;

    private long[] dateTimeStamp;
    private short[] selStartStop;
    private byte[] selFunction;
    private byte[] leftPressure;
    private byte[] rightPressure;
    private byte[] pointValue;
    private byte[] pointDeduction;
    private float[] currentSpeed;
    private float[] currentHeight;


    private float[] leftMTime;
    private float[] rightMTime;
    private int pointDeductionAcc;
    private int continuousPressureCount;
    private int gpsPauseTime;

    private String currentDate;

    final static int Init = 0;
    final static int Run = 1;
    final static int Pause = 2;

    int cur_Status = Init;
    int myCount = 1;
    long myBaseTime;
    long myPauseTime;

    private LocationManager locationManager;
    private Location mLastlocation = null;
    double sampledSpeed, maxSpeed;
    private int speedSampleCount;
    private double speedSampleSum;
    private double speedAverage;
    private double curHeight;

    private int fragmentWidth;
    private int fragmentHeight;
    private long continuousPressureAccTime;
    private int leftKeyCounterPrev;
    private int rightKeyCounterPrev;


    RequestQueue requestQueue;

    //Timer pauseTimer;
    //TimerTask tt;


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i("LifeCycyle", "Test List Fragment onActivityCreated ");

        mParentFragment = (TestFragment) getParentFragment();
        mDatabaseHelper = mParentFragment.getDatabaseHelper();

        requestQueue = RequestQueueSingleton.getInstance(getActivity().getApplicationContext())
                .getRequestQueue();
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.i("LifeCycyle", "Test List Fragment onStart");
        final Cursor cursor = mDatabaseHelper.getAllRegions();
        setListAdapter(mAdapter = new BeaconAdapter(getActivity(), cursor));

//GPS
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //권한이 없을 경우 최초 권한 요청 또는 사용자에 의한 재요청 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) &&
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // 권한 재요청
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
                return;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mParentFragment.startScanning();

        Log.i("LifeCycyle", "Test List Fragment resumed");


//GPS
        //권한 체크
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 위치정보 업데이트
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);

        referenceTime = System.currentTimeMillis();
        sampleCount = 0;
        leftSampleCount = 0;
        rightSampleCount = 0;

        dateTimeStamp = new long[MAX_SAMPLES];
        selStartStop = new short[MAX_SAMPLES];
        selFunction = new byte[MAX_SAMPLES];
        leftPressure = new byte[MAX_SAMPLES];
        rightPressure = new byte[MAX_SAMPLES];
        pointValue = new byte[MAX_SAMPLES];
        pointDeduction = new byte[MAX_SAMPLES];
        currentSpeed = new float[MAX_SAMPLES];
        currentHeight = new float[MAX_SAMPLES];


        leftMTime = new float[MAX_SAMPLES];
        rightMTime = new float[MAX_SAMPLES];


        speedAverage = 0;
        curHeight = 0;
        speedSampleCount = 0;
        speedSampleSum = 0;
        continuousPressureAccTime = 0;
        pointDeductionAcc = 0;
        continuousPressureCount = 0;
        gpsPauseTime = 0;
        leftKeyCounterPrev = 0;
        rightKeyCounterPrev = 0;

        //pauseTimer = null;
        Log.i("LifeCycyle", "Test List Fragment resumed finish");
    }

    @Override
    public void onPause() {
        super.onPause();
        mParentFragment.stopScanning();
        // 위치정보 가져오기 제거

//GPS
        locationManager.removeUpdates(this);
        Log.i("LifeCycyle", "Test List Fragment puased");
        if (requestQueue != null) {
            requestQueue.cancelAll(REQ_TAG);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        Log.i("LifeCycyle", "Test List Fragment onCreated");

        final View view = inflater.inflate(R.layout.fragment_test_list, container, false);

        sensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);


        mTvMessage = (TextView) view.findViewById(R.id.tvMessage);
        mTvHealTimeLabel = (TextView) view.findViewById(R.id.tvHealTimeLabel);
        mTvHealingSeconds = (TextView) view.findViewById(R.id.healingSecond);


        mTvAvgSpeedLabel = (TextView) view.findViewById(R.id.tvAvgSpeedLabel);
        mTvAvgSpeed = (TextView) view.findViewById(R.id.tvAvgSpeed);
        mTvAvgSpeedUnitLabel = (TextView) view.findViewById(R.id.tvAvgSpeedUnitLabel);
        mTvCurSpeedLabel = (TextView) view.findViewById(R.id.tvCurSpeedLabel);
        mTvCurSpeed = (TextView) view.findViewById(R.id.tvCurSpeed);
        mTvCurSpeedUnitLabel = (TextView) view.findViewById(R.id.tvCurSpeedUnitLabel);

        mTvHealDistLabel = (TextView) view.findViewById(R.id.tvHealDistLabel);
        mTvHealDist = (TextView) view.findViewById(R.id.tvHealDist);
        mTvCurHeightLabel = (TextView) view.findViewById(R.id.tvCurHeightLabel);
        mTvCurHeight = (TextView) view.findViewById(R.id.tvCurHeight);
        mTvStepValueLabel = (TextView) view.findViewById(R.id.tvStepValueLabel);
        mTvSteps = (TextView) view.findViewById(R.id.tvSteps);
        mTvLeftSymmetryLabel = (TextView) view.findViewById(R.id.tvLeftSymmetryLabel);
        mTvLeftSymmetry = (TextView) view.findViewById(R.id.tvLeftSymmetry);
        mTvLeftTimeLabel = (TextView) view.findViewById(R.id.tvLeftTimeLabel);
        mTvLeftTime = (TextView) view.findViewById(R.id.tvLeftTime);
        mTvStepTimeLabel = (TextView) view.findViewById(R.id.tvStepTimeLabel);
        mTvStepTime = (TextView) view.findViewById(R.id.tvStepTime);

        mTvCalConsumLabel = (TextView) view.findViewById(R.id.tvCalConsumLabel);
        mTvCalConsum = (TextView) view.findViewById(R.id.tvCalConsum);
        mTvPointLabel = (TextView) view.findViewById(R.id.tvPointLabel);
        mTvPoint = (TextView) view.findViewById(R.id.tvPoint);
        mTvPointDeducLabel = (TextView) view.findViewById(R.id.tvPointDeducLabel);
        mTvPointDeduc = (TextView) view.findViewById(R.id.tvPointDeduc);
        mTvRightSymmetryLabel = (TextView) view.findViewById(R.id.tvRightSymmetryLabel);
        mTvRightSymmetry = (TextView) view.findViewById(R.id.tvRightSymmetry);
        mTvRightTimeLabel = (TextView) view.findViewById(R.id.tvRightTimeLabel);
        mTvRightTime = (TextView) view.findViewById(R.id.tvRightTime);
        mTvFatigueLabel = (TextView) view.findViewById(R.id.tvFatigueLabel);
        mTvFatigue = (TextView) view.findViewById(R.id.tvFatigue);
        mTvConPressLabel = (TextView) view.findViewById(R.id.tvConPressLabel);
        mTvConPress = (TextView) view.findViewById(R.id.tvConPress);

        mTvStrength = (TextView) view.findViewById(R.id.tvStrength);

        mIvLeftPressureState = (ImageView) view.findViewById(R.id.leftPressureState);
        mIvRightPressureState = (ImageView) view.findViewById(R.id.rightPressureState);

        mTvRightPressure = (TextView) view.findViewById(R.id.tvRightPressure);
        mTvLeftPressure = (TextView) view.findViewById(R.id.tvLeftPressure);

        btnStart = (ImageButton) view.findViewById(R.id.bt_start);
        btnPause = (ImageButton) view.findViewById(R.id.bt_pause);
        btnStop = (ImageButton) view.findViewById(R.id.bt_stop);

        btnPause.setEnabled(false);
        btnStop.setEnabled(false);

        maxSpeed = sampledSpeed = 0;


        /*tt = new TimerTask() {
            @Override
            public void run() {
                //현재시간 측정
                //mLocation의 시간값이 60초이상 지났다면 pauseTime 증가
                //그렇치않으면 Timer 재시케줄 this 사용
            }
        };*/

        //권한 체크
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            getActivity().finish();
        }

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        //Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //if (lastKnownLocation != null) {
        //    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        //    String formatDate = sdf.format(new Date(lastKnownLocation.getTime()));
        //    tvTime.setText(": " + formatDate);  //Time
        //}
        // GPS 사용 가능 여부 확인
        boolean isEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        //tvGpsEnable.setText(": " + isEnable);  //GPS Enable

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);


        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                switch (cur_Status) {
                    case Init:
                        myBaseTime = SystemClock.elapsedRealtime();
                        myTimer.sendEmptyMessageDelayed(0,1000);

                        numSteps = 0;
                        speedAverage = 0;
                        curHeight = 0;
                        speedSampleCount = 0;
                        speedSampleSum = 0;
                        continuousPressureAccTime = 0;
                        pointDeductionAcc = 0;
                        continuousPressureCount = 0;
                        sampleCount = 0;
                        leftSampleCount = 0;
                        rightSampleCount = 0;
                        gpsPauseTime = 0;
                        leftKeyCounterPrev = 0;
                        leftKeyCounterPrev = 0;

                        cur_Status = Run;

                        mTvHealingSeconds.setText("00:00:00");
                        mTvHealDist.setText("0.0 km");
                        mTvCalConsum.setText("0 kcal");
                        mTvCurHeight.setText(String.format(Locale.KOREA, "%.2f m", curHeight));
                        mTvPoint.setText("0 P");
                        mTvSteps.setText("0");
                        mTvPointDeduc.setText("-0 P");
                        mTvLeftSymmetry.setText("0.00 %");
                        mTvRightSymmetry.setText("0.00 %");
                        mTvLeftTime.setText("0.00 sec");
                        mTvRightTime.setText("0.00 sec");
                        mTvStepTime.setText("0.00 sec");
                        mTvFatigue.setText("0.00 %");

                        btnStart.setImageDrawable(getResources().getDrawable(R.drawable.play_disable));
                        btnPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_enable));
                        btnStop.setImageDrawable(getResources().getDrawable(R.drawable.stop_enable));


                        Calendar cal = Calendar.getInstance();
                        currentDate = cal.toString();
                        break;
                    case Pause:
                        long now = SystemClock.elapsedRealtime();
                        myTimer.sendEmptyMessage(0);
                        myBaseTime += (now - myPauseTime);
                        cur_Status = Run;
                        btnStart.setImageDrawable(getResources().getDrawable(R.drawable.play_disable));
                        btnPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_enable));
                        btnStop.setImageDrawable(getResources().getDrawable(R.drawable.stop_enable));
                        break;
                }

                btnStart.setEnabled(false);
                btnPause.setEnabled(true);
                btnStop.setEnabled(true);

                sensorManager.registerListener(TestListFragment.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

            }
        });


        btnPause.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                myTimer.removeMessages(0);
                myPauseTime = SystemClock.elapsedRealtime();
                cur_Status = Pause;

                btnStart.setImageDrawable(getResources().getDrawable(R.drawable.play_enable));
                btnPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_disable));
                btnStop.setImageDrawable(getResources().getDrawable(R.drawable.stop_enable));

                btnStart.setEnabled(true);
                btnPause.setEnabled(false);
                btnStop.setEnabled(true);

                sensorManager.unregisterListener(TestListFragment.this);

            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (cur_Status != Pause)
                    myTimer.removeMessages(0);

                long now = SystemClock.elapsedRealtime();
                long outTime = now - myBaseTime;
                double final_dist = speedAverage * ((double) outTime / 1000 / 60 / 60);

                mTvHealDist.setText(String.format(Locale.KOREA, "%.1f km", final_dist));

                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String pWeight = preferences.getString("settings_body_weight", "0");
                double calConsum = 4.8 * Double.valueOf(pWeight) * (((double) outTime / 1000 / 3600));
                mTvCalConsum.setText(String.format(Locale.KOREA, "%.1f kcal", calConsum));

                //int numSamples =leftSampleCount + rightSampleCount;
                //JSONObject testResult = new JSONObject();
                //JSONArray leftPressureArray = new JSONArray();
                //JSONArray rightPressureArray = new JSONArray();
                //JSONArray leftMTimeArray = new JSONArray();
                //JSONArray rightMTimeArray = new JSONArray();

                if(sampleCount != 0)
                    selStartStop[sampleCount-1] = 254;

                JSONArray testResult = new JSONArray();
                JSONObject testResultObject = new JSONObject();
                JSONArray rawData = new JSONArray();

                try {
                    for (int i = 0; i < sampleCount ; i++) {
                        JSONArray newElement = new JSONArray();

                        newElement.put(0, dateTimeStamp[i]);
                        newElement.put(1, selStartStop[i]);
                        newElement.put(2, selFunction[i]);
                        newElement.put(3, leftPressure[i]);
                        newElement.put(4, rightPressure[i]);
                        newElement.put(5, pointValue[i]);
                        newElement.put(6, pointDeduction[i]);
                        newElement.put(7, currentSpeed[i]);
                        newElement.put(8, currentHeight[i]);
                        newElement.put(9, 0);
                        newElement.put(10, 0);
                        newElement.put(11, 0);

                        rawData.put(i, newElement);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {

                    testResult.put(0, "corrigo1");
                    testResult.put(1, "12345678!!");
                    testResult.put(2, CMD_TEST_RESULT);
                    testResult.put(3, sampleCount);
                    testResult.put(4, rawData);

                    testResultObject.put("id", "corrigo1");
                    testResultObject.put("pass", "12345678!!");
                    testResultObject.put("cmd", CMD_TEST_RESULT);
                    testResultObject.put("numSamples", sampleCount);
                    testResultObject.put("rawData", rawData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

              //  getJsonArrayResponsePost(testResult);

                JSONObject params = new JSONObject();
                try {
                    params.put("name", "Droider");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                getJsonResponsePost(testResultObject);
                //getJsonResponsePost(params);
                //getJsonArrayResponsePost(testResult);


                //variable initialization
                cur_Status = Init;

                btnStart.setImageDrawable(getResources().getDrawable(R.drawable.play_enable));
                btnPause.setImageDrawable(getResources().getDrawable(R.drawable.pause_disable));
                btnStop.setImageDrawable(getResources().getDrawable(R.drawable.stop_disable));

                btnStart.setEnabled(true);
                btnPause.setEnabled(false);
                btnStop.setEnabled(false);

                sensorManager.unregisterListener(TestListFragment.this);

                //if(pauseTimer != null)
                //    pauseTimer.cancel();

            }
        });

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                //뷰의 생성된 후 크기와 위치 구하기
                fragmentWidth = view.getWidth();
                fragmentHeight = view.getHeight();
                Log.i("TestFrag_onCreateView", "width = " + fragmentWidth + "height = " + fragmentHeight);

                //리스너 해제
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTvMessage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvMessage.setWidth(getConvertedX(400));
                mTvMessage.setX(getConvertedX(525));
                mTvMessage.setY(getConvertedY(515));
                //리스너 해제
                mTvMessage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvHealTimeLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvHealTimeLabel.setWidth(getConvertedX(400));
                mTvHealTimeLabel.setX(getConvertedX(525));
                mTvHealTimeLabel.setY(getConvertedY(640));

                //리스너 해제
                mTvHealTimeLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvHealingSeconds.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                Log.i("TestFragOnView", "ConvertedX = " + mTvHealingSeconds.getX() + "(" + getConvertedX(525) + ")" + "ConvertedY = " + mTvHealingSeconds.getY() + "(" + getConvertedY(440) + ")");
                mTvHealingSeconds.setWidth(getConvertedX(400));
                mTvHealingSeconds.setX(getConvertedX(525));
                mTvHealingSeconds.setY(getConvertedY(720));
                //mTvHealingSeconds.setWidth(getConvertedX(400));
                float x = mTvHealingSeconds.getX();
                float y = mTvHealingSeconds.getY();
                Log.i("TestFragOnView", "cx= " + x + "cy=" + y);

                //리스너 해제
                mTvHealingSeconds.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvAvgSpeedLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvAvgSpeedLabel.setWidth(getConvertedX(150));
                mTvAvgSpeedLabel.setX(getConvertedX(725));
                mTvAvgSpeedLabel.setY(getConvertedY(890));
                //리스너 해제
                mTvAvgSpeedLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvAvgSpeed.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvAvgSpeed.setWidth(getConvertedX(150));
                mTvAvgSpeed.setX(getConvertedX(725));
                mTvAvgSpeed.setY(getConvertedY(960));
                //리스너 해제
                mTvAvgSpeed.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvAvgSpeedUnitLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvAvgSpeedUnitLabel.setWidth(getConvertedX(150));
                mTvAvgSpeedUnitLabel.setX(getConvertedX(725));
                mTvAvgSpeedUnitLabel.setY(getConvertedY(1020));
                //리스너 해제
                mTvAvgSpeedUnitLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvCurSpeedLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCurSpeedLabel.setWidth(getConvertedX(150));
                mTvCurSpeedLabel.setX(getConvertedX(575));
                mTvCurSpeedLabel.setY(getConvertedY(890));
                //리스너 해제
                mTvCurSpeedLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvCurSpeed.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCurSpeed.setWidth(getConvertedX(150));
                mTvCurSpeed.setX(getConvertedX(575));
                mTvCurSpeed.setY(getConvertedY(960));
                //리스너 해제
                mTvCurSpeed.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvCurSpeedUnitLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCurSpeedUnitLabel.setWidth(getConvertedX(150));
                mTvCurSpeedUnitLabel.setX(getConvertedX(575));
                mTvCurSpeedUnitLabel.setY(getConvertedY(1020));
                //리스너 해제
                mTvCurSpeedUnitLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvHealDistLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvHealDistLabel.setX(getConvertedX(60));
                mTvHealDistLabel.setY(getConvertedY(690));
                //리스너 해제
                mTvHealDistLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvHealDist.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvHealDist.setX(getConvertedX(60));
                mTvHealDist.setY(getConvertedY(780));
                //리스너 해제
                mTvHealDist.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvCurHeightLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCurHeightLabel.setX(getConvertedX(60));
                mTvCurHeightLabel.setY(getConvertedY(940));
                //리스너 해제
                mTvCurHeightLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvCurHeight.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCurHeight.setX(getConvertedX(60));
                mTvCurHeight.setY(getConvertedY(1030));
                //리스너 해제
                mTvCurHeight.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepValueLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepValueLabel.setX(getConvertedX(60));
                mTvStepValueLabel.setY(getConvertedY(1190));
                //리스너 해제
                mTvStepValueLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvSteps.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvSteps.setX(getConvertedX(60));
                mTvSteps.setY(getConvertedY(1280));
                //리스너 해제
                mTvSteps.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftSymmetryLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftSymmetryLabel.setX(getConvertedX(60));
                mTvLeftSymmetryLabel.setY(getConvertedY(1440));
                //리스너 해제
                mTvLeftSymmetryLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftSymmetry.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftSymmetry.setX(getConvertedX(60));
                mTvLeftSymmetry.setY(getConvertedY(1530));
                //리스너 해제
                mTvLeftSymmetry.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftTimeLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftTimeLabel.setX(getConvertedX(60));
                mTvLeftTimeLabel.setY(getConvertedY(1690));
                //리스너 해제
                mTvLeftTimeLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftTime.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftTime.setX(getConvertedX(60));
                mTvLeftTime.setY(getConvertedY(1780));
                //리스너 해제
                mTvLeftTime.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepTimeLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepTimeLabel.setX(getConvertedX(60));
                mTvStepTimeLabel.setY(getConvertedY(1940));
                //리스너 해제
                mTvStepTimeLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepTime.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepTime.setX(getConvertedX(60));
                mTvStepTime.setY(getConvertedY(2030));
                //리스너 해제
                mTvStepTime.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvCalConsumLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCalConsumLabel.setX(getConvertedX(1130));
                mTvCalConsumLabel.setY(getConvertedY(690));
                //리스너 해제
                mTvCalConsumLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvCalConsum.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCalConsum.setX(getConvertedX(1130));
                mTvCalConsum.setY(getConvertedY(780));
                //리스너 해제
                mTvCalConsum.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvPointLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvPointLabel.setX(getConvertedX(1130));
                mTvPointLabel.setY(getConvertedY(940));
                //리스너 해제
                mTvPointLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvPoint.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvPoint.setX(getConvertedX(1130));
                mTvPoint.setY(getConvertedY(1030));
                //리스너 해제
                mTvPoint.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvPointDeducLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvPointDeducLabel.setX(getConvertedX(1130));
                mTvPointDeducLabel.setY(getConvertedY(1190));
                //리스너 해제
                mTvPointDeducLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvPointDeduc.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvPointDeduc.setX(getConvertedX(1130));
                mTvPointDeduc.setY(getConvertedY(1280));
                //리스너 해제
                mTvPointDeduc.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightSymmetryLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightSymmetryLabel.setX(getConvertedX(1130));
                mTvRightSymmetryLabel.setY(getConvertedY(1440));
                //리스너 해제
                mTvRightSymmetryLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightSymmetry.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightSymmetry.setX(getConvertedX(1130));
                mTvRightSymmetry.setY(getConvertedY(1530));
                //리스너 해제
                mTvRightSymmetry.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightTimeLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightTimeLabel.setX(getConvertedX(1130));
                mTvRightTimeLabel.setY(getConvertedY(1690));
                //리스너 해제
                mTvRightTimeLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightTime.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightTime.setX(getConvertedX(1130));
                mTvRightTime.setY(getConvertedY(1780));
                //리스너 해제
                mTvRightTime.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvFatigueLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvFatigueLabel.setX(getConvertedX(1130));
                mTvFatigueLabel.setY(getConvertedY(1940));
                //리스너 해제
                mTvFatigueLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvFatigue.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvFatigue.setX(getConvertedX(1130));
                mTvFatigue.setY(getConvertedY(2030));
                //리스너 해제
                mTvFatigue.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvConPressLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvConPressLabel.setX(getConvertedX(600));
                mTvConPressLabel.setY(getConvertedY(2190));
                //리스너 해제
                mTvConPressLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvConPress.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvConPress.setX(getConvertedX(600));
                mTvConPress.setY(getConvertedY(2280));
                //리스너 해제
                mTvConPress.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mIvLeftPressureState.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvLeftPressureState.setX(getConvertedX(490));
                mIvLeftPressureState.setY(getConvertedY(1740));
                //리스너 해제
                mIvLeftPressureState.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mIvRightPressureState.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvRightPressureState.setX(getConvertedX(810));
                mIvRightPressureState.setY(getConvertedY(1740));
                //리스너 해제
                mIvRightPressureState.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvRightPressure.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightPressure.setX(getConvertedX(775));
                mTvRightPressure.setY(getConvertedY(1600));
                //리스너 해제
                mTvRightPressure.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvLeftPressure.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftPressure.setX(getConvertedX(415));
                mTvLeftPressure.setY(getConvertedY(1600));
                //리스너 해제
                mTvLeftPressure.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    public int getConvertedX(int orign) {
        Log.i("TestFragGetCvrt", "c_width = " + mParentFragment.getFragmentWidth() + "c_height = " + mParentFragment.getFragmentHeight());
        return (orign * fragmentWidth) / BASE_RESOLUSION_WIDTH;
        //return (orign * mParentFragment.getFragmentWidth()) / BASE_RESOLUSION_WIDTH;
    }

    public int getConvertedY(int orign) {
        return (orign * fragmentHeight) / BASE_RESOLUSION_HEIGHT;
        //return (orign * mParentFragment.getFragmentHeight()) / BASE_RESOLUSION_HEIGHT;
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id) {
        mParentFragment.onEditRegion(id);
    }

    /**
     * Registers for monitoring and ranging events for all regions in the database.
     *
     * @param serviceConnection the service connection used to bing activity to the service
     */
    public void startScanning(final BeaconServiceConnection serviceConnection) {
        final Cursor cursor = mDatabaseHelper.getAllRegions();
        while (cursor.moveToNext()) {
            //  final UUID uuid = UUID.fromString(cursor.getString(2 /* UUID */));
            final UUID uuid = null;
            //  final int major = cursor.getInt(3 /* MAJOR */);
            //  final int minor = cursor.getInt(4 /* MINOR */);
            final int major = -1;
            final int minor = -1;
            final int event = cursor.getInt(6 /* EVENT */);

            // We must start ranging for all beacons
            serviceConnection.startRangingBeaconsInRegion(TestFragment.BEACON_COMPANY_ID, uuid, major, minor, this);
            // And additionally start monitoring only for those with these two events set
            if (event == BeaconContract.EVENT_IN_RANGE || event == BeaconContract.EVENT_OUT_OF_RANGE)
                serviceConnection.startMonitoringForRegion(TestFragment.BEACON_COMPANY_ID, uuid, major, minor, this);
        }
    }

    /**
     * Unregisters the fragment from receiving monitoring and ranging events.
     *
     * @param serviceConnection the service connection used to bind activity with the beacon service
     */
    public void stopScanning(final BeaconServiceConnection serviceConnection) {
        if (serviceConnection != null) {
            serviceConnection.stopMonitoringForRegion(this);
            serviceConnection.stopRangingBeaconsInRegion(this);
        }
    }

    @Override
    public void onBeaconsInRegion(final Beacon[] beacons, final BeaconRegion region) {
        if (beacons.length > 0) {
            //mTvTest.setText(Integer.toString(beacons.length));
            Cursor cursor = null;
            boolean dataFound = false;

            for (int i = 0; i < beacons.length; i++) {
                Log.i("ANAL BEACON ADDRESS", i + "번 장치 주소: " + beacons[i].getDeviceAddress());
                //  if(!dataFound) {
                Log.i("ANAL BEACON ADDRESS", "fineRegionByBeacon callsed ");
                cursor = mDatabaseHelper.findRegionByBeacon(beacons[i]);
                //          if(cursor.getCount()>0)
                //               dataFound = true;
                //       }
//            }

                //mAddress 로 찾기 함수 추가 후 변경
                //final Cursor cursor = mDatabaseHelper.findRegion(region);
                try {
                    if (cursor.moveToNext()) {
                        // Update signal strength in the database
                        float accuracy = 5;

                        for (final Beacon beacon : beacons) {
                            if (Proximity.UNKNOWN != beacon.getProximity() && beacon.getAccuracy() < accuracy)
                                accuracy = beacon.getAccuracy();
                        }

                        accuracy = -20 * accuracy + 100;

                        mDatabaseHelper.updateRegionSignalStrength(cursor.getLong(0 /* _ID */), (int) accuracy);

                        //Update foot pressure
                        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        final String lValue = preferences.getString("leftBeacon", "none");
                        final String rValue = preferences.getString("rightBeacon", "none");

                        Log.i("tag settings", "왼발 tag = " + lValue + "Pressure value: " + "오른발 tag = " + rValue);
                        if (!(lValue.equals("none")) && !(rValue.equals("none")) && cur_Status == Run) {

                            if (lValue.equals(cursor.getString(1)) && sampleCount < MAX_SAMPLES) {
                           //     Log.i("LEFT BEACONM", sampleCount + "번째 측정 " + "Pressure value: " + leftPressure[sampleCount] + " Major value:" + beacons[i].getMinor());
                                if (leftKeyCounterPrev < beacons[i].getMajor()) {
                                    leftKeyCounterPrev = beacons[i].getMajor();
                                } else {
                                    continue;
                                }

                                dateTimeStamp[sampleCount] = System.currentTimeMillis();
                                if (sampleCount == 0) {
                                    selStartStop[sampleCount] = 255;
                                } else
                                    selStartStop[sampleCount] = 0;
                                selFunction[sampleCount] = 1;


                                if ((sampleCount + 1) % 100 == 0) {
                                    pointValue[sampleCount] = 1;
                                } else
                                    pointValue[sampleCount] = 0;
                                int temp = sampleCount+1;
                                mTvSteps.setText("" + temp);
                                int point = sampleCount / 100 + pointDeductionAcc;
                                mTvPoint.setText("" + point + " P");

                                //long now = System.currentTimeMillis();
                                //long downScaleDiff;
                                //downScaleDiff = now - referenceTime;
                                //leftMTime[sampleCount] = (float) downScaleDiff / 1000L;
                                leftPressure[sampleCount] = (byte) ((beacons[i].getMinor() / 4095f) * 120);
                                //int temp = cursor.getInt(4 /* MINOR */);
                                //leftPressure[sampleCount] = (byte)temp;
                                //leftPressure[sampleCount] = (byte) accuracy;
                                rightPressure[sampleCount] = 0;
                                //Log.i("SAMPLE", "referenceTime :" + referenceTime + " downScaleDiff = " + String.format("%.2f", leftMTime[leftSampleCount]) + " accuracy = " + accuracy);

                                //Random generator = new Random();
                                changeCircleColor(mIvLeftPressureState, leftPressure[sampleCount]);
                                mTvLeftPressure.setText(String.format(Locale.KOREA, "%d", leftPressure[sampleCount]));
                                //changeCircleColor(mIvLeftPressureState, generator.nextInt(105));
                                Log.i("LEFT BEACONM", sampleCount + "번째 측정 " + "Pressure value: " + leftPressure[sampleCount] + " Major value:" + beacons[i].getMinor());
                            } else if (rValue.equals(cursor.getString(1)) && sampleCount < MAX_SAMPLES) {
                            //    Log.i("RIGHT BEACONM", sampleCount + "번째 측정 " + " Pressure value: " + rightPressure[sampleCount] + " Major value:" + beacons[i].getMinor());
                                if (rightKeyCounterPrev < beacons[i].getMajor()) {
                                    rightKeyCounterPrev = beacons[i].getMajor();
                                } else {
                                    continue;
                                }

                                dateTimeStamp[sampleCount] = System.currentTimeMillis();
                                if (sampleCount == 0) {
                                    selStartStop[sampleCount] = 255;
                                } else
                                    selStartStop[sampleCount] = 0;
                                selFunction[sampleCount] = 1;


                                if ((sampleCount + 1) % 100 == 0) {
                                    pointValue[sampleCount] = 1;
                                } else
                                    pointValue[sampleCount] = 0;

                                int temp = sampleCount+1;
                                mTvSteps.setText("" + temp);
                                int point = sampleCount / 100 + pointDeductionAcc;
                                mTvPoint.setText("" + point + " P");

                                //long now = System.currentTimeMillis();
                                //long downScaleDiff;
                                //downScaleDiff = now - referenceTime;
                                //rightMTime[rightSampleCount] = (float) downScaleDiff / 1000L;
                                rightPressure[sampleCount] = (byte) ((beacons[i].getMinor() / 4095f) * 120);
                                //rightPressure[sampleCount] = (byte)cursor.getInt(4 /* MINOR */);
                                //rightPressure[sampleCount] = (byte) accuracy;
                                leftPressure[sampleCount] = 0;
                                //Log.i("SAMPLE", "referenceTime :" + referenceTime + " downScaleDiff = " + String.format("%.2f", rightMTime[rightSampleCount]) + " accuracy = " + accuracy);
                                changeCircleColor(mIvRightPressureState, rightPressure[sampleCount]);
                                mTvRightPressure.setText(String.format(Locale.KOREA, "%d", rightPressure[sampleCount]));
                                Log.i("RIGHT BEACONM", sampleCount + "번째 측정 " + " Pressure value: " + rightPressure[sampleCount] + " Major value:" + beacons[i].getMinor());
                            }

                            updateSymMetrics();

                            currentSpeed[sampleCount] = (float) curHeight;
                            currentHeight[sampleCount] = (float) sampledSpeed;

                            sampleCount++;

                        } else {
                            // Toast toast = Toast.makeText(getActivity(), "비콘이 등록되어 있지 않습니다.", Toast.LENGTH_LONG);
                            //toast.show();
                        }

                        mTvStrength.setText(" " + accuracy);
                        //mTvTest.setText(cursor.getString(1));


                    }
                } finally {
                    cursor.close();
                }

                mAdapter.swapCursor(mDatabaseHelper.getAllRegions());
            }
        }
    }

    public void updateSymMetrics() {
        byte leftPressureRef = 0, rightPressureRef = 0;
        long leftMTimeRef = 0, rightMTimeRef = 0, leftMTimeRefPrev = 0, rightMTimeRefPrev = 0;
        float leftSymmetry = 0, rightSymmetry = 0, leftTime = 0, rightTime = 0, stepTime = 0, symmetryFatigue = 0;

        if (sampleCount > 0) {
            if ((leftPressure[sampleCount] >= LEFT_PRESSURE_THRESHOLD || rightPressure[sampleCount] >= RIGHT_PRESSURE_THRESHOLD)) {
                if ((leftPressure[sampleCount - 1] >= LEFT_PRESSURE_THRESHOLD || rightPressure[sampleCount - 1] >= RIGHT_PRESSURE_THRESHOLD)) {
                    continuousPressureAccTime += dateTimeStamp[sampleCount] - dateTimeStamp[sampleCount - 1];
                    pointDeductionAcc = pointDeductionAcc - POINT_DEDUCT_UNIT;
                    mTvPointDeduc.setText(String.format(Locale.KOREA, "%d P", pointDeductionAcc));
                    continuousPressureCount++;
                    pointDeduction[sampleCount] = -2;
                } else {
                    continuousPressureAccTime = 0;
                }

                if (cur_Status == Run && continuousPressureAccTime > 0) {
                    mTvConPress.setText(String.format(Locale.KOREA, "%.2f", (float) continuousPressureAccTime / 1000));
                }
            } else {
                continuousPressureAccTime = 0;
            }

            if (leftPressure[sampleCount] != 0) {
                leftMTimeRef = dateTimeStamp[sampleCount];
                leftPressureRef = leftPressure[sampleCount];

                for (int i = sampleCount - 1; i >= 0; i--) {
                    //       Log.i("USM_1", "num samples:" + sampleCount);
                    if (rightPressure[i] != 0) {
                        rightMTimeRef = dateTimeStamp[i];
                        rightPressureRef = rightPressure[i];
                        for (int j = i - 1; j >= 0; j--) {
                            //       Log.i("USM_2", "num samples:" + sampleCount);
                            if (rightPressure[i] != 0) {
                                rightMTimeRefPrev = dateTimeStamp[j];
                                break;
                            }
                        }
                        break;
                    }
                }

                for (int i = sampleCount - 1; i >= 0; i--) {
                    //          Log.i("USM_3", "num samples:" + sampleCount);
                    if (leftPressure[i] != 0) {
                        leftMTimeRefPrev = dateTimeStamp[i];
                        break;
                    }
                }
            } else {
                rightMTimeRef = dateTimeStamp[sampleCount];
                rightPressureRef = rightPressure[sampleCount];

                for (int i = sampleCount - 1; i >= 0; i--) {
                    //     Log.i("SMU_4", "num samples:" + sampleCount);
                    if (leftPressure[i] != 0) {
                        leftMTimeRef = dateTimeStamp[i];
                        leftPressureRef = leftPressure[i];
                        for (int j = i - 1; j >= 0; j--) {
                            if (leftPressure[i] != 0) {
                                leftMTimeRefPrev = dateTimeStamp[j];
                                break;
                            }
                        }
                        break;
                    }
                }

                for (int i = sampleCount - 1; i >= 0; i--) {
                    //       Log.i("USM_5", "num samples:" + sampleCount);
                    if (rightPressure[i] != 0) {
                        rightMTimeRefPrev = dateTimeStamp[i];
                        break;
                    }
                }

            }

            leftSymmetry = (leftPressureRef / ((leftPressureRef + rightPressureRef) / 2f)) * 100;
            rightSymmetry = (rightPressureRef / ((leftPressureRef + rightPressureRef) / 2f)) * 100;
            stepTime = ((leftMTimeRef > rightMTimeRef) ? ((leftMTimeRef - rightMTimeRef) / 1000f) : ((rightMTimeRef - leftMTimeRef) / 1000f));
            symmetryFatigue = (leftSymmetry > rightSymmetry) ? (leftSymmetry - rightSymmetry) : (rightSymmetry - leftSymmetry);
        }

        if (leftMTimeRefPrev != 0 && leftMTimeRef != 0)
            leftTime = ((leftMTimeRef - leftMTimeRefPrev) / 1000f);
        if (rightMTimeRefPrev != 0 && rightMTimeRef != 0)
            rightTime = ((rightMTimeRef - rightMTimeRefPrev) / 1000f);

        if (cur_Status == Run) {
            mTvLeftSymmetry.setText(String.format(Locale.KOREA, "%.2f %%", leftSymmetry));
            mTvRightSymmetry.setText(String.format(Locale.KOREA, "%.2f %%", rightSymmetry));
            mTvLeftTime.setText(String.format(Locale.KOREA, "%.2f sec", leftTime));
            mTvRightTime.setText(String.format(Locale.KOREA, "%.2f sec", rightTime));
            mTvStepTime.setText(String.format(Locale.KOREA, "%.2f sec", stepTime));
            mTvFatigue.setText(String.format(Locale.KOREA, "%.2f %%", symmetryFatigue));
        }
    }

    public void changeCircleColor(ImageView imgView, int footPressure) {
        GradientDrawable bgShape = (GradientDrawable) imgView.getBackground();
        if (footPressure >= 0 && footPressure <= 14)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel1));
        else if (footPressure >= 15 && footPressure <= 29)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel2));
        else if (footPressure >= 30 && footPressure <= 44)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel3));
        else if (footPressure >= 45 && footPressure <= 59)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel4));
        else if (footPressure >= 60 && footPressure <= 74)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel5));
        else if (footPressure >= 75 && footPressure <= 89)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel6));
        else if (footPressure >= 90 && footPressure <= 105)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel7));
    }

    @Override
    public void onEnterRegion(final BeaconRegion region) {
        final Cursor cursor = mDatabaseHelper.findRegion(region);
        try {
            if (cursor.moveToNext()) {
                final int event = cursor.getInt(6 /* EVENT */);
                if (event == BeaconContract.EVENT_IN_RANGE) {
                    fireEvent(cursor);
                }
            }
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onExitRegion(final BeaconRegion region) {
        final Cursor cursor = mDatabaseHelper.findRegion(region);
        try {
            if (cursor.moveToNext()) {
                final int event = cursor.getInt(6 /* EVENT */);
                if (event == BeaconContract.EVENT_OUT_OF_RANGE) {
                    fireEvent(cursor);
                }
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Fires the event associated with the region at the current position of the cursor.
     *
     * @param cursor the cursor with a region details obtained by f.e. {@link DatabaseHelper#findRegion(BeaconRegion)}. The cursor has to be moved to the proper position.
     */
    private void fireEvent(final Cursor cursor) {
        final boolean enabled = cursor.getInt(9 /* ENABLED */) == 1;
        if (!enabled)
            return;

        final int action = cursor.getInt(7 /* ACTION */);
        final String actionParam = cursor.getString(8 /* ACTION PARAM */);

        switch (action) {
            case BeaconContract.ACTION_MONA_LISA: {
                mParentFragment.stopScanning();
                final DialogFragment dialog = new MonalisaFragment();
                dialog.show(mParentFragment.getChildFragmentManager(), "monalisa");
                break;
            }
            case BeaconContract.ACTION_SILENT: {
                final AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_ALLOW_RINGER_MODES);
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                break;
            }
            case BeaconContract.ACTION_ALARM: {
                final Uri alarm = RingtoneManager.getActualDefaultRingtoneUri(getActivity(), RingtoneManager.TYPE_ALARM);
                final Notification notification = new NotificationCompat.Builder(getActivity()).setContentTitle(getString(R.string.alarm_notification_title))
                        .setContentText(getString(R.string.alarm_notification_message, cursor.getString(1 /* NAME */))).setSmallIcon(R.drawable.stat_sys_nrf_beacon).setAutoCancel(true)
                        .setOnlyAlertOnce(true).setSound(alarm, AudioManager.STREAM_ALARM).build();
                final NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(2, notification); // ID 1 is used by the BeaconService
                break;
            }
            case BeaconContract.ACTION_URL: {
                mParentFragment.stopScanning();
                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionParam));
                    startActivity(intent);
                } catch (final ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), R.string.no_application, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case BeaconContract.ACTION_APP: {
                mParentFragment.stopScanning();
                try {
                    final Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setPackage(actionParam);
                    startActivity(intent);
                } catch (final ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), R.string.no_given_application, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case BeaconContract.ACTION_TASKER:
                switch (TaskerIntent.testStatus(getActivity())) {
                    case OK:
                        final TaskerIntent i = new TaskerIntent(actionParam);
                        final BroadcastReceiver br = new BroadcastReceiver() {
                            @Override
                            public void onReceive(final Context context, final Intent recIntent) {
                                if (recIntent.getBooleanExtra(TaskerIntent.EXTRA_SUCCESS_FLAG, false))
                                    Toast.makeText(getActivity(), R.string.tasker_success, Toast.LENGTH_SHORT).show();
                                getActivity().unregisterReceiver(this);
                            }
                        };
                        getActivity().registerReceiver(br, i.getCompletionFilter());
                        // Start the task
                        getActivity().sendBroadcast(i);
                        break;
                    case NotEnabled:
                        Toast.makeText(getActivity(), R.string.tasker_disabled, Toast.LENGTH_SHORT).show();
                        break;
                    case AccessBlocked:
                        Toast.makeText(getActivity(), R.string.tasker_external_access_denided, Toast.LENGTH_SHORT).show();
                        break;
                    case NotInstalled:
                        Toast.makeText(getActivity(), R.string.tasker_not_installed, Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(getActivity(), R.string.tasker_error, Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
            //TvCoords.setText("X:" + event.values[0] + "Y:" + event.values[1] + "Z:" + event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        //걸음수 값
        numSteps++;
        //mTvSteps.setText(""+ numSteps);
        //포인터 값
        //int point = numSteps / 100 + pointDeductionAcc;
        //mTvPoint.setText(""+ point);
    }

    Handler myTimer = new Handler() {
        public void handleMessage(Message msg) {
            mTvHealingSeconds.setText(getTimeOut());
            myTimer.sendEmptyMessage(0);
        }
    };

    String getTimeOut() {
        long now = SystemClock.elapsedRealtime();
        long outTime = now - myBaseTime;
        if (cur_Status == Run) {
            double final_dist;
            final_dist = speedAverage * ((double) outTime / 1000 / 60);
            mTvHealDist.setText(String.format(Locale.KOREA, "%.1f km", final_dist));

            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String pWeight = preferences.getString("settings_body_weight", "0");
            double calConsum = 4.8 * Double.valueOf(pWeight) * ((double) outTime / 1000 / 3600);
            mTvCalConsum.setText(String.format(Locale.KOREA, "%.1f kcal", calConsum));
        }
        String easy_outTime = String.format(Locale.KOREA, "%02d:%02d:%02d", outTime /1000 / 3600, outTime / 1000 / 60, (outTime / 1000) % 60);
        return easy_outTime;

    }


    @Override
    public void onLocationChanged(Location location) {
        double deltaTime = 0;

        if (cur_Status == Run) {
            curHeight = location.getAltitude();
            mTvCurHeight.setText(String.format(Locale.KOREA, "%.1f m", curHeight));
        }

        if (mLastlocation != null) {
            //시간 간격
            deltaTime = (location.getTime() - mLastlocation.getTime()) / 1000.0;
            sampledSpeed = ((mLastlocation.distanceTo(location) / deltaTime) * 60 * 60) / 1000;
            Log.i("onLocationChanged", "sampledSpeed = " + sampledSpeed);
            if (cur_Status == Run) {
                speedSampleCount++;
                speedSampleSum += sampledSpeed;
                speedAverage = speedSampleSum / speedSampleCount;
                mTvCurSpeed.setText(String.format(Locale.KOREA, "%.2f", sampledSpeed));
                mTvAvgSpeed.setText(String.format(Locale.KOREA, "%.2f", speedAverage));
            }
        }
        // 현재위치를 지난 위치로 변경
        mLastlocation = location;


        //if(pauseTimer != null)
        //        pauseTimer.cancel();
        //pauseTimer = new Timer();
        //pauseTimer.schedule(tt,0, 60000);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        //권한 체크
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // 위치정보 업데이트
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void getJsonResponsePost(JSONObject json) {
        String url = getResources().getString(R.string.json_post_url);
        Log.i("VOLLEY_TEST", "url = " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getActivity(), response.toString(), Toast.LENGTH_SHORT).show();

                        String str = null;
                        try {
                           str = response.getString("test");
                            Log.i("VOLLEY_TEST", "response = "+response.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        /*
                        JSONArray jArr = null;
                        try {
                            jArr = response.getJSONArray("fruits");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.i("VOLLEY_TEST", "code = " + intVal);

                        if (jArr != null) {
                            for (int i = 0; i < jArr.length(); i++) {
                                try {
                                    Log.i("VOLLEY_TEST", "fruits = " + jArr.getString(i));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        //serverResp.setText("String Response : "+ response.toString());*/
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getActivity(), "Volley Error", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        jsonObjectRequest.setTag(REQ_TAG);
        requestQueue.add(jsonObjectRequest);
    }

    public void getJsonArrayResponsePost(JSONArray json) {
        String url = getResources().getString(R.string.json_post_url);
        Log.i("VOLLEY_TEST", "url = " + url);
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        Log.i("VOLLEY_TEST", "response = "+response.toString());
                    /*    int result = 0;
                        try {
                            result = response.getInt(1);
                            Log.i("VOLLEY_TEST",response.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Log.i("VOLLEY_TEST","result=" +result);
                        if(result == 90001)
                            Toast.makeText(getActivity(), "서버에 정상 저장되었습니다.", Toast.LENGTH_SHORT).show();
*/
                        /*JSONArray jArr = null;
                        try {
                            jArr = response.getJSONArray(0);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (jArr != null) {
                            for (int i = 0; i < jArr.length(); i++) {
                                try {
                                    Log.i("VOLLEY_TEST", "fruits = " + jArr.getString(i));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        //serverResp.setText("String Response : "+ response.toString());
                        */
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Toast.makeText(getActivity(), "Volley Error", Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        jsonArrayRequest.setTag(REQ_TAG);
        requestQueue.add(jsonArrayRequest);
    }
}
