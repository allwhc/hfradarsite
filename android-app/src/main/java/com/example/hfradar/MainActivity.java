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
    }