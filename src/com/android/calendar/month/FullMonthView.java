/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar.month;

import com.android.calendar.CalendarController;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.Event;
import com.android.calendar.EventLoader;
import com.android.calendar.R;
import com.android.calendar.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;

public class FullMonthView extends MiniMonthView {
    static final String TAG = "FullMonthView";
    static final boolean DEBUG = false;

    protected static int MAX_TITLE_LENGTH = 25;
    protected static int MAX_EVENTS_PER_DAY = 2;
    protected static int EVENT_TEXT_SIZE = 14;
    protected static int EVENT_TEXT_BUFFER = 2;
    protected static String ALL_DAY_STRING;

    protected int[] mEventDayCounts = new int[mEventNumDays];
    protected ArrayList<ArrayList<Event>> mEventDayList = new ArrayList<ArrayList<Event>>();
    protected static StringBuilder mStringBuilder = new StringBuilder(50);
    protected static Formatter mFormatter = new Formatter(
            mStringBuilder, Locale.getDefault());
    protected Resources mRes;

    public FullMonthView(Context activity, CalendarController controller, EventLoader eventLoader) {
        super(activity, controller, eventLoader);
        mRes = getResources();
        ALL_DAY_STRING = mRes.getString(R.string.edit_event_all_day_label);

        mDesiredCellHeight = (int) (100 * mScale);
        MONTH_DAY_TEXT_SIZE = (int) (12 * mScale);
        MONTH_NAME_PADDING = (int) (2 * mScale);
        EVENT_TEXT_SIZE *= mScale;
        EVENT_TEXT_BUFFER *= mScale;
        mMonthNameSpace = 0;
        mGoToView = ViewType.DETAIL;
    }

    /**
     * Draw the names of the month on the left.
     */
    @Override
    protected void drawMonthNames(Canvas canvas, Paint p) {

    }

    /**
     * Draw the grid lines for the calendar
     * @param canvas The canvas to draw on.
     * @param p The paint used for drawing.
     */
    @Override
    protected void drawGrid(Canvas canvas, Paint p) {
        p.setColor(mMonthGridLineColor);
        p.setAntiAlias(false);

        final int width = mWidth + mMonthNameSpace;
        final int height = mHeight;

        int count = 0;
        int y = mWeekOffset;
        if (y > mCellHeight) {
            y -= mCellHeight;
        }
        while (y <= height) {
            canvas.drawLine(mMonthNameSpace, y, width, y, p);
            // Compute directly to avoid rounding errors
            count++;
            y = count * height / mNumWeeks + mWeekOffset - 1;
        }

        int x = mMonthNameSpace;
        count = 0;
        while (x <= width) {
            canvas.drawLine(x, WEEK_GAP, x, height, p);
            count++;
            // Compute directly to avoid rounding errors
            x = count * mWidth / 7 + mBorder - 1 + mMonthNameSpace;
        }
    }

    @Override
    protected void drawBox(int day, int row, int column, Canvas canvas, Paint p,
            Rect r, boolean isLandscape) {
        int julianDay = Time.getJulianDay(mDrawingDay.toMillis(true), mDrawingDay.gmtoff);

        // Check if we're in a light or dark colored month
        boolean colorSameAsCurrent = ((mDrawingDay.month & 1) == 0) == mIsEvenMonth;
        // We calculate the position relative to the total size
        // to avoid rounding errors.
        int y = row * mHeight / mNumWeeks + mWeekOffset;
        int x = column * mWidth / 7 + mBorder + mMonthNameSpace;

        r.left = x;
        r.top = y;
        r.right = x + mCellWidth;
        r.bottom = y + mCellHeight;

        // Draw the cell contents (excluding monthDay number)
        if (mDrawingSelected) {
            if (mSelectionMode == SELECTION_SELECTED) {
                mBoxSelected.setBounds(r);
                mBoxSelected.draw(canvas);
            } else if (mSelectionMode == SELECTION_PRESSED) {
                mBoxPressed.setBounds(r);
                mBoxPressed.draw(canvas);
            } else {
                mBoxLongPressed.setBounds(r);
                mBoxLongPressed.draw(canvas);
            }
        } else {
            // Adjust cell boundaries to compensate for the different border
            // style.
            r.top--;
            if (column != 0) {
                r.left--;
            }
            p.setStyle(Style.FILL);
            if (mDrawingToday) {
                p.setColor(mMonthTodayBgColor);
            } else if (!colorSameAsCurrent) {
                p.setColor(mMonthOtherMonthBgColor);
            } else {
                p.setColor(mMonthBgColor);
            }
            canvas.drawRect(r, p);
        }



        p.setStyle(Paint.Style.FILL);
        p.setAntiAlias(true);
        p.setTypeface(null);

        drawEvents(julianDay, canvas, r, p, colorSameAsCurrent);

        // Draw the monthDay number
        p.setTextSize(MONTH_DAY_TEXT_SIZE);

        if (mDrawingToday && !mDrawingSelected) {
            p.setColor(mMonthTodayNumberColor);
        } else if (Utils.isSunday(column, mStartDayOfWeek)) {
            p.setColor(mMonthSundayColor);
        } else if (Utils.isSaturday(column, mStartDayOfWeek)) {
            p.setColor(mMonthSaturdayColor);
        } else {
            p.setColor(mMonthDayNumberColor);
        }

        // bolds the day if there's an event that day
        int julianOffset = julianDay - mFirstEventJulianDay;
        if (julianOffset >= 0 && julianOffset < mEventNumDays) {
            p.setFakeBoldText(mEventDays[julianOffset]);
        } else {
            p.setFakeBoldText(false);
        }
        /*Drawing of day number is done here
         *easy to find tags draw number draw day*/
        p.setTextAlign(Paint.Align.LEFT);
        // center of text
        // TODO figure out why it's not actually centered
        int textX = x + TEXT_TOP_MARGIN;
        // bottom of text
        int textY = y + TEXT_TOP_MARGIN + MONTH_DAY_TEXT_SIZE;
        canvas.drawText(String.valueOf(mDrawingDay.monthDay), textX, textY, p);
    }

    ///Create and draw the event busybits for this day
    @Override
    protected void drawEvents(int date, Canvas canvas, Rect rect, Paint p, boolean drawBg) {
        int julianOffset = date - mFirstEventJulianDay;
        if (julianOffset < 0 || julianOffset >= mEventDayList.size()) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Drawing event for " + date);
        }

        // The point at which to start drawing the event text
        int bot = rect.top + TEXT_TOP_MARGIN + MONTH_DAY_TEXT_SIZE + EVENT_TEXT_SIZE
                + EVENT_TEXT_BUFFER;
        int left = rect.left + TEXT_TOP_MARGIN;

        p.setTextAlign(Paint.Align.LEFT);
        p.setTextSize(EVENT_TEXT_SIZE);

        ArrayList<Event> dayEvents = mEventDayList.get(julianOffset);
        int numEvents = dayEvents.size();
        Time time = new Time();
        int j = 0;
        for (int i = 0; i < MAX_EVENTS_PER_DAY; i++) {
            if (mDrawingToday) {
                long millis = System.currentTimeMillis();
                while (i + j < numEvents && dayEvents.get(i + j).endMillis < millis) {
                    j++;
                }
            }
            if (i + j >= numEvents) {
                return;
            }
            Event event = dayEvents.get(i + j);
            CharSequence disp = event.title;
            if (disp.length() > MAX_TITLE_LENGTH) {
                disp = disp.subSequence(0, MAX_TITLE_LENGTH);
            }
            p.setColor(mMonthDayNumberColor);
            if (mDrawingToday && !mDrawingSelected) {
                p.setColor(mMonthTodayNumberColor);
            } else {
                p.setColor(mMonthDayNumberColor);
            }
            canvas.drawText(disp.toString(), left, bot, p);

            // Have to clear out the sb or it will keep appending forever
            mStringBuilder.setLength(0);
            if (!event.allDay) {
            disp = DateUtils.formatDateRange(mContext, mFormatter, event.startMillis,
                    event.endMillis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL)
                    .toString();
            } else {
                disp = DateUtils.formatDateRange(mContext, mFormatter, event.startMillis,
                        event.endMillis, DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_UTC)
                        .toString();
            }

            bot += EVENT_TEXT_SIZE + EVENT_TEXT_BUFFER;
            if (mDrawingToday && !mDrawingSelected) {
                p.setColor(mMonthDayNumberColor);
            } else {
                p.setColor(mMonthOtherMonthDayNumberColor);
            }
            canvas.drawText(disp.toString(), left, bot, p);

            bot += EVENT_TEXT_SIZE + EVENT_TEXT_BUFFER;
        }
        if (numEvents > MAX_EVENTS_PER_DAY - j) {
            int value = numEvents - MAX_EVENTS_PER_DAY - j;
            String format = mRes.getQuantityString(R.plurals.gadget_more_events, value);
            canvas.drawText(String.format(format, value), left, bot, p);
        }
    }

    @Override
    public void reloadEvents() {
        if (!mShowDNA) {
            return;
        }

        long millis = getFirstEventStartMillis();

        // Load the days with events in the background
//FRAG_TODO        mParentActivity.startProgressSpinner();
        final long startMillis;
        if (PROFILE_LOAD_TIME) {
            startMillis = SystemClock.uptimeMillis();
        } else {
            // To avoid a compiler error that this variable might not be initialized.
            startMillis = 0;
        }

        final ArrayList<Event> events = new ArrayList<Event>();
        mEventLoader.loadEventsInBackground(mEventNumDays, events, millis, new Runnable() {
            public void run() {
                if (DEBUG) {
                    Log.d(TAG, "found " + events.size() + " events");
                }
                if (mEventDayList.size() != mEventNumDays) {
                    mEventDayList.clear();
                    for (int i = 0; i < mEventNumDays; i++) {
                        mEventDayList.add(new ArrayList<Event>());
                    }
                } else {
                    for (int i = 0; i < mEventNumDays; i++) {
                        mEventDayList.get(i).clear();
                    }
                }
                mEvents = events;
//FRAG_TODO                mParentActivity.stopProgressSpinner();
                int numEvents = events.size();

                long millis = getFirstEventStartMillis();
                mFirstEventJulianDay = Time.getJulianDay(millis, mTempTime.gmtoff);
                // Compute the new set of days with events
                for (int i = 0; i < numEvents; i++) {
                    Event event = events.get(i);
                    int startDay = event.startDay - mFirstEventJulianDay;
                    int endDay = event.endDay - mFirstEventJulianDay + 1;
                    if (startDay < mEventNumDays || endDay >= 0) {
                        if (startDay < 0) {
                            startDay = 0;
                        }
                        if (startDay > mEventNumDays) {
                            continue;
                        }
                        if (endDay < 0) {
                            continue;
                        }
                        if (endDay > mEventNumDays) {
                            endDay = mEventNumDays;
                        }
                        for (int j = startDay; j < endDay; j++) {
                            mEventDayList.get(j).add(event);
                        }
                    }
                }
                mRedrawScreen = true;
                invalidate();
            }
        }, null);
    }

    @Override
    protected void drawingCalc(int width, int height) {
        super.drawingCalc(width, height);
        mEventDayCounts = new int[mEventNumDays];
    }
}
