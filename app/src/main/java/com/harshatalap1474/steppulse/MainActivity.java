package com.harshatalap1474.steppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import android.content.SharedPreferences;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // UI
    private TextView tvSteps, tvDistance, tvCalories;
    private LineChart activityChart;
    private Button btnShare, btnOpenBMI;

    // Sensor
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;
    private boolean isSensorRegistered = false;

    // Logic
    private int stepCount = 0;
    private int initialSteps = -1;
    private int lastPlottedSteps = 0;

    // Constants
    private static final double STRIDE_LENGTH_METERS = 0.762;
    private static final double CALORIES_PER_STEP = 0.04;

    // Chart
    private LineDataSet dataSet;
    private LineData lineData;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "step_prefs";
    private static final String KEY_INITIAL_STEPS = "initial_steps";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        initialSteps = prefs.getInt(KEY_INITIAL_STEPS, -1);

        initViews();
        checkPermission();
        initSensor();
        setupChart();
        setupButtons();
    }

    private void initViews() {
        tvSteps = findViewById(R.id.tv_steps);
        tvDistance = findViewById(R.id.tv_distance);
        tvCalories = findViewById(R.id.tv_calories);
        activityChart = findViewById(R.id.activityChart);
        btnShare = findViewById(R.id.btn_share);
        btnOpenBMI = findViewById(R.id.btn_open_bmi);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                    101
            );
        }
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = stepSensor != null;
        }

        if (!isSensorPresent) {
            tvSteps.setText("N/A");
            Toast.makeText(this, "Step sensor not available on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void setupChart() {
        activityChart.animateX(800);
        activityChart.getAxisRight().setEnabled(false);
        activityChart.getXAxis().setDrawGridLines(false);
        activityChart.getAxisLeft().setDrawGridLines(true);
        Description description = new Description();
        description.setText("Steps (Session)");
        activityChart.setDescription(description);
        activityChart.setNoDataText("Start walking to see data");

        dataSet = new LineDataSet(new ArrayList<>(), "Steps");
        dataSet.setDrawCircles(false);
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);

        lineData = new LineData(dataSet);
        activityChart.setData(lineData);
    }

    private void setupButtons() {

        btnShare.setOnClickListener(v -> {
            String msg = "I just walked " + stepCount + " steps ("
                    + String.format("%.2f", (stepCount * STRIDE_LENGTH_METERS) / 1000)
                    + " km) using StepPulse!";

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share via"));
        });

        btnOpenBMI.setOnClickListener(v -> {
            startActivity(new Intent(this, BMIActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.edit().putInt(KEY_INITIAL_STEPS, initialSteps).apply();
        unregisterSensor();
    }

    private void registerSensor() {
        if (isSensorPresent && !isSensorRegistered) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isSensorRegistered = true;
        }
    }

    private void unregisterSensor() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this);
            isSensorRegistered = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        int total = (int) event.values[0];

        if (initialSteps == -1) {
            initialSteps = total;
            prefs.edit().putInt(KEY_INITIAL_STEPS, initialSteps).apply();
            return;
        }

        stepCount = total - initialSteps;

        updateDashboard();

        if (stepCount - lastPlottedSteps >= 10) {
            lastPlottedSteps = stepCount;
            updateChart();
        }
    }

    private void updateDashboard() {
        tvSteps.setText(String.valueOf(stepCount));

        double km = (stepCount * STRIDE_LENGTH_METERS) / 1000;
        tvDistance.setText(String.format("%.2f", km));

        int cal = (int) (stepCount * CALORIES_PER_STEP);
        tvCalories.setText(String.valueOf(cal));
    }

    private void updateChart() {
        dataSet.addEntry(new Entry(dataSet.getEntryCount(), stepCount));
        activityChart.animateX(300);
        if (dataSet.getEntryCount() > 50) {
            dataSet.removeFirst();
            for (int i = 0; i < dataSet.getEntryCount(); i++) {
                dataSet.getEntryForIndex(i).setX(i);
            }
        }

        lineData.notifyDataChanged();
        activityChart.notifyDataSetChanged();
        activityChart.invalidate();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            registerSensor();

        } else {
            Toast.makeText(this, "Activity recognition permission required", Toast.LENGTH_LONG).show();
        }
    }
}
