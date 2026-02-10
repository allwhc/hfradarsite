package com.example.hfradar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AttendanceFragment extends Fragment {

    private static final int LOCATION_PERMISSION_CODE = 200;
    private static final String MyPREFERENCES = "MyPrefs1";
    private static final String PREF_SITES_LIST = "sites_list";

    private EditText edtName;
    private Spinner spnSite;
    private Button btnMark;
    private LinearLayout layoutResult;
    private TextView tvStatus, tvDetails, tvGpsStatus;
    private ProgressBar progressBar;

    private LocationManager locationManager;
    private double currentLat = 0, currentLng = 0;
    private boolean gpsAcquired = false;

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_attendance, container, false);

        edtName = view.findViewById(R.id.edtAttendanceName);
        spnSite = view.findViewById(R.id.spnAttendanceSite);
        btnMark = view.findViewById(R.id.btnMarkAttendance);
        layoutResult = view.findViewById(R.id.layoutAttendanceResult);
        tvStatus = view.findViewById(R.id.tvAttendanceStatus);
        tvDetails = view.findViewById(R.id.tvAttendanceDetails);
        tvGpsStatus = view.findViewById(R.id.tvGpsStatus);
        progressBar = view.findViewById(R.id.progressAttendance);

        // Load saved name from preferences
        SharedPreferences prefs = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String savedName = prefs.getString("attendance_name", "");
        if (!savedName.isEmpty()) {
            edtName.setText(savedName);
        }

        // Load sites into spinner
        loadSitesList();

        // Set last selected site
        String lastSite = prefs.getString("attendance_last_site", "");
        if (!lastSite.isEmpty()) {
            setSpinnerSelection(lastSite);
        }

        // Mark attendance button
        btnMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markAttendance();
            }
        });

        return view;
    }

    private void loadSitesList() {
        SharedPreferences prefs = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        String sitesJson = prefs.getString(PREF_SITES_LIST, "");

        ArrayList<String> sitesList = new ArrayList<>();
        sitesList.add("-- Select Site --");

        if (!sitesJson.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(sitesJson);
                for (int i = 0; i < arr.length(); i++) {
                    sitesList.add(arr.getString(i));
                }
            } catch (Exception e) {
                // Fallback to default sites
                String[] defaults = {"Cuda", "Kalp", "Mach", "Yanm", "Wasi", "Jgri", "Gopa", "Puri", "Ptbl", "Htby"};
                for (String s : defaults) sitesList.add(s);
            }
        } else {
            // Default sites
            String[] defaults = {"Cuda", "Kalp", "Mach", "Yanm", "Wasi", "Jgri", "Gopa", "Puri", "Ptbl", "Htby"};
            for (String s : defaults) sitesList.add(s);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, sitesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnSite.setAdapter(adapter);
    }

    private void setSpinnerSelection(String value) {
        ArrayAdapter adapter = (ArrayAdapter) spnSite.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(value)) {
                    spnSite.setSelection(i);
                    break;
                }
            }
        }
    }

    private void markAttendance() {
        String name = edtName.getText().toString().trim();
        String site = spnSite.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (site.equals("-- Select Site --")) {
            Toast.makeText(getActivity(), "Please select a site", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save name and last site to preferences
        SharedPreferences prefs = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("attendance_name", name);
        editor.putString("attendance_last_site", site);
        editor.apply();

        // Check and request location permission
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }

        // Check if GPS is enabled
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            Toast.makeText(getActivity(), "Please enable GPS/Location services", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnMark.setEnabled(false);
        tvGpsStatus.setText("Submitting attendance...");
        layoutResult.setVisibility(View.GONE);

        // Get location
        getLocationAndSubmit(name, site);
    }

    private void getLocationAndSubmit(final String name, final String site) {
        try {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                progressBar.setVisibility(View.GONE);
                btnMark.setEnabled(true);
                return;
            }

            // Try to get last known location first
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnown != null) {
                // Use last known location if recent (within 2 minutes)
                long age = System.currentTimeMillis() - lastKnown.getTime();
                if (age < 120000) {
                    currentLat = lastKnown.getLatitude();
                    currentLng = lastKnown.getLongitude();
                    submitAttendance(name, site, currentLat, currentLng);
                    return;
                }
            }

            // Request fresh location update
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    gpsAcquired = true;
                    locationManager.removeUpdates(this);
                    submitAttendance(name, site, currentLat, currentLng);
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}
            };

            // Request from both providers for fastest result
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }

            // Timeout after 15 seconds - use whatever we have
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!gpsAcquired && getActivity() != null) {
                        locationManager.removeUpdates(locationListener);
                        // Try last known as fallback
                        Location fallback = null;
                        try {
                            fallback = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (fallback == null) {
                                fallback = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            }
                        } catch (SecurityException e) { /* ignored */ }

                        if (fallback != null) {
                            submitAttendance(name, site, fallback.getLatitude(), fallback.getLongitude());
                        } else {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.GONE);
                                        btnMark.setEnabled(true);
                                        tvGpsStatus.setText("Could not mark attendance. Please try again in an open area.");
                                        Toast.makeText(getActivity(), "Unable to mark attendance. Please try again.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }
                }
            }, 15000);

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnMark.setEnabled(true);
            Toast.makeText(getActivity(), "Error marking attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void submitAttendance(String name, String site, double lat, double lng) {
        new SubmitAttendanceTask(name, site, lat, lng).execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(), "Location permission granted. Tap 'Mark My Attendance' again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Location permission is required for attendance.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // AsyncTask to submit attendance to server
    private class SubmitAttendanceTask extends AsyncTask<Void, Void, String> {
        private String name, site;
        private double lat, lng;

        SubmitAttendanceTask(String name, String site, double lat, double lng) {
            this.name = name;
            this.site = site;
            this.lat = lat;
            this.lng = lng;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                SharedPreferences prefs = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                String baseUrl = prefs.getString("serv_url", "https://hfradarsite.pythonanywhere.com");

                URL url = new URL(baseUrl + "/markAttendance");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject payload = new JSONObject();
                payload.put("staff_name", name);
                payload.put("site_code", site);
                payload.put("latitude", lat);
                payload.put("longitude", lng);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                conn.disconnect();

                return sb.toString();
            } catch (Exception e) {
                return "{\"stat\":\"error\",\"msg\":\"Network error: " + e.getMessage() + "\"}";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (getActivity() == null) return;

            progressBar.setVisibility(View.GONE);
            btnMark.setEnabled(true);
            gpsAcquired = false;

            try {
                JSONObject resp = new JSONObject(result);
                String stat = resp.optString("stat", "error");

                if (stat.equals("success")) {
                    layoutResult.setVisibility(View.VISIBLE);
                    layoutResult.setBackgroundColor(0xFFF0F9F0);
                    tvStatus.setTextColor(0xFF28A745);
                    tvStatus.setText("Attendance Marked Successfully!");

                    String details = "Time: " + resp.optString("timestamp", "");
                    tvDetails.setText(details);

                    tvGpsStatus.setText("");
                } else {
                    layoutResult.setVisibility(View.VISIBLE);
                    layoutResult.setBackgroundColor(0xFFFFF3CD);
                    tvStatus.setTextColor(0xFFDC3545);
                    tvStatus.setText("Could Not Mark Attendance");
                    tvDetails.setText(resp.optString("msg", "Unknown error"));
                    tvGpsStatus.setText("Please try again");
                }
            } catch (Exception e) {
                layoutResult.setVisibility(View.VISIBLE);
                layoutResult.setBackgroundColor(0xFFFFF3CD);
                tvStatus.setTextColor(0xFFDC3545);
                tvStatus.setText("Error");
                tvDetails.setText("Failed to parse server response");
                tvGpsStatus.setText("Please check your internet connection");
            }
        }
    }
}
