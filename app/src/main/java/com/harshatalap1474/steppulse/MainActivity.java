package com.harshatalap1474.steppulse;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // --- UI Components ---
    private TextView tvSteps, tvDistance, tvCalories, tvActiveTime, tvPace, tvDailyGoal;
    private CircularProgressIndicator progressGoal;
    private BarChart activityChart;
    private MaterialButton btnShare, btnOpenBMI;
    private Chip chipWeek, chipMonth;

    // --- Sensor & Logic ---
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isSensorPresent = false;

    // --- Data Variables ---
    private int stepCount = 0;
    private int initialSteps = -1; // To offset the "since boot" count
    private static final int DAILY_GOAL = 10000;

    // --- Math Constants ---
    private static final double STRIDE_LENGTH_METERS = 0.762;
    private static final double CALORIES_PER_STEP = 0.04;
    private static final int STEPS_PER_MINUTE_AVG = 100; // Approx cadence for walking

    // --- Storage ---
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "step_prefs";
    private static final String KEY_INITIAL_STEPS = "initial_steps";

    // --- Chart Data ---
    private ArrayList<BarEntry> chartEntries = new ArrayList<>();
    private BarDataSet barDataSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Storage
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // We do not load initialSteps here, we wait for the first sensor event

        // 2. Bind Views
        initViews();

        // 3. Setup Chart Styling
        setupChart();

        // 4. Check Permissions & Init Sensor
        checkPermission();
        initSensor();

        // 5. Button Listeners
        setupClickListeners();

        // 6. Set Goal UI
        tvDailyGoal.setText(String.format(Locale.getDefault(), "%d steps", DAILY_GOAL));
        progressGoal.setMax(DAILY_GOAL);
    }

    private void initViews() {
        tvSteps = findViewById(R.id.tv_steps);
        tvDistance = findViewById(R.id.tv_distance);
        tvCalories = findViewById(R.id.tv_calories);
        tvActiveTime = findViewById(R.id.tv_active_time);
        tvPace = findViewById(R.id.tv_pace);

        // Goal Section
        tvDailyGoal = findViewById(R.id.tv_daily_goal);
        progressGoal = findViewById(R.id.progress_goal);

        // Buttons & Chart
        activityChart = findViewById(R.id.activityChart);
        btnShare = findViewById(R.id.btn_share);
        btnOpenBMI = findViewById(R.id.btn_open_bmi);

        // Chips
        chipWeek = findViewById(R.id.chip_week);
        chipMonth = findViewById(R.id.chip_month);
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = stepSensor != null;
        }

        if (!isSensorPresent) {
            tvSteps.setText("N/A");
            Toast.makeText(this, "Step sensor not found on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void setupClickListeners() {
        btnShare.setOnClickListener(v -> {
            String msg = "🔥 StepPulse Update:\nI walked " + stepCount + " steps today!\n"
                    + "Distance: " + tvDistance.getText() + " km\n"
                    + "Calories: " + tvCalories.getText() + " kcal";

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Share Progress"));
        });

        btnOpenBMI.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BMIActivity.class);
            startActivity(intent);
        });

        // Placeholder logic for chips (since we don't have a DB yet)
        chipWeek.setOnClickListener(v -> Toast.makeText(this, "Weekly view active", Toast.LENGTH_SHORT).show());
        chipMonth.setOnClickListener(v -> Toast.makeText(this, "Monthly history coming soon!", Toast.LENGTH_SHORT).show());
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
            }
        }
    }

    // --- SENSOR LOGIC ---

    @Override
    protected void onResume() {
        super.onResume();
        if (isSensorPresent) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSensorPresent) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalStepsSinceReboot = (int) event.values[0];

            // Handle the offset (so steps start at 0 when app is installed/reset)
            if (initialSteps == -1) {
                // Try to retrieve from prefs, otherwise set current as initial
                initialSteps = prefs.getInt(KEY_INITIAL_STEPS, -1);
                if (initialSteps == -1) {
                    initialSteps = totalStepsSinceReboot;
                    prefs.edit().putInt(KEY_INITIAL_STEPS, initialSteps).apply();
                }
            }

            stepCount = totalStepsSinceReboot - initialSteps;
            if(stepCount < 0) stepCount = 0; // Safety check if device rebooted

            updateDashboard();
            updateChartData();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    // --- UI UPDATES ---

    private void updateDashboard() {
        // 1. Steps
        tvSteps.setText(String.valueOf(stepCount));

        // 2. Progress Bar
        progressGoal.setProgress(stepCount, true); // true = animate

        // 3. Distance (km)
        double distanceKm = (stepCount * STRIDE_LENGTH_METERS) / 1000;
        tvDistance.setText(String.format(Locale.US, "%.2f", distanceKm));

        // 4. Calories (kcal)
        int calories = (int) (stepCount * CALORIES_PER_STEP);
        tvCalories.setText(String.valueOf(calories));

        // 5. Active Time (Estimated: Steps / 100 steps per min)
        int totalMinutes = stepCount / STEPS_PER_MINUTE_AVG;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        tvActiveTime.setText(String.format(Locale.US, "%dh %dm", hours, minutes));

        // 6. Pace (Minutes per km)
        if (distanceKm > 0.1) {
            double pace = totalMinutes / distanceKm;
            tvPace.setText(String.format(Locale.US, "%.0f' /km", pace));
        } else {
            tvPace.setText("-- /km");
        }
    }

    // --- CHART LOGIC ---

    private void setupChart() {
        // Remove description and grid lines for a clean look
        activityChart.getDescription().setEnabled(false);
        activityChart.getLegend().setEnabled(false);
        activityChart.setDrawGridBackground(false);
        activityChart.setDrawBarShadow(false);
        activityChart.setDrawBorders(false);

        // X-Axis (Bottom)
        XAxis xAxis = activityChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#94A3B8"));
        xAxis.setGranularity(1f);

        // Y-Axis (Left)
        YAxis leftAxis = activityChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F1F5F9")); // Very light grey
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#94A3B8"));
        leftAxis.setAxisMinimum(0f); // Start at 0

        // Y-Axis (Right) - Hide it
        activityChart.getAxisRight().setEnabled(false);

        // Interaction
        activityChart.setTouchEnabled(false); // Disable zoom/touch for dashboard feel
    }

    private void updateChartData() {
        // In a real app, this would pull historical data from a Room Database.
        // For this demo, we will visualize the current session growing.

        // Add current step count as a new bar (simulating "Now")
        // To prevent the chart from getting too crowded, we only keep the last 7 updates
        if (chartEntries.size() >= 7) {
            chartEntries.remove(0);
        }

        // The X-value is simply the size (0, 1, 2...)
        chartEntries.add(new BarEntry(chartEntries.size(), stepCount));

        // Re-index X values so they are always 0 to N
        for(int i=0; i<chartEntries.size(); i++){
            chartEntries.get(i).setX(i);
        }

        barDataSet = new BarDataSet(chartEntries, "Activity");
        barDataSet.setColor(Color.parseColor("#4F46E5")); // Indigo color
        barDataSet.setDrawValues(false); // Hide numbers on top of bars
        barDataSet.setHighLightAlpha(0); // Remove highlight on tap

        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.5f); // Slim bars

        activityChart.setData(data);
        activityChart.invalidate(); // Refresh
    }

    // Permission Result Handler
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initSensor();
            } else {
                Toast.makeText(this, "Permission Required!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}