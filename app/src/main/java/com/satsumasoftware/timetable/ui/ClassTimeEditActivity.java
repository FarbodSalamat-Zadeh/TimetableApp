package com.satsumasoftware.timetable.ui;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.satsumasoftware.timetable.R;
import com.satsumasoftware.timetable.TimetableApplication;
import com.satsumasoftware.timetable.db.util.ClassUtils;
import com.satsumasoftware.timetable.framework.ClassTime;
import com.satsumasoftware.timetable.framework.Timetable;
import com.satsuware.usefulviews.LabelledSpinner;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalTime;

import java.util.ArrayList;

public class ClassTimeEditActivity extends AppCompatActivity {

    protected static final String EXTRA_CLASS_TIME = "extra_class_time";
    protected static final String EXTRA_CLASS_DETAIL_ID = "extra_class_detail_id";
    protected static final String EXTRA_TAB_POSITION = "extra_tab_position";

    private int mTabPos;
    private boolean mIsNewTime;

    private ClassTime mClassTime;
    private int mClassDetailId;

    private TextView mStartTimeText, mEndTimeText;

    private DayOfWeek mDayOfWeek;
    private int mWeekNumber;
    private LocalTime mStartTime, mEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_time_edit);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;

        Bundle extras = getIntent().getExtras();
        mClassDetailId = extras.getInt(EXTRA_CLASS_DETAIL_ID);
        mTabPos = extras.getInt(EXTRA_TAB_POSITION);

        if (extras.getParcelable(EXTRA_CLASS_TIME) != null) {
            mClassTime = extras.getParcelable(EXTRA_CLASS_TIME);
        }
        mIsNewTime = mClassTime == null;

        int titleResId = mIsNewTime ? R.string.title_activity_class_time_new :
                R.string.title_activity_class_time_edit;
        getSupportActionBar().setTitle(getResources().getString(titleResId));

        toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCloseAction();
            }
        });

        LabelledSpinner spinnerDay = (LabelledSpinner) findViewById(R.id.spinner_day);
        if (!mIsNewTime) {
            spinnerDay.setSelection(mClassTime.getDay().getValue() - 1);
        }
        spinnerDay.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView,
                                     int position, long id) {
                mDayOfWeek = DayOfWeek.of(position + 1);
            }
            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {}
        });

        Timetable timetable = ((TimetableApplication) getApplication()).getCurrentTimetable();
        assert timetable != null;
        int weekRotations = timetable.getWeekRotations();

        mWeekNumber = mIsNewTime ? 1 : mClassTime.getWeekNumber();
        LabelledSpinner spinnerWeek = (LabelledSpinner) findViewById(R.id.spinner_week);

        if (timetable.hasFixedScheduling()) {
            spinnerWeek.setVisibility(View.GONE);

        } else {
            ArrayList<String> weekItems = new ArrayList<>();
            for (int i = 1; i <= weekRotations; i++) {
                String item = getString(R.string.week_item, String.valueOf(i));
                weekItems.add(item);
            }

            spinnerWeek.setItemsArray(weekItems);
            spinnerWeek.setSelection(mWeekNumber - 1);
            spinnerWeek.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
                @Override
                public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView,
                                         int position, long id) {
                    mWeekNumber = position + 1;
                }
                @Override
                public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {}
            });
        }

        mStartTimeText = (TextView) findViewById(R.id.textView_start_time);
        mEndTimeText = (TextView) findViewById(R.id.textView_end_time);

        if (!mIsNewTime) {
            mStartTime = mClassTime.getStartTime();
            mEndTime = mClassTime.getEndTime();
            updateTimeTexts();
        }

        mStartTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int initialHour = 9;
                int initialMinute = 0;
                if (mStartTime != null) {
                    initialHour = mStartTime.getHour();
                    initialMinute = mStartTime.getMinute();
                }

                new TimePickerDialog(ClassTimeEditActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        mStartTime = LocalTime.of(hour, minute);
                        updateTimeTexts();
                    }
                }, initialHour, initialMinute, true).show();
            }
        });
        mEndTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int initialHour = 9;
                int initialMinute = 0;
                if (mEndTime != null) {
                    initialHour = mEndTime.getHour();
                    initialMinute = mEndTime.getMinute();
                }
                new TimePickerDialog(ClassTimeEditActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                        mEndTime = LocalTime.of(hour, minute);
                        updateTimeTexts();
                    }
                }, initialHour, initialMinute, true).show();
            }
        });
    }

    private void updateTimeTexts() {
        if (mStartTime != null) {
            mStartTimeText.setText(mStartTime.toString());
            mStartTimeText.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.mdu_text_black));
        }
        if (mEndTime != null) {
            mEndTimeText.setText(mEndTime.toString());
            mEndTimeText.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.mdu_text_black));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_edit, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mIsNewTime) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                handleDoneAction();
                break;
            case R.id.action_delete:
                handleDeleteAction();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleCloseAction();
        super.onBackPressed();
    }

    private void handleCloseAction() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private void handleDoneAction() {
        if (mStartTime == null || mEndTime == null) {
            Snackbar.make(findViewById(R.id.rootView),
                    R.string.message_times_required, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (mStartTime.equals(mEndTime)) {
            Snackbar.make(findViewById(R.id.rootView),
                    R.string.message_start_time_equal_end, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (mStartTime.isAfter(mEndTime)) {
            Snackbar.make(findViewById(R.id.rootView),
                    R.string.message_start_time_after_end, Snackbar.LENGTH_SHORT).show();
            return;
        }

        int id = mIsNewTime ? ClassUtils.getHighestClassTimeId(this) + 1 : mClassTime.getId();

        Timetable timetable = ((TimetableApplication) getApplication()).getCurrentTimetable();
        assert timetable != null;

        mClassTime = new ClassTime(id, timetable.getId(), mClassDetailId, mDayOfWeek, mWeekNumber,
                mStartTime, mEndTime);

        if (mIsNewTime) {
            ClassUtils.addClassTime(this, mClassTime);
        } else {
            ClassUtils.replaceClassTime(this, mClassTime.getId(), mClassTime);
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_TAB_POSITION, mTabPos);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void handleDeleteAction() {
        ClassUtils.completelyDeleteClassTime(this, mClassTime.getId());
        Intent intent = new Intent();
        intent.putExtra(EXTRA_TAB_POSITION, mTabPos);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

}
