package com.example.unitconverter;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Spinner spinnerCategory, spinnerFrom, spinnerTo;
    EditText etValue;
    Button btnConvert;
    TextView tvResult;

    String[] categories = {"Length", "Weight", "Temperature"};

    // Each factor converts the unit to the base unit (metre / kilogram)
    String[] lengthUnits = {"Millimetre", "Centimetre", "Metre", "Kilometre", "Inch", "Foot"};
    double[] lengthFactors = {0.001, 0.01, 1, 1000, 0.0254, 0.3048};

    String[] weightUnits = {"Gram", "Kilogram", "Pound", "Ounce"};
    double[] weightFactors = {0.001, 1, 0.45359237, 0.028349523125};

    String[] temperatureUnits = {"Celsius", "Fahrenheit", "Kelvin"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        etValue = findViewById(R.id.etValue);
        btnConvert = findViewById(R.id.btnConvert);
        tvResult = findViewById(R.id.tvResult);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // When the category changes, reload the unit spinners
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadUnits(position);
                tvResult.setText("Result will appear here");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        btnConvert.setOnClickListener(v -> convert());
    }

    private String[] getUnits(int category) {
        if (category == 0) return lengthUnits;
        if (category == 1) return weightUnits;
        return temperatureUnits;
    }

    private void loadUnits(int category) {
        String[] units = getUnits(category);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        spinnerTo.setSelection(1); // "from" and "to" start different
    }

    private void convert() {
        String text = etValue.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show();
            return;
        }

        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            return;
        }

        int category = spinnerCategory.getSelectedItemPosition();
        int fromIndex = spinnerFrom.getSelectedItemPosition();
        int toIndex = spinnerTo.getSelectedItemPosition();
        String toName = getUnits(category)[toIndex];

        double result;

        if (category == 2) {
            result = convertTemperature(value, fromIndex, toIndex);
            if (Double.isNaN(result)) {
                Toast.makeText(this, "Temperature cannot be below absolute zero",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            double[] factors = (category == 0) ? lengthFactors : weightFactors;
            double inBase = value * factors[fromIndex];
            result = inBase / factors[toIndex];
        }

        tvResult.setText(String.format(Locale.getDefault(), "%.4f %s", result, toName));
    }

    // Index: 0 = Celsius, 1 = Fahrenheit, 2 = Kelvin. Returns NaN below absolute zero.
    private double convertTemperature(double value, int from, int to) {
        double celsius;
        if (from == 0) celsius = value;
        else if (from == 1) celsius = (value - 32) * 5 / 9;
        else celsius = value - 273.15;

        if (celsius < -273.15) return Double.NaN;

        if (to == 0) return celsius;
        if (to == 1) return celsius * 9 / 5 + 32;
        return celsius + 273.15;
    }
}