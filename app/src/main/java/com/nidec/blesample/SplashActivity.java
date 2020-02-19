package com.nidec.blesample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {

    // =======================ADD THESE TWO VARIABLES TO SPLASH SCREEN========================
    Handler mHandler;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    // ======================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        // ==============DO THIS ON THE SPLASH SCREEN, AFTER ANIMATIONS=======================
        mHandler = new Handler();
        // Do a runtime request for coarse location
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app uses BLE which requires location permissions");
            //builder.setMessage("Sample message here");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        // Start main activity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        // ==============DO THIS ON THE SPLASH SCREEN, AFTER ANIMATIONS=======================

    }

    // =============NEED THIS CODE TO START MAIN ACTIVITY AFTER RECEIVING PERMISSIONS===============
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Start main activity
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
    // =============NEED THIS CODE TO START MAIN ACTIVITY AFTER RECEIVING PERMISSIONS===============
}
