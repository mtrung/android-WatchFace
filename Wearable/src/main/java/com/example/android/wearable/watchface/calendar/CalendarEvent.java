package com.example.android.wearable.watchface.calendar;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.wearable.provider.WearableCalendarContract;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Created by tvo on 5/27/15.
 *
 * Represent a single event including recurring event and excluding all day event.
 * Work on both phone-side & wear-side.
 */
public class CalendarEvent implements Comparable<CalendarEvent> {
    private static final String TAG = CalendarEvent.class.getSimpleName();

    private String title;
    public long start;
    public int allDay;

	public void setCursor(Cursor cursor) {
        int columnId = 0;
        title = cursor.getString(columnId++);
        start = cursor.getLong(columnId++);
        allDay = cursor.getInt(columnId++);
	}

    static SimpleDateFormat timeFormat;
    static SimpleDateFormat dateFormat;

	public String toString() {
        if (dateFormat==null) dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy");

        //return String.format("Event: %s %s allDay=%d", dateFormat.format(start), getTimeLeftString(false), allDay);
        return String.format("Event: %s %s", dateFormat.format(start), getTimeLeftString(2));
    }

    @Override
    public int compareTo(CalendarEvent other) {
        return (int)(start - other.start);
    }

    public long getTimeLeftMinute() {
        long now = System.currentTimeMillis();
        long diff = start - now;
        long minuteDiff = (diff / (60 * 1000));
        return minuteDiff;
    }

    public String getTimeLeftString(int format) {
        long timeLeft = getTimeLeftMinute();

        if (format == 0) {
            return formatTimeLeft(timeLeft);
        }

        Date startDate = new Date(start);
        if (timeFormat==null) timeFormat = new SimpleDateFormat("h:mma");

        if (format == 1) {
            return String.format("[%s %s]", timeFormat.format(startDate), title);
        }

        return String.format("[%s %s] %s", timeFormat.format(startDate), title, formatTimeLeft(timeLeft));
    }


    //  ...static

    public static String formatTimeLeft(long minutes) {
        if (minutes < 60) {
            return minutes + " MIN";
        } else return String.format("%d:%02d HRS", minutes / 60, minutes % 60);
    }


    public static final String[] FIELDS = {
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Events.ALL_DAY
    };

    public static CalendarEvent newInstance(Cursor csr) {
        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setCursor(csr);
        return calendarEvent;
    }

    /**
     * To get next events (single events + recurring events - all day events)
     * @param context
     * @param calendarId google calendar id. Pass in -1 to run on wear-side.
     * @param hourRange
     * @return ArrayList<CalendarEvent>
     */
    public static ArrayList<CalendarEvent> getNextEvents(Context context, long calendarId, int hourRange) {
        ContentResolver contentResolver = context.getContentResolver();

        long now = System.currentTimeMillis();
        long next = now + (DateUtils.HOUR_IN_MILLIS * hourRange);

        Uri.Builder eventsUriBuilder;
        String selection;
        String sortOrder;

        if (calendarId == -1) {
            eventsUriBuilder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
            //  ...exclude all day events
            selection = String.format("%s=0", CalendarContract.Events.ALL_DAY);
            //  ...WearableCalendarContract doesn't support sortOrder query
            sortOrder = null;
        }
        else {
            eventsUriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            //  ...exclude all day events
            selection = String.format("%s=%d AND %s=0",
                    CalendarContract.Events.CALENDAR_ID, calendarId,
                    CalendarContract.Events.ALL_DAY);
            sortOrder = CalendarContract.Instances.BEGIN + " ASC";
        }

        ContentUris.appendId(eventsUriBuilder, now);
        ContentUris.appendId(eventsUriBuilder, next);
        Uri uri = eventsUriBuilder.build();
        //Log.d("CalendarEvent", "query: " + uri.toString() + "\n" + selection);

        Cursor cursor;
        try {
            cursor = contentResolver.query(uri, FIELDS, selection, null, sortOrder);
        } catch (Exception ex) {
            Log.e(TAG, ex.getLocalizedMessage());
            return null;
        }

        if (cursor.getCount() == 0) {
            Log.d(TAG, "No event for the query");
            return null;
        }

        ArrayList<CalendarEvent> events = new ArrayList<>();
        while (cursor.moveToNext()) {
            CalendarEvent event =  CalendarEvent.newInstance(cursor);
            Log.d(TAG, event.toString());

            //  ...filter out all day event
            if (event.allDay == 1) continue;

            events.add(event);
        }

        //  ...sort events here since WearableCalendarContract doesn't support sortOrder query
        if (calendarId == -1) {
            Collections.sort(events);
        }

        Log.d(TAG, "Next events found: " + events.size());
        return events;
    }
}
