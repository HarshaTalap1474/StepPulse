package com.harshatalap1474.steppulse;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BMIActivity extends AppCompatActivity {

    private EditText etWeight, etHeight;
    private TextView tvResult;
    private Button btnCalculate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

        initViews();
        setupListener();
    }

    private void initViews() {
        etWeight = findViewById(R.id.et_weight);
        etHeight = findViewById(R.id.et_height);
        tvResult = findViewById(R.id.tv_bmi_result);
        btnCalculate = findViewById(R.id.btn_calculate);
    }

    private void setupListener() {
        btnCalculate.setOnClickListener(v -> calculateBMI());
    }

    private void calculateBMI() {
        String wStr = etWeight.getText().toString().trim();
        String hStr = etHeight.getText().toString().trim();

        if (TextUtils.isEmpty(wStr) || TextUtils.isEmpty(hStr)) {
            showError("Enter both weight and height");
            return;
        }

        float weight, height, hM;

        try {
            weight = Float.parseFloat(wStr);
            height = Float.parseFloat(hStr);
            hM = height/100f;
        } catch (NumberFormatException e) {
            showError("Invalid number format");
            return;
        }

        if (weight <= 0 || hM <= 0) {
            showError("Values must be greater than zero");
            return;
        }

        float bmi = weight / (hM * hM);
        String category = getBMICategory(bmi);

        tvResult.setText(String.format(Locale.US, "%.1f (%s)", bmi, category));
    }

    private String getBMICategory(float bmi) {
        if (bmi < 18.5f) return "Underweight";
        if (bmi < 25f) return "Normal";
        if (bmi < 30f) return "Overweight";
        return "Obese";
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
