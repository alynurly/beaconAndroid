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
package kr.co.corrigo.smartwalker.analysis;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import net.dinglisch.android.tasker.TaskerIntent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import kr.co.corrigo.smartwalker.R;
import kr.co.corrigo.smartwalker.StepCounter.StepDetector;
import kr.co.corrigo.smartwalker.StepCounter.StepListener;
import kr.co.corrigo.smartwalker.beacon.MonalisaFragment;
import kr.co.corrigo.smartwalker.beacon.adapter.BeaconAdapter;
import kr.co.corrigo.smartwalker.common.RequestQueueSingleton;
import kr.co.corrigo.smartwalker.common.ValueFormatter;
import kr.co.corrigo.smartwalker.database.BeaconContract;
import kr.co.corrigo.smartwalker.database.DatabaseHelper;
import no.nordicsemi.android.beacon.Beacon;
import no.nordicsemi.android.beacon.BeaconRegion;
import no.nordicsemi.android.beacon.BeaconServiceConnection;
import no.nordicsemi.android.beacon.Proximity;

public class AnalysisListFragment extends ListFragment implements BeaconServiceConnection.BeaconsListener, BeaconServiceConnection.RegionListener, SensorEventListener, StepListener, OnChartValueSelectedListener  {
    private static final int BASE_RESOLUSION_WIDTH = 1440 ;
    private static final int BASE_RESOLUSION_HEIGHT = 2530 ;
    private static final int LEFT_PRESSURE_THRESHOLD = 60;
    private static final int RIGHT_PRESSURE_THRESHOLD = 60;
    private static final int POINT_DEDUCT_UNIT = 2;
    static final String ANAL_REQ_TAG = "ListREQ";
    static final String RESULT_REQ_TAG = "ANAL";
    private static final int CMD_ANAL_RESULT = 2;
    private static final int CMD_ANAL_LIST = 3;
    private static final int CMD_ANAL_ITEM = 4;


    private AnalysisFragment mParentFragment;
    private DatabaseHelper mDatabaseHelper;
    private BeaconAdapter mAdapter;

    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;

    private BarChart chart;
    private TextView curDate;
    protected Typeface tfLight;
    private Button startStop;
    private Button listBtn;
    private boolean isAnalyzing;

    private TextView mTvStepValueAnalLabel, mTvStepValueAnal, mTvLeftSymmetryAnalLabel, mTvLeftSymmetryAnal, mTvLeftTimeAnalLabel, mTvLeftTimeAnal, mTvStepTimeAnalLabel, mTvStepTimeAnal, mTvRightSymmetryAnalLabel, mTvRightSymmetryAnal, mTvRightTimeAnalLabel, mTvRightTimeAnal, mTvFatigueAnalLabel, mTvFatigueAnal, mTvConPressAnalLabel, mTvConPressAnal;
    private ImageView mIvLeftPressureStateAnal, mIvRightPressureStateAnal;
    private TextView mTvRightPressureAnal, mTvLeftPressureAnal;

    private static final int MAX_SAMPLES = 200;
    private long referenceTime;
    private int sampleCount;
    private int leftSampleCount;
    private int rightSampleCount;
    private byte[] leftPressureChart;
    private byte[] rightPressureChart;

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

    private int fragmentWidth;
    private int fragmentHeight;
    private long continuousPressureAccTime;
    private int leftKeyCounterPrev;
    private int rightKeyCounterPrev;

    RequestQueue requestQueue;


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mParentFragment = (AnalysisFragment) getParentFragment();
        mDatabaseHelper = mParentFragment.getDatabaseHelper();

        requestQueue = RequestQueueSingleton.getInstance(getActivity().getApplicationContext())
                .getRequestQueue();
    }

    @Override
    public void onStart() {
        super.onStart();

        final Cursor cursor = mDatabaseHelper.getAllRegions();
        setListAdapter(mAdapter = new BeaconAdapter(getActivity(), cursor));
    }

    @Override
    public void onResume() {
        super.onResume();
        mParentFragment.startScanning();

        referenceTime = System.currentTimeMillis();
        sampleCount = 0;
        leftSampleCount = 0;
        rightSampleCount = 0;
        leftPressureChart = new byte[MAX_SAMPLES];
        rightPressureChart = new byte[MAX_SAMPLES];

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

        continuousPressureAccTime = 0;
        pointDeductionAcc = 0;
        continuousPressureCount = 0;
        gpsPauseTime = 0;
        leftKeyCounterPrev = 0;
        rightKeyCounterPrev = 0;
    }

    @Override
    public void onPause() {
        super.onPause();
        mParentFragment.stopScanning();

        if (requestQueue != null) {
            requestQueue.cancelAll(ANAL_REQ_TAG);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_analysis_list, container, false);

        tfLight = Typeface.createFromAsset(getActivity().getAssets(), "fonts/OpenSans-Light.ttf");

        sensorManager = (SensorManager) getActivity().getSystemService(getActivity().SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        mTvStepValueAnalLabel = (TextView) view.findViewById(R.id.tvStepValueAnalLabel);
        mTvStepValueAnal = (TextView) view.findViewById(R.id.tvStepValueAnal);
        mTvLeftSymmetryAnalLabel = (TextView) view.findViewById(R.id.tvLeftSymmetryAnalLabel);
        mTvLeftSymmetryAnal = (TextView) view.findViewById(R.id.tvLeftSymmetryAnal);
        mTvLeftTimeAnalLabel = (TextView) view.findViewById(R.id.tvLeftTimeAnalLabel);
        mTvLeftTimeAnal = (TextView) view.findViewById(R.id.tvLeftTimeAnal);
        mTvStepTimeAnalLabel = (TextView) view.findViewById(R.id.tvStepTimeAnalLabel);
        mTvStepTimeAnal = (TextView) view.findViewById(R.id.tvStepTimeAnal);

        mTvRightSymmetryAnalLabel = (TextView) view.findViewById(R.id.tvRightSymmetryAnalLabel);
        mTvRightSymmetryAnal = (TextView) view.findViewById(R.id.tvRightSymmetryAnal);
        mTvRightTimeAnalLabel = (TextView) view.findViewById(R.id.tvRightTimeAnalLabel);
        mTvRightTimeAnal = (TextView) view.findViewById(R.id.tvRightTimeAnal);
        mTvFatigueAnalLabel = (TextView) view.findViewById(R.id.tvFatigueAnalLabel);
        mTvFatigueAnal = (TextView) view.findViewById(R.id.tvFatigueAnal);
        mTvConPressAnalLabel = (TextView) view.findViewById(R.id.tvConPressAnalLabel);
        mTvConPressAnal = (TextView) view.findViewById(R.id.tvConPressAnal);


        mIvLeftPressureStateAnal = (ImageView) view.findViewById(R.id.leftPressureStateAnal);
        mIvRightPressureStateAnal = (ImageView) view.findViewById(R.id.rightPressureStateAnal);

        mTvRightPressureAnal = (TextView) view.findViewById(R.id.tvRightPressureAnal);
        mTvLeftPressureAnal = (TextView) view.findViewById(R.id.tvLeftPressureAnal);

        chart = (BarChart)view.findViewById(R.id.chart1);

        curDate = (TextView)view.findViewById(R.id.curDate);
        startStop = (Button)view.findViewById(R.id.startStop);
        listBtn = (Button)view.findViewById(R.id.listBtn);

        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if(!isAnalyzing) {
                    isAnalyzing = true;

                    numSteps = 0;
                    continuousPressureAccTime = 0;
                    pointDeductionAcc = 0;
                    continuousPressureCount = 0;
                    sampleCount = 0;
                    leftSampleCount = 0;
                    rightSampleCount = 0;
                    gpsPauseTime = 0;
                    leftKeyCounterPrev = 0;
                    leftKeyCounterPrev = 0;

                    chart.fitScreen();

                    mTvStepValueAnal.setText("0");
                    mTvLeftSymmetryAnal.setText("0.00 %");
                    mTvRightSymmetryAnal.setText("0.00 %");
                    mTvLeftTimeAnal.setText("0.00 sec");
                    mTvRightTimeAnal.setText("0.00 sec");
                    mTvStepTimeAnal.setText("0.00 sec");
                    mTvFatigueAnal.setText("0.00 %");

                    startStop.setText("중지");

                    referenceTime = System.currentTimeMillis();
                }else{
                    isAnalyzing = false;
                    startStop.setText("시작");
                    updateCurDate();

                    //서버로 보내는 코드 추가
                    if(sampleCount != 0)
                        selStartStop[sampleCount-1] = 254;

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
                        testResultObject.put("id", "corrigo1");
                        testResultObject.put("pass", "12345678!!");
                        testResultObject.put("cmd", CMD_ANAL_RESULT);
                        testResultObject.put("numSamples", sampleCount);
                        testResultObject.put("rawData", rawData);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    getJsonResponsePost(testResultObject);
                }
            }
        });

        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                isAnalyzing = false;
                startStop.setText("시작");
                updateCurDate();

                JSONObject listRequestObject = new JSONObject();

                try {
                    listRequestObject.put("id", "corrigo1");
                    listRequestObject.put("pass", "12345678!!");
                    listRequestObject.put("cmd", CMD_ANAL_LIST);
                    listRequestObject.put("numLists", 1);
                    listRequestObject.put("Lists", 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                String url = getResources().getString(R.string.json_post_url);
                Log.i("VOLLEY_TEST", "url = " + url);
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, listRequestObject,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONArray jArr = null;
                                try {
                                    jArr = response.getJSONArray("Lists");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                if(jArr.length()==0) {
                                    Toast.makeText(getActivity(), "No item", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                final String[] dates = new String[jArr.length()];
                                String itemSelected;
                                for(int i=0; i < jArr.length() ; i++) {
                                    try {
                                    //    Log.i("VOLLEY_TEST", "fruits = " + jArr.getString(i));
                                        dates[i] = jArr.getString(i);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("Select Date");
                                builder.setItems(dates, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        Toast.makeText(getActivity(), dates[item], Toast.LENGTH_SHORT).show();

                                        //서버로 보내는 코드 추가
                                        JSONObject itemSelectedObject = new JSONObject();
                                        try {
                                            itemSelectedObject.put("id", "corrigo1");
                                            itemSelectedObject.put("pass", "12345678!!");
                                            itemSelectedObject.put("cmd", CMD_ANAL_ITEM);
                                            itemSelectedObject.put("numSamples", 1);
                                            itemSelectedObject.put("date", dates[item]);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        getJsonResponsePost(itemSelectedObject);
                                    }
                                });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getActivity(), "Volley Error", Toast.LENGTH_SHORT).show();
                    }
                });

                jsonObjectRequest.setTag(ANAL_REQ_TAG);
                requestQueue.add(jsonObjectRequest);
            }
        });

        updateCurDate();
        initializeChart();

        return view;
    }

    public void initializeChart() {
        chart.setOnChartValueSelectedListener(this);
        chart.getDescription().setEnabled(false);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);

//        chart.setDrawBorders(true);

        // scaling can now only be done on x- and y-axis separately
        chart.setPinchZoom(true);

        chart.setDrawBarShadow(false);

        chart.setDrawGridBackground(false);

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it
        //MyMarkerView mv = new MyMarkerView(getActivity(), R.layout.custom_marker_view);
        //mv.setChartView(chart); // For bounds control
        //chart.setMarker(mv); // Set the marker to the chart

        Legend l = chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(true);
        l.setTypeface(tfLight);
        l.setYOffset(0f);
        l.setXOffset(10f);
        l.setYEntrySpace(0f);
        l.setTextSize(8f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setTypeface(tfLight);
        //xAxis.setGranularity(0.5f);
        //xAxis.setCenterAxisLabels(true);

        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        class MyXAxisValueFormatter implements IAxisValueFormatter {

            private String[] mValues;

            public MyXAxisValueFormatter(String[] values) {
                this.mValues = values;
            }

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // "value" represents the position of the label on the axis (x or y)
                return mValues[(int) value];
            }

            /** this is only needed if numbers are returned, else return 0 */
            public int getDecimalDigits() { return 0; }
        }

        String[] values = new String[] {"a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c"};

//      xAxis.setValueFormatter(new MyXAxisValueFormatter(values));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tfLight);
        leftAxis.setValueFormatter(new LargeValueFormatter());
        leftAxis.setDrawGridLines(false);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)


        //chart.animateY(1500);

        chart.getAxisRight().setEnabled(false);
    }

    public void updateCurDate(){
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int date = cal.get(Calendar.DATE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR);
        int minute = cal.get(Calendar.MINUTE);

        String korDayOfWeek = "";
        switch (dayOfWeek) {
            case 1:
                korDayOfWeek = "일";
                break;
            case 2:
                korDayOfWeek = "월";
                break;
            case 3:
                korDayOfWeek = "화";
                break;
            case 4:
                korDayOfWeek = "수";
                break;
            case 5:
                korDayOfWeek = "목";
                break;
            case 6:
                korDayOfWeek = "금";
                break;
            case 7:
                korDayOfWeek = "토";
                break;
        }

        curDate.setText(year + ". " + month + ". " + date + ". " + "(" + korDayOfWeek + ") " + hour + "시 " + minute + "분");
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                chart.setX(getConvertedX(0));
                chart.setY(getConvertedY(190));
                //리스너 해제
                chart.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvStepValueAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepValueAnalLabel.setX(getConvertedX(60));
                mTvStepValueAnalLabel.setY(getConvertedY(1190));
                //리스너 해제
                mTvStepValueAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepValueAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepValueAnal .setX(getConvertedX(60));
                mTvStepValueAnal .setY(getConvertedY(1280));
                //리스너 해제
                mTvStepValueAnal .getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftSymmetryAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftSymmetryAnalLabel.setX(getConvertedX(60));
                mTvLeftSymmetryAnalLabel.setY(getConvertedY(1440));
                //리스너 해제
                mTvLeftSymmetryAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftSymmetryAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftSymmetryAnal.setX(getConvertedX(60));
                mTvLeftSymmetryAnal.setY(getConvertedY(1530));
                //리스너 해제
                mTvLeftSymmetryAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftTimeAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftTimeAnalLabel.setX(getConvertedX(60));
                mTvLeftTimeAnalLabel.setY(getConvertedY(1690));
                //리스너 해제
                mTvLeftTimeAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftTimeAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftTimeAnal.setX(getConvertedX(60));
                mTvLeftTimeAnal.setY(getConvertedY(1780));
                //리스너 해제
                mTvLeftTimeAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepTimeAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepTimeAnalLabel.setX(getConvertedX(60));
                mTvStepTimeAnalLabel.setY(getConvertedY(1940));
                //리스너 해제
                mTvStepTimeAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepTimeAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepTimeAnal.setX(getConvertedX(60));
                mTvStepTimeAnal.setY(getConvertedY(2030));
                //리스너 해제
                mTvStepTimeAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvRightSymmetryAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightSymmetryAnalLabel.setX(getConvertedX(1130));
                mTvRightSymmetryAnalLabel.setY(getConvertedY(1440));
                //리스너 해제
                mTvRightSymmetryAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightSymmetryAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightSymmetryAnal.setX(getConvertedX(1130));
                mTvRightSymmetryAnal.setY(getConvertedY(1530));
                //리스너 해제
                mTvRightSymmetryAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightTimeAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightTimeAnalLabel.setX(getConvertedX(1130));
                mTvRightTimeAnalLabel.setY(getConvertedY(1690));
                //리스너 해제
                mTvRightTimeAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightTimeAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightTimeAnal.setX(getConvertedX(1130));
                mTvRightTimeAnal.setY(getConvertedY(1780));
                //리스너 해제
                mTvRightTimeAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvFatigueAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvFatigueAnalLabel.setX(getConvertedX(1130));
                mTvFatigueAnalLabel.setY(getConvertedY(1940));
                //리스너 해제
                mTvFatigueAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvFatigueAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvFatigueAnal.setX(getConvertedX(1130));
                mTvFatigueAnal.setY(getConvertedY(2030));
                //리스너 해제
                mTvFatigueAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvConPressAnalLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvConPressAnalLabel.setX(getConvertedX(600));
                mTvConPressAnalLabel.setY(getConvertedY(2190));
                //리스너 해제
                mTvConPressAnalLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvConPressAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvConPressAnal.setX(getConvertedX(600));
                mTvConPressAnal.setY(getConvertedY(2280));
                //리스너 해제
                mTvConPressAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mIvLeftPressureStateAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvLeftPressureStateAnal.setX(getConvertedX(490));
                mIvLeftPressureStateAnal.setY(getConvertedY(1740));
                //리스너 해제
                mIvLeftPressureStateAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mIvRightPressureStateAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvRightPressureStateAnal.setX(getConvertedX(810));
                mIvRightPressureStateAnal.setY(getConvertedY(1740));
                //리스너 해제
                mIvRightPressureStateAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvRightPressureAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightPressureAnal.setX(getConvertedX(775));
                mTvRightPressureAnal.setY(getConvertedY(1600));
                //리스너 해제
                mTvRightPressureAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvLeftPressureAnal.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftPressureAnal.setX(getConvertedX(415));
                mTvLeftPressureAnal.setY(getConvertedY(1600));
                //리스너 해제
                mTvLeftPressureAnal.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public int getConvertedX(int orign) {
        Log.i("TestFragGetCvrt", "c_width = " + mParentFragment.getFragmentWidth() + "c_height = "+ mParentFragment.getFragmentHeight());
        return (orign* mParentFragment.getFragmentWidth())/BASE_RESOLUSION_WIDTH;
    }

    public int getConvertedY(int orign) {
        return (orign* mParentFragment.getFragmentHeight())/BASE_RESOLUSION_HEIGHT;
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
            //final UUID uuid = UUID.fromString(cursor.getString(2 /* UUID */));
            final UUID uuid = null;
            //final int major = cursor.getInt(3 /* MAJOR */);
            //final int minor = cursor.getInt(4 /* MINOR */);
            final int major = -1;
            final int minor = -1;
            final int event = cursor.getInt(6 /* EVENT */);

            // We must start ranging for all beacons
            serviceConnection.startRangingBeaconsInRegion(AnalysisFragment.BEACON_COMPANY_ID, uuid, major, minor, this);
            //serviceConnection.startRangingBeaconsInRegion(uuid, major, minor, this);
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

                try {
                    if (cursor.moveToNext()) {

                        // Update signal strength in the database
                        float accuracy = 5;
                        for (final Beacon beacon : beacons)
                            if (Proximity.UNKNOWN != beacon.getProximity() && beacon.getAccuracy() < accuracy)
                                accuracy = beacon.getAccuracy();
                        accuracy = -20 * accuracy + 100;

                        //update Signal Strength
                        mDatabaseHelper.updateRegionSignalStrength(cursor.getLong(0 /* _ID */), (int) accuracy);

                        //Update foot pressure
                        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        final String lValue = preferences.getString("leftBeacon", "none");
                        final String rValue = preferences.getString("rightBeacon", "none");

                        if (!(lValue.equals("none")) && !(rValue.equals("none")) && isAnalyzing) {

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
                                selFunction[sampleCount] = 2;


                                if ((sampleCount + 1) % 100 == 0) {
                                    pointValue[sampleCount] = 1;
                                } else
                                    pointValue[sampleCount] = 0;
                                int temp = sampleCount+1;
                                mTvStepValueAnal.setText("" + temp);
                                int point = sampleCount / 100 + pointDeductionAcc;
                                //mTvPoint.setText("" + point + " P");

                                long now = System.currentTimeMillis();
                                long downScaleDiff;
                                downScaleDiff = now - referenceTime;
                                leftMTime[leftSampleCount] = (float) downScaleDiff / 1000L;
                                leftPressureChart[leftSampleCount] = (byte) ((beacons[i].getMinor() / 4095f) * 120);
                                leftPressure[sampleCount] = (byte) ((beacons[i].getMinor() / 4095f) * 120);
                                //int temp = cursor.getInt(4 /* MINOR */);
                                //leftPressure[sampleCount] = (byte)temp;
                                //leftPressure[sampleCount] = (byte) accuracy;
                                rightPressure[sampleCount] = 0;
                                //Log.i("SAMPLE", "referenceTime :" + referenceTime + " downScaleDiff = " + String.format("%.2f", leftMTime[leftSampleCount]) + " accuracy = " + accuracy);
                                leftSampleCount++;

                                //Random generator = new Random();
                                changeCircleColor(mIvLeftPressureStateAnal, leftPressure[sampleCount]);
                                mTvLeftPressureAnal.setText(String.format(Locale.KOREA, "%d", leftPressure[sampleCount]));
                                //changeCircleColor(mIvLeftPressureState, generator.nextInt(105));
                                //Log.i("LEFT BEACONM", sampleCount + "번째 측정 " + "Pressure value: " + leftPressure[sampleCount] + " Major value:" + beacons[i].getMinor());
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
                                mTvStepValueAnal.setText("" + temp);
                                int point = sampleCount / 100 + pointDeductionAcc;
                                //mTvPoint.setText("" + point + " P");

                                long now = System.currentTimeMillis();
                                long downScaleDiff;
                                downScaleDiff = now - referenceTime;
                                rightMTime[rightSampleCount] = (float) downScaleDiff / 1000L;
                                rightPressureChart[rightSampleCount] = (byte) ((beacons[i].getMinor() / 4095f) * 120);
                                rightPressure[sampleCount] = (byte) ((beacons[i].getMinor() / 4095f) * 120);
                                //rightPressure[sampleCount] = (byte)cursor.getInt(4 /* MINOR */);
                                //rightPressure[sampleCount] = (byte) accuracy;
                                leftPressure[sampleCount] = 0;
                                //Log.i("SAMPLE", "referenceTime :" + referenceTime + " downScaleDiff = " + String.format("%.2f", rightMTime[rightSampleCount]) + " accuracy = " + accuracy);
                                changeCircleColor(mIvRightPressureStateAnal, rightPressure[sampleCount]);
                                mTvRightPressureAnal.setText(String.format(Locale.KOREA, "%d", rightPressure[sampleCount]));
                               // Log.i("RIGHT BEACONM", sampleCount + "번째 측정 " + " Pressure value: " + rightPressure[sampleCount] + " Major value:" + beacons[i].getMinor());
                                rightSampleCount++;
                            }

                            updateSymMetrics();

                        //    currentSpeed[sampleCount] = (float) curHeight;
                        //    currentHeight[sampleCount] = (float) sampledSpeed;

                            sampleCount++;
                            drawChart();

                        } else {
                            // Toast toast = Toast.makeText(getActivity(), "비콘이 등록되어 있지 않습니다.", Toast.LENGTH_LONG);
                            //toast.show();
                        }
                  /*      if (!(lValue.equals("none")) && !(rValue.equals("none")) && isAnalyzing) {
                            if (lValue.equals(cursor.getString(1)) && leftSampleCount < MAX_SAMPLES) {
                                long now = System.currentTimeMillis();
                                long downScaleDiff;
                                downScaleDiff = now - referenceTime;
                                leftMTime[leftSampleCount] = (float) downScaleDiff / 1000L;
                                leftPressure[leftSampleCount] = (int) accuracy;
                                updateMetrics();
                                Log.i("SAMPLE", "referenceTime :" + referenceTime + " downScaleDiff = " + String.format("%.2f", leftMTime[leftSampleCount]) + " accuracy = " + accuracy);
                                Random generator = new Random();
                                //changeCircleColor(mIvLeftPressureState, leftPressure[leftSampleCount]);
                                changeCircleColor(mIvLeftPressureStateAnal, generator.nextInt(105));
                                Log.i("LEFT BEACON", leftSampleCount + "번째 측정 " + leftPressure[leftSampleCount]);
                                leftSampleCount++;
                            } else if (rValue.equals(cursor.getString(1)) && rightSampleCount < MAX_SAMPLES) {
                                long now = System.currentTimeMillis();
                                long downScaleDiff;
                                downScaleDiff = now - referenceTime;
                                rightMTime[rightSampleCount] = (float) downScaleDiff / 1000L;
                                rightPressure[rightSampleCount] = (int) accuracy;
                                updateMetrics();
                                Log.i("SAMPLE", "referenceTime :" + referenceTime + " downScaleDiff = " + String.format("%.2f", rightMTime[rightSampleCount]) + " accuracy = " + accuracy);
                                changeCircleColor(mIvRightPressureStateAnal, rightPressure[rightSampleCount]);
                                Log.i("RIGHT BEACON", rightSampleCount + "번째 측정 " + rightPressure[rightSampleCount]);
                                rightSampleCount++;
                            }
                            drawChart();
                        } else {
                            Toast toast = Toast.makeText(getActivity(), "비콘이 등록되어 있지 않습니다.", Toast.LENGTH_LONG);
                            toast.show();
                        }*/

                    }
                } finally {
                    cursor.close();
                }
                mAdapter.swapCursor(mDatabaseHelper.getAllRegions());
            }
        }
    }
/*
    public void updateMetrics() {
        int leftSymmetry = 0, rightSymmetry = 0, leftTime = 0, rightTime = 0, stepTime = 0, symmetryFatigue = 0;

        if (leftSampleCount > 0 && rightSampleCount > 0) {
            leftSymmetry = (leftPressure[leftSampleCount] / ((leftPressure[leftSampleCount] + rightPressure[rightSampleCount]) / 2)) * 100;
            rightSymmetry = (rightPressure[leftSampleCount] / ((leftPressure[leftSampleCount] + rightPressure[rightSampleCount]) / 2)) * 100;
            stepTime = (int) ((leftMTime[leftSampleCount] > rightMTime[rightSampleCount]) ? (leftMTime[leftSampleCount] - rightMTime[rightSampleCount]) / 100 : (rightMTime[rightSampleCount] - leftMTime[leftSampleCount]) / 100);
            symmetryFatigue = (leftSymmetry > rightSymmetry) ? (leftSymmetry - rightSymmetry) : (rightSymmetry - leftSymmetry);
        }

        if (leftSampleCount > 1)
            leftTime = (int) ((leftMTime[leftSampleCount] - leftMTime[leftSampleCount - 1]) / 100);
        if (rightSampleCount > 1)
            rightTime = (int) ((rightMTime[rightSampleCount] - rightMTime[rightSampleCount - 1]) / 100);

        mTvLeftSymmetryAnal.setText(Integer.toString(leftSymmetry));
        mTvRightSymmetryAnal.setText(Integer.toString(rightSymmetry));
        mTvLeftTimeAnal.setText(Integer.toString(leftTime));
        mTvRightTimeAnal.setText(Integer.toString(rightTime));
        mTvStepTimeAnal.setText(Integer.toString(stepTime));
        mTvFatigueAnal.setText(Integer.toString(symmetryFatigue));
    }
*/
public void updateSymMetrics() {
    byte leftPressureRef = 0, rightPressureRef = 0;
    long leftMTimeRef = 0, rightMTimeRef = 0, leftMTimeRefPrev = 0, rightMTimeRefPrev = 0;
    float leftSymmetry = 0, rightSymmetry = 0, leftTime = 0, rightTime = 0, stepTime = 0, symmetryFatigue = 0;

    if (sampleCount > 0) {
        if ((leftPressure[sampleCount] >= LEFT_PRESSURE_THRESHOLD || rightPressure[sampleCount] >= RIGHT_PRESSURE_THRESHOLD)) {
            if ((leftPressure[sampleCount - 1] >= LEFT_PRESSURE_THRESHOLD || rightPressure[sampleCount - 1] >= RIGHT_PRESSURE_THRESHOLD)) {
                continuousPressureAccTime += dateTimeStamp[sampleCount] - dateTimeStamp[sampleCount - 1];
                pointDeductionAcc = pointDeductionAcc - POINT_DEDUCT_UNIT;
                //mTvPointDeduc.setText(String.format(Locale.KOREA, "%d P", pointDeductionAcc));
                continuousPressureCount++;
                pointDeduction[sampleCount] = -2;
            } else {
                continuousPressureAccTime = 0;
            }

            if (isAnalyzing && continuousPressureAccTime > 0) {
                mTvConPressAnal.setText(String.format(Locale.KOREA, "%.2f", (float) continuousPressureAccTime / 1000));
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

    if (isAnalyzing) {
        mTvLeftSymmetryAnal.setText(String.format(Locale.KOREA, "%.2f %%", leftSymmetry));
        mTvRightSymmetryAnal.setText(String.format(Locale.KOREA, "%.2f %%", rightSymmetry));
        mTvLeftTimeAnal.setText(String.format(Locale.KOREA, "%.2f sec", leftTime));
        mTvRightTimeAnal.setText(String.format(Locale.KOREA, "%.2f sec", rightTime));
        mTvStepTimeAnal.setText(String.format(Locale.KOREA, "%.2f sec", stepTime));
        mTvFatigueAnal.setText(String.format(Locale.KOREA, "%.2f %%", symmetryFatigue));
    }
}
    public void changeCircleColor(ImageView imgView, int footPressure) {
        GradientDrawable bgShape = (GradientDrawable)imgView.getBackground();
        if(footPressure >= 0 && footPressure <= 14)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel1));
        else if(footPressure >= 15 && footPressure <= 29)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel2));
        else if(footPressure >= 30 && footPressure <= 44)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel3));
        else if(footPressure >= 45 && footPressure <= 59)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel4));
        else if(footPressure >= 60 && footPressure <= 74)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel5));
        else if(footPressure >= 75 && footPressure <= 89)
            bgShape.setColor(getResources().getColor(R.color.footPressureLevel6));
        else if(footPressure >= 90 && footPressure <= 105)
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
        numSteps++;
   //     mTvStepValueAnal.setText(""+ numSteps);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        Log.i("Activity", "Selected: " + e.toString() + ", dataSet: " + h.getDataSetIndex());
    }

    @Override
    public void onNothingSelected() {
        Log.i("Activity", "Nothing selected.");
    }

    public void drawChart() {

        float groupSpace = 0.08f;
        float barSpace = 0.06f; // x4 DataSet
        float barWidth = 0.2f; // x4 DataSet
        // (0.2 + 0.06) * 4 + 0.08 = 1.00 -> interval per "group"

        int groupCount =leftSampleCount > rightSampleCount ? rightSampleCount : leftSampleCount;
        int startSec = 1;

        ArrayList<BarEntry> values1 = new ArrayList<>();
        ArrayList<BarEntry> values2 = new ArrayList<>();

        for (int i = 0; i < leftSampleCount; i++) {
            values1.add(new BarEntry(leftMTime[i], leftPressureChart[i]));
        }

        for (int i = 0; i < rightSampleCount; i++) {
            values2.add(new BarEntry(rightMTime[i], rightPressureChart[i]));
        }

        BarDataSet set1, set2;

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {

            set1 = (BarDataSet) chart.getData().getDataSetByIndex(0);
            set2 = (BarDataSet) chart.getData().getDataSetByIndex(1);
            set1.setValues(values1);
            set2.setValues(values2);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();

        } else {
            // create 4 DataSets
            set1 = new BarDataSet(values1, "Left");
            set1.setColor(Color.rgb(104, 241, 175));
            set2 = new BarDataSet(values2, "Right");
            set2.setColor(Color.rgb(241, 0, 0));

            BarData data = new BarData(set1, set2);
            data.setValueFormatter(new LargeValueFormatter());
            data.setValueTypeface(tfLight);

            chart.setData(data);
        }
        chart.setVisibleXRangeMaximum(100);
        // specify the width each bar should have
        chart.getBarData().setBarWidth(barWidth);

        // restrict the x-axis range
        chart.getXAxis().setAxisMinimum(startSec);

        // barData.getGroupWith(...) is a helper that calculates the width each group needs based on the provided parameters
        //chart.getXAxis().setAxisMaximum(startSec + chart.getBarData().getGroupWidth(groupSpace, barSpace) * groupCount);
        //chart.groupBars(startSec, groupSpace, barSpace);
        chart.moveViewToX(chart.getData().getEntryCount());
        //chart.invalidate();
    }

    public void getJsonResponsePost(JSONObject json) {
        String url = getResources().getString(R.string.json_post_url);
        Log.i("VOLLEY_TEST", "url = " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            if(response.getInt("cmd") == CMD_ANAL_ITEM){
                                JSONArray jArr = response.getJSONArray("rawData");

                                //화면 초기화
                                numSteps = 0;
                                continuousPressureAccTime = 0;
                                pointDeductionAcc = 0;
                                continuousPressureCount = 0;
                                sampleCount = 0;
                                leftSampleCount = 0;
                                rightSampleCount = 0;
                                gpsPauseTime = 0;
                                leftKeyCounterPrev = 0;
                                leftKeyCounterPrev = 0;

                                chart.fitScreen();

                                mTvStepValueAnal.setText("0");
                                mTvLeftSymmetryAnal.setText("0.00 %");
                                mTvRightSymmetryAnal.setText("0.00 %");
                                mTvLeftTimeAnal.setText("0.00 sec");
                                mTvRightTimeAnal.setText("0.00 sec");
                                mTvStepTimeAnal.setText("0.00 sec");
                                mTvFatigueAnal.setText("0.00 %");

                                //그래프 데이터 생성
                                long referenceTimeLeft=0;
                                long referenceTimeRight=0;

                                for(int i =0; i < jArr.length(); i++) {
                                    JSONArray item = jArr.getJSONArray(i);
                                    if(item.getInt(3) != 0){
                                        if(leftSampleCount==0) {
                                            referenceTimeLeft = item.getLong(0);
                                            leftPressureChart[leftSampleCount] = (byte) item.getInt(3);
                                            leftMTime[leftSampleCount] = 0;
                                        } else {
                                            leftPressureChart[leftSampleCount] = (byte) item.getInt(3);
                                            leftMTime[leftSampleCount] = (float)(item.getLong(0)-referenceTimeLeft)/1000L;
                                        }
                                        leftSampleCount++;
                                    } else {
                                        if(rightSampleCount==0) {
                                            referenceTimeRight = item.getLong(0);
                                            rightPressureChart[rightSampleCount]= (byte)item.getInt(4);
                                            rightMTime[rightSampleCount]= 0;
                                        } else{
                                            rightPressureChart[rightSampleCount] = (byte) item.getInt(4);
                                            rightMTime[rightSampleCount] = (float)(item.getLong(0)-referenceTimeRight)/1000L;
                                        }
                                        rightSampleCount++;
                                    }
                                }
                                //그래프 그리기
                                drawChart();

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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

        jsonObjectRequest.setTag(RESULT_REQ_TAG);
        requestQueue.add(jsonObjectRequest);
    }
}
