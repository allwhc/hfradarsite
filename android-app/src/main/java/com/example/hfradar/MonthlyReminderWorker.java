package com.example.hfradar;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MonthlyReminderWorker extends Worker {

    public static final String WORK_NAME = "monthly_reminder_work";
    private static final String PREFS_NAME = "ReminderPrefs";
    private static final String PREF_REMINDERS_ENABLED = "reminders_enabled";

    public MonthlyReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Check if reminders are enabled
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean remindersEnabled = prefs.getBoolean(PREF_REMINDERS_ENABLED, true);

        if (!remindersEnabled) {
            return Result.success();
        }

        // Check if today is the 1st of the month
        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.DAY_OF_MONTH) == 1) {
            // Create notification channel (required for Android 8+)
            NotificationHelper.createNotificationChannel(context);

            // Show the reminder notification
            NotificationHelper.showMonthlyReminder(context);
        }

        return Result.success();
    }

    /**
     * Schedule the monthly reminder worker.
     * Runs daily and checks if it's the 1st of the month.
     */
    public static void schedule(Context context) {
        // Calculate initial delay to 9 AM
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 9);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);

        // If 9 AM has passed today, schedule for tomorrow
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long initialDelay = target.getTimeInMillis() - now.getTimeInMillis();

        // Create periodic work request - runs every 24 hours
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                MonthlyReminderWorker.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        // Enqueue with KEEP policy to avoid duplicate scheduling
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    /**
     * Cancel the scheduled reminder worker.
     */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }

    /**
     * Check if reminders are enabled in preferences.
     */
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_REMINDERS_ENABLED, true);
    }

    /**
     * Set reminder enabled state in preferences.
     */
    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_REMINDERS_ENABLED, enabled).apply();
    }
}
