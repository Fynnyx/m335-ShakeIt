package ch.fwesterath.shakeit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

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

    private static final String NOTIF_CHANNEL_ID = "shakeit_channel";
    private static final String NOTIF_CHANNEL_NAME = "ShakeIt Channel";

    private final int SHAKE_INTESITY = 50;
    private final int MIN_SHAKE_INTESITY = 10;


    // Shaking Status
    private boolean isShaking = false;
    // High Score
    private int highscore = 0;
    // Current Score
    private int score = 0;

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

    // Notification
    private NotificationManager notificationManager;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get View Components
        chart = findViewById(R.id.chart);
        shakeTextView = findViewById(R.id.shakingStatusText);
        highscoreTextView = findViewById(R.id.highscoreValue);
        scoreTextView = findViewById(R.id.scoreValue);

        // Setup Buttons
        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(v -> {
            resetHighscore();
        });




        // Set chart and AxisLine styling
        chart.setBackgroundColor(getResources().getColor(R.color.white));
        chart.getAxisLeft().mAxisMinimum = -10;

        xAxisLineData.setLabel(getString(R.string.x_axis));
        xAxisLineData.setCircleColor(Color.RED);
        xAxisLineData.setColor(Color.RED);

        // Finish chart setup
        chart.setData(lineData);
        chart.invalidate();

        // Get SensorManager and Accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Get SharedPreferences
        sharedPreferences = getSharedPreferences("SHARED_PREFS", MODE_PRIVATE);
        highscore = sharedPreferences.getInt("highscore", 0);
        highscoreTextView.setText(String.valueOf(highscore));
        scoreTextView.setText(String.valueOf(score));

        this.notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, NOTIF_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accSensor, SAMPLING_RATE);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        addValuesToDataSets(sensorEvent.values);
        newShakeDetection(sensorEvent.values);
        setTextViews();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //
    void detectShake(float[] values) {
        float currentValue = values[0];
        deltaX = Math.abs(lastX - currentValue);
        if ((deltaX > SHAKE_INTESITY) || (isShaking && deltaX > 5)) {
            isShaking = true;
            score += deltaX;
        } else if (isShaking && deltaX < SHAKE_INTESITY) {
            if (isShaking) {
                setHighscore();
            }
            isShaking = false;
            score = 0;
        }
        lastX = currentValue;

    }

    void setHighscore() {
        if (score > highscore) {
            highscore = score;
            highscoreTextView.setText(String.valueOf(highscore));
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("highscore", highscore);
            editor.apply();
            sendNotification("New Highscore", "Your new highscore is: " + highscore);
        }
    }

    void resetHighscore() {
        highscore = 0;
        highscoreTextView.setText(String.valueOf(highscore));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("highscore", highscore);
        editor.apply();
    }

    // Notification
    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setContentTitle("ShakeIt - " + title);
            builder.setContentText(message);
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            builder.setAutoCancel(true);
            builder.setDefaults(Notification.DEFAULT_ALL);
        notificationManager.notify(1, builder.build());
    }

    // Add values to chart
    void addValuesToDataSets(float[] values) {
        time++;

        xAxisLineData.addEntry(new Entry(time, values[0]));

        if(xAxisLineData.getEntryCount() >= CHART_ENTRIES_LIMIT) {
            xAxisLineData.removeFirst();
        }

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(xAxisLineData);
        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    // Set TextViews
    void setTextViews() {
        if(isShaking) {
            shakeTextView.setText(getString(R.string.shaking));
            shakeTextView.setTextColor(getResources().getColor(R.color.green));
        } else {
            shakeTextView.setText(getString(R.string.not_shaking));
            shakeTextView.setTextColor(getResources().getColor(R.color.red));
        }

        if (isShaking) {
            scoreTextView.setText(String.valueOf(score));
        }
    }

    float newHighX = -1;
    float newLastX = 0;
    float newDeltaX = 0;
    float newCurrentValue = 0;
    boolean moveUp = false;


    // New Shake Detection
    void newShakeDetection(float[] values) {
        newCurrentValue = values[0];
        newDeltaX = Math.abs(lastX - newCurrentValue);

        if (newCurrentValue < 0.1 && newCurrentValue > -0.1) {
        }
        else if (!moveUp && newCurrentValue > newLastX) {
//            if (!moveUp) {
                deltaX = Math.abs(newHighX - newLastX);
//                Log.i("MainActivity", "Delta: " + deltaX);
                newHighX = newLastX;
//            }
            moveUp = true;
//            Log.e("MainActivity", "Current: " + newCurrentValue + " - Last: " + newLastX);

        } else if (moveUp && newCurrentValue < newLastX) {
//            if (moveUp) {
                deltaX = Math.abs(newHighX - newLastX);
//                Log.i("MainActivity", "Delta: " + deltaX);
                newHighX = newLastX;
//            }
            moveUp = false;
//            Log.d("MainActivity", "Current: " + newCurrentValue + " - Last: " + newLastX);
        }

        if (deltaX > SHAKE_INTESITY ) {
            score += deltaX;
            isShaking = true;
        } else {
            if (isShaking) {
                setHighscore();
            }
            score = 0;
            isShaking = false;
        }

        // Log Current and Last Value
//        Log.d("MainActivity", "Current: " + newCurrentValue + " - Last: " + newLastX);
        newLastX = newCurrentValue;
    }
}
