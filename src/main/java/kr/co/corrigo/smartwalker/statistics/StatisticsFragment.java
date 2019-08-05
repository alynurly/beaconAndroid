package kr.co.corrigo.smartwalker.statistics;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.LargeValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import kr.co.corrigo.smartwalker.R;
import kr.co.corrigo.smartwalker.common.BoardHelpFragment;
import kr.co.corrigo.smartwalker.common.RequestQueueSingleton;
import kr.co.corrigo.smartwalker.common.ValueFormatter;
import kr.co.corrigo.smartwalker.setting.SettingsActivity;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatisticsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatisticsFragment extends Fragment implements View.OnClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final int BASE_RESOLUSION_WIDTH = 1440;
    private static final int BASE_RESOLUSION_HEIGHT = 2530;
    static final String STAT_REQ_TAG = "RawDateREQ";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    //private TextView mTestTextView;
    private TextView mCurTimeView;

    private TextView mTvHealDistStatLabel, mTvHealDistStat, mTvStepValueStatLabel, mTvStepValueStat, mTvLeftSymmetryStatLabel, mTvLeftSymmetryStat, mTvLeftTimeStatLabel, mTvLeftTimeStat, mTvStepTimeStatLabel, mTvStepTimeStat, mTvPointStatLabel, mTvPointStat, mTvRightSymmetryStatLabel, mTvRightSymmetryStat, mTvRightTimeStatLabel, mTvRightTimeStat, mTvFatigueStatLabel, mTvFatigueStat, mTvCalConsumStatLabel, mTvCalConsumStat;
    private ImageView mIvLeftPressureStateStat, mIvRightPressureStateStat;

    //    private ToggleButton mBtnMin;
    private Button[] btn = new Button[5];
    private Button btn_unfocus;
    private int[] btn_id = {R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4};

    private Button leftBtn, rightBtn;

    private Calendar cal;

    private BarChart chart;
    protected Typeface tfLight;


    private int fragmentWidth;
    private int fragmentHeight;
    private int selectedBtnIndex;

    RequestQueue requestQueue;
    private int CMD_STAT_REQ = 5;

    private long referenceTime;
    private int leftSampleCount;
    private int rightSampleCount;
    private byte[] leftPressureChart;
    private byte[] rightPressureChart;
    private float[] leftMTime;
    private float[] rightMTime;

    private String dateBegin = null;
    private String dateEnd = null;

    public StatisticsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment StatisticsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StatisticsFragment newInstance(String param1, String param2) {
        StatisticsFragment fragment = new StatisticsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        requestQueue = RequestQueueSingleton.getInstance(getActivity().getApplicationContext()).getRequestQueue();
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);

        //Log.i("FragSize", "OriginX = " + mTestTextView.getX() + "OriginY = "+ mTestTextView.getY() );

        //ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mTestTextView.getLayoutParams();
        //params.setMargins(getConvertedX(520), getConvertedY(440), 0, 0);
        //mTvHealingSeconds.setX(getConvertedX(520));
        //mTvHealingSeconds.setY(getConvertedY(440));
/*
        mTestTextView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {


                //뷰의 생성된 후 크기와 위치 구하기
                float x;
                float y;
                Log.i("TreatFragOnView", "ConvetedX = " + mTestTextView.getX() + "(" + getConvertedX(525) + ")" + "ConvetedY = " + mTestTextView.getY() + "(" + getConvertedY(440) + ")");
                //ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams)mTestTextView.getLayoutParams();
                //params.setMargins(getConvertedX(520), getConvertedY(440), 0, 0);
                mTestTextView.setX(getConvertedX(1150));
                mTestTextView.setY(getConvertedY(2250));
                x = mTestTextView.getX();
                y = mTestTextView.getY();
                Log.i("TreatFragOnView", "cx= " + x + "cy=" + y);

                //리스너 해제
                mTestTextView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
*/
        chart.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                chart.setX(getConvertedX(0));
                chart.setY(getConvertedY(190));
                //리스너 해제
                chart.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvStepValueStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepValueStatLabel.setX(getConvertedX(60));
                mTvStepValueStatLabel.setY(getConvertedY(1190));
                //리스너 해제
                mTvStepValueStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepValueStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepValueStat.setX(getConvertedX(60));
                mTvStepValueStat.setY(getConvertedY(1280));
                //리스너 해제
                mTvStepValueStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftSymmetryStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftSymmetryStatLabel.setX(getConvertedX(60));
                mTvLeftSymmetryStatLabel.setY(getConvertedY(1440));
                //리스너 해제
                mTvLeftSymmetryStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftSymmetryStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftSymmetryStat.setX(getConvertedX(60));
                mTvLeftSymmetryStat.setY(getConvertedY(1530));
                //리스너 해제
                mTvLeftSymmetryStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftTimeStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftTimeStatLabel.setX(getConvertedX(60));
                mTvLeftTimeStatLabel.setY(getConvertedY(1690));
                //리스너 해제
                mTvLeftTimeStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvLeftTimeStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvLeftTimeStat.setX(getConvertedX(60));
                mTvLeftTimeStat.setY(getConvertedY(1780));
                //리스너 해제
                mTvLeftTimeStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepTimeStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepTimeStatLabel.setX(getConvertedX(60));
                mTvStepTimeStatLabel.setY(getConvertedY(1940));
                //리스너 해제
                mTvStepTimeStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvStepTimeStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvStepTimeStat.setX(getConvertedX(60));
                mTvStepTimeStat.setY(getConvertedY(2030));
                //리스너 해제
                mTvStepTimeStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvHealDistStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvHealDistStatLabel.setX(getConvertedX(60));
                mTvHealDistStatLabel.setY(getConvertedY(2190));
                //리스너 해제
                mTvHealDistStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvHealDistStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvHealDistStat.setX(getConvertedX(60));
                mTvHealDistStat.setY(getConvertedY(2280));
                //리스너 해제
                mTvHealDistStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvPointStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvPointStatLabel.setX(getConvertedX(1130));
                mTvPointStatLabel.setY(getConvertedY(1190));
                //리스너 해제
                mTvPointStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvPointStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvPointStat.setX(getConvertedX(1130));
                mTvPointStat.setY(getConvertedY(1280));
                //리스너 해제
                mTvPointStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightSymmetryStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightSymmetryStatLabel.setX(getConvertedX(1130));
                mTvRightSymmetryStatLabel.setY(getConvertedY(1440));
                //리스너 해제
                mTvRightSymmetryStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightSymmetryStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightSymmetryStat.setX(getConvertedX(1130));
                mTvRightSymmetryStat.setY(getConvertedY(1530));
                //리스너 해제
                mTvRightSymmetryStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightTimeStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightTimeStatLabel.setX(getConvertedX(1130));
                mTvRightTimeStatLabel.setY(getConvertedY(1690));
                //리스너 해제
                mTvRightTimeStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvRightTimeStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvRightTimeStat.setX(getConvertedX(1130));
                mTvRightTimeStat.setY(getConvertedY(1780));
                //리스너 해제
                mTvRightTimeStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvFatigueStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvFatigueStatLabel.setX(getConvertedX(1130));
                mTvFatigueStatLabel.setY(getConvertedY(1940));
                //리스너 해제
                mTvFatigueStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvFatigueStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvFatigueStat.setX(getConvertedX(1130));
                mTvFatigueStat.setY(getConvertedY(2030));
                //리스너 해제
                mTvFatigueStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mTvCalConsumStatLabel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCalConsumStatLabel.setX(getConvertedX(1130));
                mTvCalConsumStatLabel.setY(getConvertedY(2190));
                //리스너 해제
                mTvCalConsumStatLabel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mTvCalConsumStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTvCalConsumStat.setX(getConvertedX(1130));
                mTvCalConsumStat.setY(getConvertedY(2280));
                //리스너 해제

                float x, y;

                x = mTvCalConsumStat.getX();
                y = mTvCalConsumStat.getY();
                Log.i("TreatFragOnView", "mTvCalConsumStat: cx= " + x + "cy=" + y);

                mTvCalConsumStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        mIvLeftPressureStateStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvLeftPressureStateStat.setX(getConvertedX(490));
                mIvLeftPressureStateStat.setY(getConvertedY(1740));
                //리스너 해제
                mIvLeftPressureStateStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        mIvRightPressureStateStat.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mIvRightPressureStateStat.setX(getConvertedX(810));
                mIvRightPressureStateStat.setY(getConvertedY(1740));
                //리스너 해제
                mIvRightPressureStateStat.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        tfLight = Typeface.createFromAsset(getActivity().getAssets(), "fonts/OpenSans-Light.ttf");

        /*
        mBtnMin = (ToggleButton) view.findViewById(R.id.button_mn);

        mBtnMin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mBtnMin.isChecked()){
                    mBtnMin.setBackgroundResource(R.color.headerBackgroundColorDisabled);
                }else{
                    mBtnMin.setBackgroundResource(R.color.darkGray);
                }
            }
        }); */

        mTvStepValueStatLabel = (TextView) view.findViewById(R.id.tvStepValueStatLabel);
        mTvStepValueStat = (TextView) view.findViewById(R.id.tvStepValueStat);
        mTvLeftSymmetryStatLabel = (TextView) view.findViewById(R.id.tvLeftSymmetryStatLabel);
        mTvLeftSymmetryStat = (TextView) view.findViewById(R.id.tvLeftSymmetryStat);
        mTvLeftTimeStatLabel = (TextView) view.findViewById(R.id.tvLeftTimeStatLabel);
        mTvLeftTimeStat = (TextView) view.findViewById(R.id.tvLeftTimeStat);
        mTvStepTimeStatLabel = (TextView) view.findViewById(R.id.tvStepTimeStatLabel);
        mTvStepTimeStat = (TextView) view.findViewById(R.id.tvStepTimeStat);
        mTvHealDistStatLabel = (TextView) view.findViewById(R.id.tvHealDistStatLabel);
        mTvHealDistStat = (TextView) view.findViewById(R.id.tvHealDistStat);

        mTvPointStatLabel = (TextView) view.findViewById(R.id.tvPointStatLabel);
        mTvPointStat = (TextView) view.findViewById(R.id.tvPointStat);
        mTvRightSymmetryStatLabel = (TextView) view.findViewById(R.id.tvRightSymmetryStatLabel);
        mTvRightSymmetryStat = (TextView) view.findViewById(R.id.tvRightSymmetryStat);
        mTvRightTimeStatLabel = (TextView) view.findViewById(R.id.tvRightTimeStatLabel);
        mTvRightTimeStat = (TextView) view.findViewById(R.id.tvRightTimeStat);
        mTvFatigueStatLabel = (TextView) view.findViewById(R.id.tvFatigueStatLabel);
        mTvFatigueStat = (TextView) view.findViewById(R.id.tvFatigueStat);
        mTvCalConsumStatLabel = (TextView) view.findViewById(R.id.tvCalConsumStatLabel);
        mTvCalConsumStat = (TextView) view.findViewById(R.id.tvCalConsumStat);

        mIvLeftPressureStateStat = (ImageView) view.findViewById(R.id.leftPressureStateStat);
        mIvRightPressureStateStat = (ImageView) view.findViewById(R.id.rightPressureStateStat);

        for (int i = 0; i < btn.length; i++) {
            btn[i] = (Button) view.findViewById(btn_id[i]);
            btn[i].setBackgroundColor(Color.rgb(207, 207, 207));
            btn[i].setOnClickListener(this);
        }

        btn_unfocus = btn[0];
        selectedBtnIndex = 0;
        btn_unfocus.setTextColor(Color.rgb(255, 255, 255));
        btn_unfocus.setBackgroundColor(Color.rgb(3, 106, 150));

        mCurTimeView = (TextView) view.findViewById(R.id.targetTime);

        cal = Calendar.getInstance();
        //임시설정
        cal.set(2019, 1, 1, 1, 1, 0);
        cal.set(Calendar.MILLISECOND, 0);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
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

        mCurTimeView.setText(year + ". " + month + ". " + date + ". " + "(" + korDayOfWeek + ") " + hour + "시 " + minute + "분");


        //mTestTextView = (TextView) view.findViewById(R.id.textView5);

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {


                //뷰의 생성된 후 크기와 위치 구하기
                fragmentWidth = view.getWidth();
                fragmentHeight = view.getHeight();
                Log.i("TreatFrag_onCreateView", "width = " + fragmentWidth + "height = " + fragmentHeight);

                //리스너 해제
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        leftBtn = (Button) view.findViewById(R.id.btn_left);
        rightBtn = (Button) view.findViewById(R.id.btn_right);

        leftBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                switch (selectedBtnIndex) {
                    case 0:
                        cal.add(Calendar.MINUTE, -1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 1:
                        cal.add(Calendar.HOUR, -1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 2:
                        cal.add(Calendar.DATE, -1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 3:
                        cal.add(Calendar.MONTH, -1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 4:
                        cal.add(Calendar.YEAR, -1);
                        updateDateString(selectedBtnIndex);
                        break;
                }
                requestCharData();
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                switch (selectedBtnIndex) {
                    case 0:
                        cal.add(Calendar.MINUTE, 1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 1:
                        cal.add(Calendar.HOUR, 1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 2:
                        cal.add(Calendar.DATE, 1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 3:
                        cal.add(Calendar.MONTH, 1);
                        updateDateString(selectedBtnIndex);
                        break;
                    case 4:
                        cal.add(Calendar.YEAR, 1);
                        updateDateString(selectedBtnIndex);
                        break;
                }

                requestCharData();
            }
        });


        //Chart settings
        chart = (BarChart) view.findViewById(R.id.chart2);
        chart.getDescription().setEnabled(false);
        //chart.setDragEnabled(true);
        //chart.setScaleEnabled(true);

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
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);

        xAxis.setDrawGridLines(false);
        //xAxis.setAvoidFirstLastClipping(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

       /* class MyXAxisValueFormatter implements IAxisValueFormatter {

            private String[] mValues;

            public MyXAxisValueFormatter(String[] values) {
                this.mValues = values;
            }

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // "value" represents the position of the label on the axis (x or y)
                return mValues[(int) value];
            }

            public int getDecimalDigits() { return 0; }
        }
*/
        // String[] values = new String[] {"a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c","a", "b", "c"};

//      xAxis.setValueFormatter(new MyXAxisValueFormatter(values));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTypeface(tfLight);
        leftAxis.setValueFormatter(new LargeValueFormatter());
        leftAxis.setDrawGridLines(false);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)


        //chart.animateY(1500);

        chart.getAxisRight().setEnabled(false);

        // Inflate the layout for this fragment
        return view;
    }

    public int getConvertedX(int orign) {
        Log.i("TreatFragGetCvrt", "c_width = " + fragmentWidth + "c_height = " + fragmentHeight);
        return (orign * fragmentWidth) / BASE_RESOLUSION_WIDTH;
    }

    public int getConvertedY(int orign) {
        return (orign * fragmentHeight) / BASE_RESOLUSION_HEIGHT;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (requestQueue != null) {
            requestQueue.cancelAll(STAT_REQ_TAG);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //final String value = preferences.getString("leftBeacon", "none");
        //mTestTextView.setText(value);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.settings, menu);
        inflater.inflate(R.menu.about, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                final Intent intent = new Intent(getActivity(), SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                return true;
            case R.id.action_about:
                final BoardHelpFragment helpFragment = BoardHelpFragment.getInstance(BoardHelpFragment.MODE_DFU);
                helpFragment.show(getChildFragmentManager(), null);
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        //setForcus(btn_unfocus, (Button) findViewById(v.getId()));
        //Or use switch
        //
        //cal = Calendar.getInstance();
        XAxis xAxis = chart.getXAxis();
        Log.i("DATECHECK", "onclick called");
        switch (v.getId()) {
            case R.id.button_0:
                setFocus(btn_unfocus, btn[0]);
                selectedBtnIndex = 0;
                xAxis.setLabelCount(6);
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value) + "초";
                    }
                });
                updateDateString(selectedBtnIndex);
                break;

            case R.id.button_1:
                setFocus(btn_unfocus, btn[1]);
                selectedBtnIndex = 1;
                xAxis.setLabelCount(12);
                xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value) + "분";
                    }
                });
                updateDateString(selectedBtnIndex);
                break;

            case R.id.button_2:
                setFocus(btn_unfocus, btn[2]);
                selectedBtnIndex = 2;
         /*       xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value) + "시";
                    }
                });*/
                updateDateString(selectedBtnIndex);
                break;

            case R.id.button_3:
                setFocus(btn_unfocus, btn[3]);
                selectedBtnIndex = 3;
          /*      xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value) + "일";
                    }
                });*/
                updateDateString(selectedBtnIndex);
                break;

            case R.id.button_4:
                setFocus(btn_unfocus, btn[4]);
                selectedBtnIndex = 4;
        /*        xAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value) + "월";
                    }
                });*/
                updateDateString(selectedBtnIndex);
                break;
        }
        requestCharData();
    }

    private void updateDateString(int curBtnIndex) {
        Log.i("DATECHECK", "updateDateSting called");
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int date = cal.get(Calendar.DATE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        Calendar referenceTimeLocal = Calendar.getInstance();

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

        switch (curBtnIndex) {
            case 0:
                mCurTimeView.setText(year + ". " + month + ". " + date + ". " + "(" + korDayOfWeek + ") " + hour + "시 " + minute + "분");
                dateBegin = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, date, hour, minute);
                dateEnd = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, date, hour, minute + 1);
                referenceTimeLocal.set(year, month, date, hour, minute,0);
                referenceTimeLocal.set(Calendar.MILLISECOND, 0);
                referenceTime = referenceTimeLocal.getTimeInMillis();
                Log.i("DATECHECK", dateBegin + ", " + dateEnd);
                break;
            case 1:
                mCurTimeView.setText(year + ". " + month + ". " + date + ". " + "(" + korDayOfWeek + ") " + hour + "시");
                dateBegin = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, date, hour, 0);
                dateEnd = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, date, hour + 1, 0);
                referenceTimeLocal.set(year, month, date, hour, 0,0);
                referenceTimeLocal.set(Calendar.MILLISECOND, 0);
                referenceTime = referenceTimeLocal.getTimeInMillis();
                Log.i("DATECHECK", dateBegin + ", " + dateEnd);
                break;
            case 2:
                mCurTimeView.setText(year + ". " + month + ". " + date + ". " + "(" + korDayOfWeek + ")");
                dateBegin = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, date, 0, 0);
                dateEnd = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, date + 1, 0, 0);
                referenceTimeLocal.set(year, month, date, 0, 0,0);
                referenceTimeLocal.set(Calendar.MILLISECOND, 0);
                referenceTime = referenceTimeLocal.getTimeInMillis();
                Log.i("DATECHECK", dateBegin + ", " + dateEnd);
                break;
            case 3:
                mCurTimeView.setText(year + ". " + month + ".");
                dateBegin = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month, 1, 0, 0);
                dateEnd = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, month + 1, 1, 0, 0);
                referenceTimeLocal.set(year, month, 1, 0, 0,0);
                referenceTimeLocal.set(Calendar.MILLISECOND, 0);
                referenceTime = referenceTimeLocal.getTimeInMillis();
                Log.i("DATECHECK", dateBegin + ", " + dateEnd);
                break;
            case 4:
                mCurTimeView.setText(year + ".");
                dateBegin = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year, 1, 1, 0, 0);
                dateEnd = String.format(Locale.KOREA, "%04d-%02d-%02d %02d:%02d", year + 1, 1, 1, 0, 0);
                referenceTimeLocal.set(year, 1, 1, 0, 0, 0);
                referenceTimeLocal.set(Calendar.MILLISECOND, 0);
                referenceTime = referenceTimeLocal.getTimeInMillis();
                Log.i("DATECHECK", dateBegin + ", " + dateEnd + " referenceTime" + referenceTime);
                break;
        }
    }

    private void requestCharData() {

        JSONObject graphDataRequest = new JSONObject();
        JSONArray referenceDates = new JSONArray();
        try {
            graphDataRequest.put("id", "corrigo1");
            graphDataRequest.put("pass", "12345678!!");
            graphDataRequest.put("cmd", CMD_STAT_REQ);
            graphDataRequest.put("numSamples", 1);
            referenceDates.put(0, dateBegin);
            referenceDates.put(1, dateEnd);
            graphDataRequest.put("referenceDates", referenceDates);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getJsonResponsePost(graphDataRequest);
    }

    private void setFocus(Button btn_unfocus, Button btn_focus) {
        btn_unfocus.setTextColor(Color.rgb(49, 50, 51));
        btn_unfocus.setBackgroundColor(Color.rgb(207, 207, 207));
        btn_focus.setTextColor(Color.rgb(255, 255, 255));
        btn_focus.setBackgroundColor(Color.rgb(3, 106, 150));
        this.btn_unfocus = btn_focus;
    }

    private void drawGroupChart(byte[] leftArr, byte[] rightArr) {
        float groupSpace = 0.08f;
        float barSpace = 0.06f; // x4 DataSet
        float barWidth = 0.4f; // x4 DataSet
        // (0.2 + 0.03) * 4 + 0.08 = 1.00 -> interval per "group"

        int groupCount;
        if (leftArr.length > rightArr.length) {
            groupCount = rightArr.length;
        } else {
            groupCount = leftArr.length;
        }
        int startValue = 1;

        ArrayList<BarEntry> values1 = new ArrayList<>();
        ArrayList<BarEntry> values2 = new ArrayList<>();

        for (int i = 0; i < groupCount; i++) {
            values1.add(new BarEntry(i, leftArr[i]));
            values2.add(new BarEntry(i, rightArr[i]));
            Log.i("GRAPH_DATA", i + "번째 데이터: left=" + leftArr[i] + " right=" + rightArr[i]);
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
            set2.setColor(Color.rgb(164, 228, 251));

            BarData data = new BarData(set1, set2);
            data.setValueFormatter(new LargeValueFormatter());
            data.setValueTypeface(tfLight);

            chart.setData(data);
        }

        // specify the width each bar should have
        chart.getBarData().setBarWidth(barWidth);

        // restrict the x-axis range
        chart.getXAxis().setAxisMinimum(startValue);

        // barData.getGroupWith(...) is a helper that calculates the width each group needs based on the provided parameters
        chart.getXAxis().setAxisMaximum(startValue + chart.getBarData().getGroupWidth(groupSpace, barSpace) * groupCount);
        chart.groupBars(startValue, groupSpace, barSpace);
        chart.invalidate();
    }

    public void drawChart() {

        float groupSpace = 0.08f;
        float barSpace = 0.06f; // x4 DataSet
        float barWidth = 0.2f; // x4 DataSet
        // (0.2 + 0.06) * 4 + 0.08 = 1.00 -> interval per "group"

        int groupCount = leftSampleCount > rightSampleCount ? rightSampleCount : leftSampleCount;
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
                            Log.i("VOLLEY_TEST", "On Response");
                            if (response.getInt("cmd") == CMD_STAT_REQ) {
                                JSONArray jArr = null;
                                try {
                                    jArr = response.getJSONArray("rawData");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                chart.fitScreen();

                                int numSamples = response.getInt("numSamples");

                                if (selectedBtnIndex == 0) {
                                    leftSampleCount = 0;
                                    rightSampleCount = 0;
                                    leftPressureChart = new byte[numSamples];
                                    rightPressureChart = new byte[numSamples];
                                    leftMTime = new float[numSamples];
                                    rightMTime = new float[numSamples];

                                    //그래프 데이터 생성
                                    Log.i("drawChart", "jArr length:" + jArr.length());
                                    for (int i = 0; i < jArr.length(); i++) {
                                        JSONArray item = jArr.getJSONArray(i);
                                        if (item.getInt(3) != 0) {
                                            leftPressureChart[leftSampleCount] = (byte) item.getInt(3);
                                            long temp = item.getLong(0);
                                            //temp = temp - 122000;
                                            leftMTime[leftSampleCount] = (temp - referenceTime) / 1000f;
                                            //Log.i("drawChart", "temp" + temp + " rT: " + referenceTime + " leftMTime:" + leftMTime[leftSampleCount]);
                                            leftSampleCount++;
                                        } else {
                                            rightPressureChart[rightSampleCount] = (byte) item.getInt(4);
                                            long temp = item.getLong(0);
                                            //temp = temp - 122000;
                                            rightMTime[rightSampleCount] = (temp- referenceTime) / 1000f;
                                            //Log.i("drawChart", "temp" + temp + " rT: " + referenceTime + " rightMTime:" + rightMTime[rightSampleCount]);
                                            rightSampleCount++;
                                        }
                                    }
                                    drawChart();
                                } else {
                                    //그래프 데이터 생성
                                    byte[] leftGroupData = new byte[0], rightGroupData = new byte[0];

                                    if (selectedBtnIndex == 1) {
                                        leftGroupData = new byte[60];
                                        rightGroupData = new byte[60];

                                        for (int i = 0; i < jArr.length(); i++) {
                                            JSONArray item = jArr.getJSONArray(i);

                                            int index = (int) ((item.getLong(0) - referenceTime) / 1000 / 60);
                                            if (item.getInt(3) != 0) {
                                                leftGroupData[index] = (byte) item.getInt(3);
                                            } else {
                                                rightGroupData[index] = (byte) item.getInt(4);
                                            }
                                        }
                                    }

                                    if (selectedBtnIndex == 2) {
                                        leftGroupData = new byte[24];
                                        rightGroupData = new byte[24];

                                        for (int i = 0; i < jArr.length(); i++) {
                                            JSONArray item = jArr.getJSONArray(i);

                                            int index = (int) ((item.getLong(0) - referenceTime) / 1000 / 60 / 60);
                                            if (item.getInt(3) != 0) {
                                                leftGroupData[index] = (byte) item.getInt(3);
                                            } else {
                                                rightGroupData[index] = (byte) item.getInt(4);
                                            }
                                        }
                                    }

                                    if (selectedBtnIndex == 3) {
                                        leftGroupData = new byte[31];
                                        rightGroupData = new byte[31];

                                        for (int i = 0; i < jArr.length(); i++) {
                                            JSONArray item = jArr.getJSONArray(i);

                                            int index = (int) ((item.getLong(0) - referenceTime) / 1000 / 60 / 60 / 24);
                                            if (item.getInt(3) != 0) {
                                                leftGroupData[index] = (byte) item.getInt(3);
                                            } else {
                                                rightGroupData[index] = (byte) item.getInt(4);
                                            }
                                        }
                                    }

                                    if (selectedBtnIndex == 4) {
                                        leftGroupData = new byte[12];
                                        rightGroupData = new byte[12];

                                        for (int i = 0; i < jArr.length(); i++) {
                                            JSONArray item = jArr.getJSONArray(i);

                                            Log.i("VOLLEY_TEST", "Btn 4  item.getLong(0): " + item.getLong(0) + " cal.getTimeInMillis(): " + referenceTime);
                                            int days = (int) ((item.getLong(0) - referenceTime) / 1000 / 60 / 60 / 24);
                                            float indexD = days / 31f;
                                            int index = Math.round(indexD);
                                            Log.i("VOLLEY_TEST", "Btn 4 days: " + days + " indexD: " + indexD + " index: " + index);
                                            if (item.getInt(3) != 0) {
                                                leftGroupData[index] = (byte) item.getInt(3);
                                            } else {
                                                rightGroupData[index] = (byte) item.getInt(4);
                                            }
                                        }
                                    }
                                    drawGroupChart(leftGroupData, rightGroupData);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
            }
        });
        jsonObjectRequest.setTag(STAT_REQ_TAG);
        requestQueue.add(jsonObjectRequest);
    }
}
