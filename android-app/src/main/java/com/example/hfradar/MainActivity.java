package com.example.hfradar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    DrawerLayout dLayout;
    public static final String MyPREFERENCES1 = "ScanPrefs";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar); // get the reference of Toolbar
        setSupportActionBar(toolbar); // Setting/replace toolbar as the ActionBar
        // implement setNavigationOnClickListener event
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dLayout.openDrawer(Gravity.LEFT);
            }
        });
        setNavigationDrawer(); // call method

        // Schedule monthly reminder notifications
        scheduleMonthlyReminder();

        // Auto-fetch technicians map if not present
        autoFetchConfig();
    }

    private void scheduleMonthlyReminder() {
        // Create notification channel (required for Android 8+)
        NotificationHelper.createNotificationChannel(this);

        // Schedule the monthly reminder worker if enabled
        if (MonthlyReminderWorker.isEnabled(this)) {
            MonthlyReminderWorker.schedule(this);
        }
    }

    @Override
    public void onBackPressed() {

        if (getFragmentManager().getBackStackEntryCount() == 1) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }


    private void setNavigationDrawer() {
        dLayout = (DrawerLayout) findViewById(R.id.drawer_layout); // initiate a DrawerLayout
        NavigationView navView = (NavigationView) findViewById(R.id.navigation); // initiate a Navigation View
        // implement setNavigationItemSelectedListener event on NavigationView
        SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES1, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("scan_data1", "0");
        editor.commit();

        // Set Site Report (HomeInventory) as default screen
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, new HomeInventory()).commit();
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                Fragment frag = null;
                int itemId = menuItem.getItemId(); // get selected menu item's id
                // check selected menu item's id and replace a Fragment Accordingly
                if (itemId == R.id.first) {
                    // Site Report (formerly Add Data)
                    SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES1, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putString("scan_data1", "0");
                    editor.commit();
                    frag = new HomeInventory();
                }
                else if (itemId == R.id.third) {
                    frag = new HistoryFragment();
                }
                else if (itemId == R.id.fifth) {
                    frag = new AssignCredentials();
                }
                else if (itemId == R.id.sixth) {
                    frag = new VerifyDataFragment();
                }
                else if (itemId == R.id.seventh) {
                    frag = new AttendanceFragment();
                }
                else if (itemId == R.id.fourth) {
                    Intent i = new Intent(getApplicationContext(), login_form.class);
                    startActivity(i);
                }
                    // display a toast message with menu item's title
                    Toast.makeText(getApplicationContext(), menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                    if (frag != null) {
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.frame, frag); // replace a Fragment with Frame Layout
                        transaction.commit(); // commit the changes
                        dLayout.closeDrawers(); // close the all open Drawer Views
                        return true;
                    }
                    return false;
                }
            });
        }

    private void autoFetchConfig() {
        SharedPreferences prefs = getSharedPreferences("MyPrefs1", Context.MODE_PRIVATE);
        String techMap = prefs.getString("technicians_map", "");
        if (!techMap.isEmpty()) return; // already have it

        String baseUrl = prefs.getString("serv_url", "https://hfradarsite.pythonanywhere.com");
        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... params) {
                try {
                    URL url = new URL(params[0] + "/getConfig");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    OutputStream os = conn.getOutputStream();
                    os.write("{}".getBytes("UTF-8"));
                    os.close();

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    conn.disconnect();

                    JSONObject resp = new JSONObject(sb.toString());
                    SharedPreferences.Editor editor = prefs.edit();
                    if (resp.has("sites")) {
                        JSONArray sites = resp.getJSONArray("sites");
                        StringBuilder csv = new StringBuilder();
                        for (int i = 0; i < sites.length(); i++) {
                            if (i > 0) csv.append(",");
                            csv.append(sites.getString(i));
                        }
                        editor.putString("sites_list", csv.toString());
                    }
                    if (resp.has("technicians")) {
                        editor.putString("technicians_map", resp.getJSONObject("technicians").toString());
                    }
                    editor.apply();
                } catch (Exception e) {
                    // silent fail
                }
                return null;
            }
        }.execute(baseUrl);
    }
    }