package com.example.hfradar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "FieldSyncHistory.db";
    private static final int DATABASE_VERSION = 2;

    // History table
    public static final String TABLE_HISTORY = "history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SITE = "site";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_TOTAL_RADIAL = "total_radial";
    public static final String COLUMN_METHOD = "method"; // "Add Data" or "QR Code Scan"
    public static final String COLUMN_STATUS = "status"; // "Success" or "Failed"
    public static final String COLUMN_SENT_DATE = "sent_date";
    public static final String COLUMN_SENT_TIME = "sent_time";

    // Saved data table
    public static final String TABLE_SAVED_DATA = "saved_data";
    public static final String COLUMN_RADIAL_DATA = "radial_data"; // JSON array of radial counts
    public static final String COLUMN_REASONS_DATA = "reasons_data"; // JSON array of reason strings
    public static final String COLUMN_IS_SENT = "is_sent";
    public static final String COLUMN_CREATED_DATE = "created_date";
    public static final String COLUMN_MODIFIED_DATE = "modified_date";

    private static final String CREATE_HISTORY_TABLE =
        "CREATE TABLE " + TABLE_HISTORY + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_SITE + " TEXT NOT NULL, " +
        COLUMN_MONTH + " INTEGER NOT NULL, " +
        COLUMN_YEAR + " INTEGER NOT NULL, " +
        COLUMN_TOTAL_RADIAL + " INTEGER NOT NULL, " +
        COLUMN_METHOD + " TEXT NOT NULL, " +
        COLUMN_STATUS + " TEXT NOT NULL, " +
        COLUMN_SENT_DATE + " TEXT NOT NULL, " +
        COLUMN_SENT_TIME + " TEXT NOT NULL)";

    private static final String CREATE_SAVED_DATA_TABLE =
        "CREATE TABLE " + TABLE_SAVED_DATA + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_SITE + " TEXT NOT NULL, " +
        COLUMN_MONTH + " INTEGER NOT NULL, " +
        COLUMN_YEAR + " INTEGER NOT NULL, " +
        COLUMN_RADIAL_DATA + " TEXT NOT NULL, " +
        COLUMN_REASONS_DATA + " TEXT, " +
        COLUMN_TOTAL_RADIAL + " INTEGER NOT NULL, " +
        COLUMN_IS_SENT + " INTEGER DEFAULT 0, " +
        COLUMN_CREATED_DATE + " TEXT NOT NULL, " +
        COLUMN_MODIFIED_DATE + " TEXT NOT NULL, " +
        "UNIQUE(" + COLUMN_SITE + ", " + COLUMN_MONTH + ", " + COLUMN_YEAR + "))";

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_HISTORY_TABLE);
        db.execSQL(CREATE_SAVED_DATA_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add reasons_data column to saved_data table
            try {
                db.execSQL("ALTER TABLE " + TABLE_SAVED_DATA + " ADD COLUMN " + COLUMN_REASONS_DATA + " TEXT");
            } catch (Exception e) {
                // Column may already exist
            }
        }
    }

    // Add history entry
    public long addHistoryEntry(String site, int month, int year, int totalRadial,
                                 String method, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Date now = new Date();

        values.put(COLUMN_SITE, site);
        values.put(COLUMN_MONTH, month);
        values.put(COLUMN_YEAR, year);
        values.put(COLUMN_TOTAL_RADIAL, totalRadial);
        values.put(COLUMN_METHOD, method);
        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_SENT_DATE, dateFormat.format(now));
        values.put(COLUMN_SENT_TIME, timeFormat.format(now));

        long id = db.insert(TABLE_HISTORY, null, values);
        db.close();
        return id;
    }

    // Get all history entries
    public List<HistoryEntry> getAllHistory() {
        List<HistoryEntry> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_ID + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                HistoryEntry entry = new HistoryEntry();
                entry.id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                entry.site = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE));
                entry.month = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MONTH));
                entry.year = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR));
                entry.totalRadial = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_RADIAL));
                entry.method = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_METHOD));
                entry.status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                entry.sentDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENT_DATE));
                entry.sentTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SENT_TIME));
                historyList.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }

    // Save or update data
    public long saveData(String site, int month, int year, String radialDataJson, int totalRadial) {
        return saveData(site, month, year, radialDataJson, null, totalRadial);
    }

    // Save or update data with reasons
    public long saveData(String site, int month, int year, String radialDataJson, String reasonsDataJson, int totalRadial) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String now = dateFormat.format(new Date());

        values.put(COLUMN_SITE, site);
        values.put(COLUMN_MONTH, month);
        values.put(COLUMN_YEAR, year);
        values.put(COLUMN_RADIAL_DATA, radialDataJson);
        values.put(COLUMN_REASONS_DATA, reasonsDataJson);
        values.put(COLUMN_TOTAL_RADIAL, totalRadial);
        values.put(COLUMN_IS_SENT, 0);
        values.put(COLUMN_MODIFIED_DATE, now);

        // Check if entry exists
        String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_SAVED_DATA +
                       " WHERE " + COLUMN_SITE + "=? AND " + COLUMN_MONTH + "=? AND " + COLUMN_YEAR + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{site, String.valueOf(month), String.valueOf(year)});

        long id;
        if (cursor.moveToFirst()) {
            // Update existing
            int existingId = cursor.getInt(0);
            db.update(TABLE_SAVED_DATA, values, COLUMN_ID + "=?", new String[]{String.valueOf(existingId)});
            id = existingId;
        } else {
            // Insert new
            values.put(COLUMN_CREATED_DATE, now);
            id = db.insert(TABLE_SAVED_DATA, null, values);
        }
        cursor.close();
        db.close();
        return id;
    }

    // Get saved data for site/month/year
    public SavedDataEntry getSavedData(String site, int month, int year) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SAVED_DATA +
                       " WHERE " + COLUMN_SITE + "=? AND " + COLUMN_MONTH + "=? AND " + COLUMN_YEAR + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{site, String.valueOf(month), String.valueOf(year)});

        SavedDataEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = new SavedDataEntry();
            entry.id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            entry.site = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE));
            entry.month = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MONTH));
            entry.year = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR));
            entry.radialDataJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RADIAL_DATA));
            try {
                int reasonsIdx = cursor.getColumnIndex(COLUMN_REASONS_DATA);
                if (reasonsIdx >= 0) {
                    entry.reasonsDataJson = cursor.getString(reasonsIdx);
                }
            } catch (Exception e) {
                // Column may not exist in older DB versions
            }
            entry.totalRadial = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_RADIAL));
            entry.isSent = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SENT)) == 1;
            entry.createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE));
            entry.modifiedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODIFIED_DATE));
        }
        cursor.close();
        db.close();
        return entry;
    }

    // Get all unsent saved data
    public List<SavedDataEntry> getUnsentData() {
        List<SavedDataEntry> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_SAVED_DATA + " WHERE " + COLUMN_IS_SENT + "=0 ORDER BY " + COLUMN_ID + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                SavedDataEntry entry = new SavedDataEntry();
                entry.id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                entry.site = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE));
                entry.month = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MONTH));
                entry.year = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_YEAR));
                entry.radialDataJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RADIAL_DATA));
                entry.totalRadial = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_RADIAL));
                entry.isSent = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SENT)) == 1;
                entry.createdDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_DATE));
                entry.modifiedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MODIFIED_DATE));
                dataList.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return dataList;
    }

    // Mark data as sent
    public void markAsSent(String site, int month, int year) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SENT, 1);
        db.update(TABLE_SAVED_DATA, values,
                  COLUMN_SITE + "=? AND " + COLUMN_MONTH + "=? AND " + COLUMN_YEAR + "=?",
                  new String[]{site, String.valueOf(month), String.valueOf(year)});
        db.close();
    }

    // Helper classes
    public static class HistoryEntry {
        public int id;
        public String site;
        public int month;
        public int year;
        public int totalRadial;
        public String method;
        public String status;
        public String sentDate;
        public String sentTime;

        public String getMonthName() {
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                              "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            if (month >= 1 && month <= 12) {
                return months[month - 1];
            }
            return "";
        }
    }

    public static class SavedDataEntry {
        public int id;
        public String site;
        public int month;
        public int year;
        public String radialDataJson;
        public String reasonsDataJson;
        public int totalRadial;
        public boolean isSent;
        public String createdDate;
        public String modifiedDate;

        public String getMonthName() {
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                              "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            if (month >= 1 && month <= 12) {
                return months[month - 1];
            }
            return "";
        }
    }
}
