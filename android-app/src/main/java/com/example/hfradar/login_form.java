package com.example.hfradar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class login_form extends AppCompatActivity {
    EditText txtPassword;
    // login button
    Button btnLogin;
    String password="";
    public static final String MyPREFERENCES = "MyPrefs1";
    SharedPreferences sharedpreferences;
    String pwd = "test";
    String apipwd = "";
    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // Notification permission request code
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_form);
        SharedPreferences settings = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        apipwd = settings.getString("app_pwd", pwd);

        // Request notification permission on first launch (Android 13+)
        requestNotificationPermissionIfNeeded();

        txtPassword = (EditText) findViewById(R.id.txtPassword);

        txtPassword.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Validation.hasText(txtPassword);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        // Login button
        btnLogin = (Button) findViewById(R.id.btnLogin);

        // Login button click event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Get username, password from EditText
                if ( checkValidation ())
                {
                    txtPassword = (EditText) findViewById(R.id.txtPassword);
                    password = txtPassword.getText().toString();
                    if(password.equals(apipwd))
                    {
                        Intent i = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(i);
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Entered password is incorrect!",
                                Toast.LENGTH_LONG).show();
                    }
                }

            }
        });


    }

    @Override
    public void onBackPressed() {
        //do nothing
    }

    @Override
    public void onStop() {
        // call the superclass method first
        super.onStop();

        SharedPreferences settings = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("app_sel_menu", "04" );
        editor.commit();
    }

    private boolean checkValidation() {
        boolean ret = true;

        if (!Validation.hasText(txtPassword))
        {
            Toast.makeText(getApplicationContext(), "Please enter password!",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return ret;
    }

    /**
     * Request notification permission for Android 13+ (API 33+)
     * Only asks once on first app launch
     */
    private void requestNotificationPermissionIfNeeded() {
        // Only request for Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SharedPreferences settings = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
            boolean hasAskedBefore = settings.getBoolean("notification_permission_asked", false);

            // Only ask if we haven't asked before
            if (!hasAskedBefore) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);

                    // Mark that we've asked (even if denied, don't ask again)
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("notification_permission_asked", true);
                    editor.apply();
                }
            }
        }
    }

    /**
     * Handle the result of notification permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted. You'll receive monthly reminders.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You can enable it in Settings.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}