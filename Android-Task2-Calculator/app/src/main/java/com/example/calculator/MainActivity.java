package com.example.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView tvDisplay;
    private final StringBuilder expression = new StringBuilder();
    private boolean showingResult = false; // true right after "="
    private boolean hasError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDisplay = findViewById(R.id.tvDisplay);

        int[] buttonIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
                R.id.btnEquals, R.id.btnClear, R.id.btnBack
        };

        View.OnClickListener listener = v -> onButtonClick(((Button) v).getText().toString());
        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(listener);
        }

        updateDisplay();
    }

    private void onButtonClick(String label) {
        switch (label) {
            case "C":
                clearAll();
                break;
            case "DEL":
                backspace();
                break;
            case "=":
                calculate();
                break;
            case "+":
            case "-":
            case "×":
            case "÷":
                appendOperator(label.charAt(0));
                break;
            case ".":
                appendDecimal();
                break;
            default:
                appendDigit(label);
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (hasError) {
            tvDisplay.setText("Error");
        } else if (expression.length() == 0) {
            tvDisplay.setText("0");
        } else {
            tvDisplay.setText(expression.toString());
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷';
    }

    // The number currently being typed (text after the last operator)
    private String currentNumber() {
        int i = expression.length() - 1;
        while (i >= 0 && !isOperator(expression.charAt(i))) {
            i--;
        }
        return expression.substring(i + 1);
    }

    private void resetIfNeeded() {
        if (hasError || showingResult) {
            expression.setLength(0);
            hasError = false;
            showingResult = false;
        }
    }

    private void appendDigit(String digit) {
        resetIfNeeded();
        if (expression.length() >= 30) return; // keep the display readable

        // Avoid leading zeros like "007"
        if (currentNumber().equals("0")) {
            expression.deleteCharAt(expression.length() - 1);
        }
        expression.append(digit);
    }

    private void appendDecimal() {
        resetIfNeeded();
        if (expression.length() >= 30) return;

        String current = currentNumber();
        if (current.contains(".")) return; // only one dot per number
        if (current.isEmpty() || current.equals("-")) {
            expression.append('0');
        }
        expression.append('.');
    }

    private void appendOperator(char op) {
        if (hasError) return;
        showingResult = false; // keep going with the result

        if (expression.length() == 0) {
            if (op == '-') expression.append('-'); // allow a negative first number
            return;
        }

        int last = expression.length() - 1;
        if (isOperator(expression.charAt(last))) {
            if (expression.length() == 1) return; // lone "-"
            expression.setCharAt(last, op);        // replace the previous operator
        } else if (expression.length() < 30) {
            expression.append(op);
        }
    }

    private void backspace() {
        if (hasError) {
            clearAll();
            return;
        }
        showingResult = false;
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
        }
    }

    private void clearAll() {
        expression.setLength(0);
        hasError = false;
        showingResult = false;
    }

    private void calculate() {
        if (hasError || showingResult || expression.length() == 0) return;

        // Drop a trailing operator, e.g. "5+" becomes "5"
        while (expression.length() > 0 && isOperator(expression.charAt(expression.length() - 1))) {
            expression.deleteCharAt(expression.length() - 1);
        }
        if (expression.length() == 0) return;

        try {
            double result = evaluate(expression.toString());
            expression.setLength(0);
            expression.append(format(result));
            showingResult = true;
        } catch (ArithmeticException | NumberFormatException e) {
            expression.setLength(0);
            hasError = true; // e.g. division by zero
        }
    }

    // Evaluates the expression with the correct order: × and ÷ first, then + and -
    private double evaluate(String exp) {
        ArrayList<Double> numbers = new ArrayList<>();
        ArrayList<Character> ops = new ArrayList<>();
        StringBuilder num = new StringBuilder();

        for (int i = 0; i < exp.length(); i++) {
            char c = exp.charAt(i);
            if (isOperator(c) && !(c == '-' && i == 0)) { // a leading "-" is a sign
                numbers.add(Double.parseDouble(num.toString()));
                ops.add(c);
                num.setLength(0);
            } else {
                num.append(c);
            }
        }
        numbers.add(Double.parseDouble(num.toString()));

        // Pass 1: multiplication and division
        int i = 0;
        while (i < ops.size()) {
            char op = ops.get(i);
            if (op == '×' || op == '÷') {
                double a = numbers.get(i);
                double b = numbers.get(i + 1);
                if (op == '÷' && b == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                numbers.set(i, op == '×' ? a * b : a / b);
                numbers.remove(i + 1);
                ops.remove(i);
            } else {
                i++;
            }
        }

        // Pass 2: addition and subtraction
        double result = numbers.get(0);
        for (int k = 0; k < ops.size(); k++) {
            double b = numbers.get(k + 1);
            result = (ops.get(k) == '+') ? result + b : result - b;
        }
        return result;
    }

    // Nice display: 11.0 becomes "11", 0.30000000000000004 becomes "0.3"
    private String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || Math.abs(value) >= 1e15) {
            throw new ArithmeticException("Out of range");
        }
        BigDecimal bd = new BigDecimal(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }
}