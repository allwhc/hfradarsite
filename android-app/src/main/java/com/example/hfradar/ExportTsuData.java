package com.example.hfradar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import static android.content.Context.MODE_PRIVATE;

public class ExportTsuData extends Fragment {
	Button btnGo, btnList,btnId1, btnId2,btnLc1, btnLc2,btnRc1, btnRc2, btnScan;
	TextView textId,textMon,textYr,textMsg,textP;
    TableLayout stk;
    TableRow trow1;
    LinearLayout linear_layout_10;
    View view;
    boolean bGo=false,bData=false,bvalscan=false;
    AlertDialogManager alert = new AlertDialogManager();
    SQLiteDatabase mDatabase;
    public static final String DATABASE_NAME = "HFDB1";
    public static final String MyPREFERENCES1 = "ScanPrefs";
    public static final String MyPREFERENCES = "MyPrefs1";
    String id1 ="",cmon="",sid="";
    int imon=0,iselid=0,iselmon=0,iselyr=0,crmon1=0,iv1=0;
    private final String USER_AGENT = "Mozilla/5.0";
    String[] spinnerArray1 = { "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec" };
    String[] lsthfids = { "Cuda","Kalp","Mach","Yanm","Wasi","Jgri","Gopa","Puri","Ptbl","Htby" };
    String[] lstyrs = { "2015","2016","2017","2018","2019","2020","2021","2022","2023","2024","2025" };
    String[] lstdata = { };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the srch_prod for this fragment
        view = inflater.inflate(R.layout.export_tsu, container, false);
        mDatabase = getActivity().openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null);
        mDatabase.execSQL("create table if not exists TsuInfo (id integer primary key, tsuid text,tsudate text,radialcnt text,radialcnthex text);");
        stk = view.findViewById(R.id.table_main);
        //String smenu = settings.getString("app_id", "Cuda");

        String smenu = "Cuda";
        SharedPreferences settings1 = getActivity().getSharedPreferences(MyPREFERENCES1, Context.MODE_PRIVATE);
        String scandata = settings1.getString("scan_data", "Scan_Result");
        String scandata1 = settings1.getString("scan_data1", "0");
        //scandata = "Htby:11:2020:23:23:24:23:23:24:23:23:24:23:23:24:23:23:24:23:23:24:23:23:24:23:23:24:23:23:24:23:23:24:700";
        //scandata = "GOPA:9:2021:24:24:24:24:24:24:24:22:24:24:24:24:24:24:15:0:0:0:0:0:0:0:0:0:0:7:9:9:24:24:422";
        lstdata = scandata.split(":");
        Calendar now = Calendar.getInstance();
        int crmon = (now.get(Calendar.MONTH))+1;
        int cryr = now.get(Calendar.YEAR);
        if(crmon == 1) {
            crmon1 = 12;
            cryr = cryr - 1;
        }
        else {
            crmon1 = crmon;
        }
        if(scandata1.equals("1")) {
            String sdata = lstdata[0];
            String sdata1 = lstdata[1];
            String sdata2 = lstdata[2];
            for (int i = 0; i < 10; i++) {
                if (lsthfids[i].toUpperCase().equals(sdata.toUpperCase())) {
                    bvalscan = true;
                    smenu = sdata;
                }
            }
        }

        textId = (TextView) view.findViewById(R.id.textId);
        trow1 = (TableRow) view.findViewById(R.id.trow1);
        textMon = (TextView) view.findViewById(R.id.textMon);
        textMsg = (TextView) view.findViewById(R.id.textMsg);
        textP = (TextView) view.findViewById(R.id.textP);
        linear_layout_10 = (LinearLayout) view.findViewById(R.id.linear_layout_10);
        linear_layout_10.setVisibility(View.GONE);
        textMsg.setVisibility(View.GONE);
        trow1.setVisibility(View.GONE);
        //textMon.setText(spinnerArray1[(crmon1-2)]);
        iselid=getlstIdx(lsthfids, smenu);;iselmon=crmon;iselyr=getlstIdx(lstyrs, (""+cryr));
        textId.setText(lsthfids[iselid]);
        textYr = (TextView) view.findViewById(R.id.textYr);
        textYr.setText(lstyrs[iselyr]);
        btnId1 = (Button) view.findViewById(R.id.btnId1);
        btnId2 = (Button) view.findViewById(R.id.btnId2);
        btnLc1 = (Button) view.findViewById(R.id.btnLc1);
        btnLc2 = (Button) view.findViewById(R.id.btnLc2);
        btnRc1 = (Button) view.findViewById(R.id.btnRc1);
        btnRc2 = (Button) view.findViewById(R.id.btnRc2);
        btnScan = (Button) view.findViewById(R.id.btnScan);
        btnGo = (Button) view.findViewById(R.id.btnGo);
        //btnLc1.setEnabled(false);btnLc2.setEnabled(true);
        //btnLc1.setBackgroundColor(Color.LTGRAY);btnLc2.setBackgroundColor(Color.rgb(214, 118, 1));
        if(iv1==0) enadisbtn(btnLc1,btnLc2,textMon,(crmon1-2), spinnerArray1, (spinnerArray1.length), 0);
        btnRc1.setEnabled(false);btnRc2.setEnabled(false);
        btnRc1.setBackgroundColor(Color.LTGRAY);btnRc2.setBackgroundColor(Color.LTGRAY);
        if(iselid==0) {
            btnId1.setEnabled(false);
            btnId1.setBackgroundColor(Color.LTGRAY);
        }
        if(iselid==9) {
            btnId2.setEnabled(false);
            btnId2.setBackgroundColor(Color.LTGRAY);
        }
        if(id1.equals("")) {
            btnGo.setEnabled(false);
            btnGo.setBackgroundColor(Color.LTGRAY);
        }
        else {
            btnGo.setEnabled(true);
            btnGo.setBackgroundColor(Color.rgb(214, 118, 1));
        }
        if(!scandata.equals("Scan_Result") && scandata1.equals("1") && bvalscan == true) {
            list_data(lstdata[0], Integer.parseInt(lstdata[1]), lstdata[2]);
        }
        btnScan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Fragment frag = new ScannedBarcodeActivity();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.frame, frag); // replace a Fragment with Frame Layout
                transaction.commit();
            }
        });

        btnId1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                iselid = enadisbtn(btnId1,btnId2,textId,iselid, lsthfids, (lsthfids.length), 0);
            }
        });

        btnId2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                iselid = enadisbtn(btnId1,btnId2,textId,iselid, lsthfids, (lsthfids.length), 1);
            }
        });

        btnLc1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                iselmon = enadisbtn(btnLc1,btnLc2,textMon,(crmon1-2), spinnerArray1, (spinnerArray1.length), 0);
            }
        });

        btnLc2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                iselmon = enadisbtn(btnLc1,btnLc2,textMon,(crmon1-2), spinnerArray1, (spinnerArray1.length), 1);
            }
        });


        btnRc1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                iselyr = enadisbtn(btnRc1,btnRc2,textYr,iselyr, lstyrs, (lstyrs.length), 0);
            }
        });

        btnRc2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                iselyr = enadisbtn(btnRc1,btnRc2,textYr,iselyr, lstyrs, (lstyrs.length), 1);
            }
        });


        btnList = (Button) view.findViewById(R.id.btnList);

        // Login button click event
        btnList.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
            sid = textId.getText().toString();
            if(iselmon == 1) {
                imon = 12;
            }
            else {
                imon = iselmon;
            }
            cmon = ""+imon;
            stk.removeAllViewsInLayout();
            linear_layout_10.setVisibility(View.VISIBLE);
            if(!sid.equals("Select")) {
                if (!cmon.equals("Select")) {
                    int cmon = imon;
                    String curmon = "";
                    if (cmon < 10)
                        curmon = "0" + cmon;
                    else
                        curmon = "" + cmon;
                    String curyr = textYr.getText().toString();
                    String[] monlst = {"31", "28", "31", "30", "31", "30", "31", "31", "30", "31", "30", "31"};

                    int ilday = Integer.parseInt(monlst[(Integer.parseInt(curmon) - 1)]);
                    if(ilday == 28 && is_leap_year(Integer.parseInt(curyr))) {
                        ilday = 29;
                    }
                    String fdt = curyr + "-" + curmon + "-01";
                    String tdt = curyr + "-" + curmon + "-"+ilday;

                    String sql1 = "SELECT * FROM TsuInfo where tsuid='"+sid+"' and  tsudate between '" + fdt + "' and '" + tdt + "'";
                    Cursor c1 = mDatabase.rawQuery(sql1, null);
                    List lstday = new ArrayList();
                    id1 = "";
                    int tot_rc = 0;
                    if (c1.moveToFirst()) {
                        do {
                            id1 = c1.getString(1);
                            String dt = c1.getString(2);
                            String rc = c1.getString(3);
                            String[] lstday1 = dt.split("-");
                            lstday.add(Integer.parseInt(lstday1[2]));
                        } while (c1.moveToNext());
                    }
                    c1.close();
                    if (!id1.equals("")) {
                        textMsg.setVisibility(View.GONE);
                        trow1.setVisibility(View.VISIBLE);

                        String fdt1 = "";
                        try {
                            for (int i = 1; i <= ilday; i++) {
                                if (!lstday.contains(i)) {
                                    if (i < 10)
                                        fdt1 = curyr + "-" + curmon + "-0" + i;
                                    else
                                        fdt1 = curyr + "-" + curmon + "-" + i;
                                    String sql = "SELECT * FROM TsuInfo where tsuid='"+id1+"' and tsudate ='"+fdt1+"'";
                                    Cursor c = mDatabase.rawQuery(sql, null);
                                    if (c.moveToFirst()) {
                                        do {
                                            String rc = c.getString(3);
                                        } while (c.moveToNext());
                                    }
                                    else {
                                       String SQLiteQuery = "INSERT INTO TsuInfo (tsuid, tsudate, radialcnt) VALUES('" +
                                                id1 + "','" + fdt1 + "','0')";
                                        mDatabase.execSQL(SQLiteQuery);
                                    }
                                    c.close();
                                }
                            }
                        } catch (Exception e) {
                            // how to do the rollback
                            e.printStackTrace();
                        }


                        String sql = "SELECT * FROM TsuInfo where tsuid='"+sid+"' and tsudate between '" + fdt + "' and '" + tdt + "' order by tsudate";
                        Cursor c = mDatabase.rawQuery(sql, null);
                        if (c.moveToFirst()) {
                            do {
                                String id = c.getString(1);
                                String dt = c.getString(2);
                                String rc = c.getString(3);
                                tot_rc = tot_rc + Integer.parseInt(rc);
                                bData = true;
                                TableRow tbrow = new TableRow(getActivity().getApplicationContext());
                                tbrow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                                TextView tvepcid = new TextView(getActivity().getApplicationContext());
                                TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                                layoutParams.weight = 1;
                                tvepcid.setLayoutParams(layoutParams);
                                tvepcid.setText(id);
                                tvepcid.setTextColor(Color.BLACK);
                                tvepcid.setGravity(Gravity.CENTER);
                                tvepcid.setPadding(10, 10, 10, 10);

                                TextView tvtagid = new TextView(getActivity().getApplicationContext());
                                tvtagid.setLayoutParams(layoutParams);
                                tvtagid.setText(dt);
                                tvtagid.setTextColor(Color.BLACK);
                                tvtagid.setGravity(Gravity.CENTER);
                                tvtagid.setPadding(10, 10, 10, 10);

                                TextView tvtime = new TextView(getActivity().getApplicationContext());
                                tvtime.setLayoutParams(layoutParams);
                                tvtime.setText(rc);
                                tvtime.setTextColor(Color.BLACK);
                                tvtime.setGravity(Gravity.CENTER);
                                tvtime.setPadding(10, 10, 10, 10);

                                tbrow.addView(tvepcid);
                                tbrow.addView(tvtagid);
                                tbrow.addView(tvtime);
                                tbrow.setWeightSum(3);
                                stk.addView(tbrow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

                            } while (c.moveToNext());
                        }
                        c.close();
                        textP.setText("Total RC="+tot_rc);
                    }
                    else {
                        trow1.setVisibility(View.GONE);
                        textMsg.setVisibility(View.VISIBLE);
                        textMsg.setText("No records found!");
                        textP.setText("");
                    }
                    if (id1.equals("")) {
                        btnGo.setEnabled(false);
                        btnGo.setBackgroundColor(Color.LTGRAY);
                    }
                    else {
                        btnGo.setEnabled(true);
                        btnGo.setBackgroundColor(Color.rgb(214, 118, 1));
                    }
                } else {
                    Toast.makeText(getActivity(), "Please select month", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(getActivity(), "Please select id", Toast.LENGTH_LONG).show();
            }
            }
        });

        // Login button click event
        btnGo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
             if(bGo == false) {
                 if(bData) {
                     new FillUnqId().execute();
                 }
                 else {
                     Toast.makeText(getActivity(), "No Data to Send", Toast.LENGTH_LONG).show();
                 }
             }
             }
        });
        return view;
    }

    private void list_data(String id1, int imonth, String cyear) {
        sid = id1;
        stk.removeAllViewsInLayout();
        if(bvalscan == true) {
            btnId1.setEnabled(false);btnId2.setEnabled(false);
            btnId1.setBackgroundColor(Color.LTGRAY);btnId2.setBackgroundColor(Color.LTGRAY);
        }
        else {
            trow1.setVisibility(View.GONE);
            textMsg.setVisibility(View.VISIBLE);
            textMsg.setText("No records found!");
            textP.setText("");
            btnGo.setEnabled(true);
            btnGo.setBackgroundColor(Color.rgb(214, 118, 1));
            return;
        }
        linear_layout_10.setVisibility(View.VISIBLE);
        imon = imonth;
        cmon = ""+imon;

        int cmon = imon;
        String curmon = "";
        if (cmon < 10)
            curmon = "0" + cmon;
        else
            curmon = "" + cmon;
        String curyr = cyear;
        String[] monlst = {"31", "28", "31", "30", "31", "30", "31", "31", "30", "31", "30", "31"};

        int ilday = Integer.parseInt(monlst[(Integer.parseInt(curmon) - 1)]);
        if(ilday == 28 && is_leap_year(Integer.parseInt(curyr))) {
            ilday = 29;
        }

        textMsg.setVisibility(View.GONE);
        trow1.setVisibility(View.VISIBLE);
        int tot_rc = 0,tot_rc1 = 0;
        String fdt1 = "";
        int itot = Integer.parseInt(lstdata[ilday+3]);
        for (int i = 1; i <= ilday; i++) {
            String rc = lstdata[i + 2];
            tot_rc1 = tot_rc1 + Integer.parseInt(rc);
        }
        if(itot == tot_rc1) {
            try {
                for (int i = 1; i <= ilday; i++) {
                    if (i < 10)
                        fdt1 = curyr + "-" + curmon + "-0" + i;
                    else
                        fdt1 = curyr + "-" + curmon + "-" + i;

                    String id = id1;
                    String dt = fdt1;
                    String rc = lstdata[i + 2];
                    String rchex = lstdata[ilday+3+i];
                    tot_rc = tot_rc + Integer.parseInt(rc);
                    bData = true;
                    String sql = "SELECT * FROM TsuInfo where tsuid='"+id1+"' and tsudate ='"+fdt1+"'";
                    Cursor c = mDatabase.rawQuery(sql, null);
                    String SQLiteQuery = "";
                    if (c.moveToFirst()) {
                        SQLiteQuery = "UPDATE TsuInfo set radialcnthex='"+rchex+"', radialcnt='"+rc+"' where tsuid='"+id1+"' and tsudate ='"+fdt1+"'";
                    }
                    else {
                        SQLiteQuery = "INSERT INTO TsuInfo (tsuid, tsudate, radialcnt, radialcnthex) VALUES('" +
                                id1 + "','" + fdt1 + "','" + rc + "','" + rchex + "')";
                    }
                    mDatabase.execSQL(SQLiteQuery);
                    TableRow tbrow = new TableRow(getActivity().getApplicationContext());
                    tbrow.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

                    TextView tvepcid = new TextView(getActivity().getApplicationContext());
                    TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                    layoutParams.weight = 1;
                    tvepcid.setLayoutParams(layoutParams);
                    tvepcid.setText(id);
                    tvepcid.setTextColor(Color.BLACK);
                    tvepcid.setGravity(Gravity.CENTER);
                    tvepcid.setPadding(10, 10, 10, 10);

                    TextView tvtagid = new TextView(getActivity().getApplicationContext());
                    tvtagid.setLayoutParams(layoutParams);
                    tvtagid.setText(dt);
                    tvtagid.setTextColor(Color.BLACK);
                    tvtagid.setGravity(Gravity.CENTER);
                    tvtagid.setPadding(10, 10, 10, 10);

                    TextView tvtime = new TextView(getActivity().getApplicationContext());
                    tvtime.setLayoutParams(layoutParams);
                    tvtime.setText(rc);
                    tvtime.setTextColor(Color.BLACK);
                    tvtime.setGravity(Gravity.CENTER);
                    tvtime.setPadding(10, 10, 10, 10);

                    tbrow.addView(tvepcid);
                    tbrow.addView(tvtagid);
                    tbrow.addView(tvtime);
                    tbrow.setWeightSum(3);
                    stk.addView(tbrow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                }
            } catch (Exception e) {
                // how to do the rollback
                e.printStackTrace();
            }
            textP.setText("Total RC=" + tot_rc);
        }
        else {
            trow1.setVisibility(View.GONE);
            textMsg.setVisibility(View.VISIBLE);
            textMsg.setText("No records found!");
            textP.setText("");
        }
        btnGo.setEnabled(true);
        btnGo.setBackgroundColor(Color.rgb(214, 118, 1));
    }

    private boolean is_leap_year(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    private int enadisbtn(Button btn1,Button btn2,TextView txt1,int iv, String[] lstval, int ilen, int ioper) {
        if(btn1==btnLc1) {
            if (ioper == 0) {
                if (iv >= crmon1 - 1) iv = iv - 1;
            } else if (ioper == 1) {
                if (iv <= (crmon1 - 2)) iv = iv + 1;
            }
            iv1 = iv;
            if (iv == crmon1 - 1) {
                btn2.setEnabled(false);
                btn2.setBackgroundColor(Color.LTGRAY);
                btn1.setEnabled(true);
                btn1.setBackgroundColor(Color.rgb(214, 118, 1));
            } else if (iv == crmon1 - 2) {
                btn1.setEnabled(false);
                btn1.setBackgroundColor(Color.LTGRAY);
                btn2.setEnabled(true);
                btn2.setBackgroundColor(Color.rgb(214, 118, 1));
            }
            txt1.setText(lstval[iv]);
        } else {
            if (ioper == 0) {
                if (iv >= 1) {
                    iv = iv - 1;
                    txt1.setText(lstval[iv]);
                    btn1.setEnabled(true);
                    btn1.setBackgroundColor(Color.rgb(214, 118, 1));
                }
                if (iv == 0) {
                    btn1.setEnabled(false);
                    btn1.setBackgroundColor(Color.LTGRAY);
                }

                if (iv == ilen - 1) {
                    btn2.setEnabled(false);
                    btn2.setBackgroundColor(Color.LTGRAY);
                } else {
                    btn2.setEnabled(true);
                    btn2.setBackgroundColor(Color.rgb(214, 118, 1));
                }
            }
            if (ioper == 1) {
                if (iv <= (ilen - 2)) {
                    iv = iv + 1;
                    txt1.setText(lstval[iv]);
                    btn2.setEnabled(true);
                    btn2.setBackgroundColor(Color.rgb(214, 118, 1));
                }
                if (iv == (ilen - 1)) {
                    btn2.setEnabled(false);
                    btn2.setBackgroundColor(Color.LTGRAY);
                }
                if (iv == 0) {
                    btn1.setEnabled(false);
                    btn1.setBackgroundColor(Color.LTGRAY);
                } else {
                    btn1.setEnabled(true);
                    btn1.setBackgroundColor(Color.rgb(214, 118, 1));
                }
            }
        }
        return iv;
    }

    private int getlstIdx(String[] lst, String val) {
        int idx = 0;
        for(int i=0;i<lst.length;i++) {
            idx = i;
            if(val.toUpperCase().equals(lst[i].toUpperCase())) {
                break;
            }
        }
        return idx;
    }



    class FillUnqId extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         * */
        String sres="";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * getting All products from url
         * */
        protected String doInBackground(String... args) {
            SharedPreferences settings1 = getActivity().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
            String baseUrl = settings1.getString("serv_url", "https://hfradarsite.pythonanywhere.com");
            String url = baseUrl + "/upldTsuData";
            //String url="http://niothfradar.pythonanywhere.com/upldTsuData";
            URL object= null;
            try {
                object = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            JSONObject parent = new JSONObject();
            JSONArray cred1 = new JSONArray();

            int cmon = imon;
            String curmon = "";
            if(cmon<10)
                curmon = "0"+cmon;
            else
                curmon = "" + cmon;
            String curyr = textYr.getText().toString();
            String[] monlst = {"31", "28", "31", "30", "31", "30", "31", "31", "30", "31", "30", "31"};

            int ilday = Integer.parseInt(monlst[(Integer.parseInt(curmon) - 1)]);
            if(ilday == 28 && is_leap_year(Integer.parseInt(curyr))) {
                ilday = 29;
            }

            String fdt = curyr + "-" + curmon + "-01";
            String tdt = curyr + "-" + curmon + "-"+ilday;

            String sql = "SELECT * FROM TsuInfo where tsuid='"+sid+"' and tsudate between '"+fdt+"' and '"+tdt+"'";
            Cursor c =  mDatabase.rawQuery( sql, null );
            int cnt=1;
            if (c.moveToFirst()) {
                do {
                    String id = c.getString(1);
                    String dt = c.getString(2);
                    String rc = c.getString(3);
                    String rchex = c.getString(4);
                    JSONObject cred   = new JSONObject();
                    try {
                        cred.put("id",id);
                        cred.put("dt", dt);
                        cred.put("rc", rc);
                        cred.put("rchex", rchex);
                        cred1.put(cred);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    cnt++;
                } while (c.moveToNext());
            }
            c.close();

            try {
                parent.put("cred1", cred1);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            HttpClientExample http = new HttpClientExample();

            try {
                sres=http.sendPost(url,parent);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String file_url) {
            if(!sres.equals("")) {
                alert.showAlertDialog(getActivity(), "Send operation succedded..", "HF Radar data is sent sucessfully", false);
            }
            else {
                alert.showAlertDialog(getActivity(), "Send operation failed..", "HF Radar data is not sent", false);

            }
        }
    }
}