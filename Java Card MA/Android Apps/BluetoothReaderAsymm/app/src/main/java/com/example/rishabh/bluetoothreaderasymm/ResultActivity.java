package com.example.rishabh.bluetoothreaderasymm;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {
    private final String TAG = "ResultActivity";

    public static final int PRINT_RESULT_SUCCESS = 0;
    public static final int PRINT_RESULT_FAILURE=1;
    public static final int FILE_TRANSFER=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Intent intent = getIntent();
        String timings = null;
        if(intent.hasExtra("TIMINGS")) {
            timings = intent.getStringExtra("TIMINGS");
        }
        int wht = getIntent().getIntExtra("WHAT", PRINT_RESULT_FAILURE);
        TextView resultText = (TextView) findViewById(R.id.resultText);
        if(wht == PRINT_RESULT_SUCCESS) {
            resultText.setText("\t\t\t\tMutual Authentication Successful\n\n");
            if(timings != null)
                resultText.append(timings + "ms");
        }
        else if(wht == PRINT_RESULT_FAILURE) {
            resultText.setText("Mutual Authentication failed\n\n");
        }
    }
}
