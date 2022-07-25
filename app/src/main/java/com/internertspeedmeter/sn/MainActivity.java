package com.internertspeedmeter.sn;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.saifullah.nurani.speedtest.InternetSpeedBuilder;

import java.text.DecimalFormat;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import me.ibrahimsn.lib.Speedometer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Speedometer speedometer = findViewById(R.id.speedometer);
        Button startButton = findViewById(R.id.speedButton);
        final DecimalFormat dec = new DecimalFormat("#.##");
        TextView speedUp = findViewById(R.id.uploadSpeed);
        TextView speedDown = findViewById(R.id.downloadSpeed);
        TextView ping = findViewById(R.id.pingSpeed);

        final boolean[] isUploadCompleted = {false};
        final boolean[] isDownloadComplete = {false};

        startButton.setOnClickListener(v -> {
            startButton.setEnabled(false);
            InternetSpeedBuilder builder = new InternetSpeedBuilder(this);
            builder.setOnInternetSpeedListener(new InternetSpeedBuilder.OnInternetSpeedListener() {
                @Override
                public void onProgressDownloadSpeed(String s, int position1) {
                    if (!isUploadCompleted[0]) {
                        speedometer.setSpeed(0, 1000L, new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                return null;
                            }
                        });
                        isUploadCompleted[0] = true;
                        speedometer.setMetricText("Mbps");
                    }
                    speedDown.setText(s);
                    speedometer.setSpeed(position1, 1000L, () -> null);
                }

                @Override
                public void onProgressUploadSpeed(String s, int position) {
                    if (!isDownloadComplete[0]) {
                        speedometer.setSpeed(0, 1000L, new Function0<Unit>() {
                            @Override
                            public Unit invoke() {
                                return null;
                            }
                        });
                    }
                    speedometer.setSpeed(position, 1000L, () -> null);
                    speedUp.setText(s);
                }

                @Override
                public void onProgressPing(String s, int position) {
                    speedometer.setSpeed(position, 1000L, () -> {
                        return null;
                    });
                    ping.setText(s);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCompleted() {
                    startButton.setEnabled(true);
                    speedometer.setSpeed(0, 1000L, new Function0<Unit>() {
                        @Override
                        public Unit invoke() {
                            speedometer.setMetricText("Ms");
                            return null;
                        }
                    });
                }
            });
            builder.startTest();
        });
    }
}