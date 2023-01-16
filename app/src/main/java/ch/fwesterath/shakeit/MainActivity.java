package ch.fwesterath.shakeit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Constants
    private final static int SAMPLING_RATE = 100000 ; // in microseconds
    private final static int DISPLAY_PERIOD = 30; // in seconds
    private final static int CHART_ENTRIES_LIMIT = SAMPLING_RATE / 100000 * DISPLAY_PERIOD; // equals 300 displayed entries

    private int SHAKE_INTESITY = 20;

    // Shaking Status
    private boolean isShaking = false;
    // High Score
    private float highscore = 0;

    // Shared Preferences
    private SharedPreferences sharedPreferences;
    private SensorManager sensorManager;
    private Sensor accSensor;

    // Chart
    LineChart chart;
    LineData lineData;
    private LineDataSet xAxisLineData = new LineDataSet(new LinkedList<>(), "x");

    // Values
    private float highX;
    private float lastX;
    private float deltaX;

    private int time;

    // TextViews
    private TextView shakeTextView;
    private TextView highscoreTextView;
    private TextView scoreTextView;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get View Components
        chart = findViewById(R.id.chart);
        shakeTextView = findViewById(R.id.shakingStatusText);
        highscoreTextView = findViewById(R.id.highscoreValue);
        scoreTextView = findViewById(R.id.scoreValue);

        // Set chart and AxisLine styling
        chart.setBackgroundColor(getResources().getColor(R.color.white));

        xAxisLineData.setLabel(getString(R.string.x_axis));
        xAxisLineData.setCircleColor(Color.RED);
        xAxisLineData.setColor(Color.RED);

        // Finish chart setup
        chart.setData(lineData);
        chart.invalidate();

        // Get SensorManager and Accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accSensor, SAMPLING_RATE);

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences("SHARED_PREFS", MODE_PRIVATE);
        highscore = sharedPreferences.getInt("highscore", 0);
        highscoreTextView.setText(String.valueOf(highscore));
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        addValuesToDataSets(sensorEvent.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    void addValuesToDataSets(float[] values) {
        time++;

        xAxisLineData.addEntry(new Entry(time, values[0]));
//        yAxisLineData.addEntry(new Entry(time, values[1]));
//        zAxisLineData.addEntry(new Entry(time, values[2]));

        if(xAxisLineData.getEntryCount() >= CHART_ENTRIES_LIMIT) {
            xAxisLineData.removeFirst();
        }

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(xAxisLineData);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }
}
