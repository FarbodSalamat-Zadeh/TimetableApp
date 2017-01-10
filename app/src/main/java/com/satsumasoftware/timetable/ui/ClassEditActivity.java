package com.satsumasoftware.timetable.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.satsumasoftware.timetable.R;
import com.satsumasoftware.timetable.TimetableApplication;
import com.satsumasoftware.timetable.db.util.ClassUtils;
import com.satsumasoftware.timetable.db.util.SubjectUtils;
import com.satsumasoftware.timetable.framework.Class;
import com.satsumasoftware.timetable.framework.ClassDetail;
import com.satsumasoftware.timetable.framework.ClassTime;
import com.satsumasoftware.timetable.framework.ClassTimeGroup;
import com.satsumasoftware.timetable.framework.Color;
import com.satsumasoftware.timetable.framework.Subject;
import com.satsumasoftware.timetable.framework.Timetable;
import com.satsumasoftware.timetable.ui.adapter.ClassTimesAdapter;
import com.satsumasoftware.timetable.ui.adapter.SubjectsAdapter;
import com.satsumasoftware.timetable.util.TextUtilsKt;
import com.satsumasoftware.timetable.util.UiUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Invoked and displayed to the user to edit the details of a class.
 *
 * Currently, it is also responsible for showing the details, since there is no activity dedicated
 * to merely displaying the details (like in {@link AssignmentDetailActivity}).
 *
 * It can also be called to create a new class. If so, there will be no intent extra data supplied
 * to this activity (i.e. {@link #EXTRA_CLASS} will be null).
 *
 * @see Class
 */
public class ClassEditActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ClassDetailActivity";

    /**
     * The key for the {@link Class} passed through an intent extra.
     *
     * It should be null if we're creating a new class.
     */
    protected static final String EXTRA_CLASS = "extra_class";

    /**
     * The key for the integer identifier of the {@link ClassDetail} to be displayed.
     *
     * Because class details in this activity are shown each as separate tabs, we use this value
     * to determine which tab should be shown to the user. It can also be null, notably if we're
     * creating a new class.
     *
     * @see ClassDetail#getId()
     */
    protected static final String EXTRA_CLASS_DETAIL_ID = "extra_class_detail_id";

    protected static final int REQUEST_CODE_SUBJECT_DETAIL = 2;
    protected static final int REQUEST_CODE_CLASS_TIME_DETAIL = 3;

    private boolean mIsNew;

    private int mNewDetailIdCount = 1;

    private Class mClass;
    private ArrayList<Integer> mClassDetailIds;

    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private TabLayout mTabLayout;

    private Subject mSubject;
    private TextView mSubjectText;
    private AlertDialog mSubjectDialog;

    private EditText mEditTextModule;

    private LocalDate mStartDate, mEndDate;
    private TextView mStartDateText, mEndDateText;

    private DynamicPagerAdapter mPagerAdapter;

    private ArrayList<ArrayList<ClassTimeGroup>> mAllClassTimeGroups;
    private ArrayList<ClassTimesAdapter> mAdapters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_edit);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        assert getSupportActionBar() != null;

        int displayedDetailId = -1;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mClass = extras.getParcelable(EXTRA_CLASS);
            displayedDetailId = extras.getInt(EXTRA_CLASS_DETAIL_ID, -1);
        }
        mIsNew = mClass == null;

        int titleResId = mIsNew ? R.string.title_activity_class_new :
                R.string.title_activity_class_edit;
        getSupportActionBar().setTitle(getResources().getString(titleResId));

        mToolbar.setNavigationIcon(UiUtils.tintDrawable(this, R.drawable.ic_close_black_24dp));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCloseAction();
            }
        });

        setupLayout(displayedDetailId);
    }

    private void setupLayout(int displayedDetailId) {
        setupSubjectText();

        mEditTextModule = (EditText) findViewById(R.id.editText_module);
        if (!mIsNew) {
            mEditTextModule.setText(mClass.getModuleName());
        }

        setupDateTexts();
        setupDateSwitch();

        setupExpandToggle();

        setupTabs(displayedDetailId);
    }

    private void setupTabs(int displayedDetailId) {
        mTabLayout = (TabLayout) findViewById(R.id.tabLayout);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        mPagerAdapter = new DynamicPagerAdapter();

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(mPagerAdapter);

        mTabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        mTabLayout.setTabTextColors(
                ContextCompat.getColor(this, R.color.mdu_text_white_secondary),
                ContextCompat.getColor(this, R.color.mdu_text_white));

        populateTabs(viewPager, displayedDetailId);
    }

    private void populateTabs(ViewPager viewPager, int displayedDetailId) {
        mClassDetailIds = new ArrayList<>();

        mAllClassTimeGroups = new ArrayList<>();
        mAdapters = new ArrayList<>();

        if (!mIsNew) {
            mSubject = Subject.create(this, mClass.getSubjectId());
            updateLinkedSubject();

            ArrayList<ClassDetail> classDetails =
                    ClassUtils.getClassDetailsForClass(this, mClass.getId());
            for (ClassDetail classDetail : classDetails) {
                addDetailTab(classDetail, false);
            }
        } else {
            addDetailTab(null, false);  // first tab for adding detail
        }
        addDetailTab(null, true);

        // Go to the tab sent via the intent with displayedDetailId
        goToTab(displayedDetailId, viewPager);
    }

    private void goToTab(int displayedDetailId, ViewPager viewPager) {
        if (displayedDetailId == -1) {
            return;
        }

        int tabCount = 0;
        for (int classDetailId : mClassDetailIds) {
            if (classDetailId == displayedDetailId) {
                break;
            }
            tabCount++;
        }
        viewPager.setCurrentItem(tabCount);
    }

    private void setupSubjectText() {
        mSubjectText = (TextView) findViewById(R.id.textView_subject);

        mSubjectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ClassEditActivity.this);

                final ArrayList<Subject> subjects =
                        SubjectUtils.getSubjects(ClassEditActivity.this);
                Collections.sort(subjects, new Comparator<Subject>() {
                    @Override
                    public int compare(Subject subject, Subject t1) {
                        return subject.getName().compareTo(t1.getName());
                    }
                });

                SubjectsAdapter adapter = new SubjectsAdapter(getBaseContext(), subjects);
                adapter.setOnEntryClickListener(new SubjectsAdapter.OnEntryClickListener() {
                    @Override
                    public void onEntryClick(View view, int position) {
                        mSubject = subjects.get(position);
                        mSubjectDialog.dismiss();
                        updateLinkedSubject();
                    }
                });

                RecyclerView recyclerView = new RecyclerView(getBaseContext());
                recyclerView.setHasFixedSize(true);
                recyclerView.setLayoutManager(new LinearLayoutManager(ClassEditActivity.this));
                recyclerView.setAdapter(adapter);

                View titleView =
                        getLayoutInflater().inflate(R.layout.dialog_title_with_padding, null);
                ((TextView) titleView.findViewById(R.id.title)).setText(R.string.choose_subject);

                builder.setView(recyclerView)
                        .setCustomTitle(titleView)
                        .setPositiveButton(R.string.action_new, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(
                                        ClassEditActivity.this, SubjectEditActivity.class);
                                startActivityForResult(intent, REQUEST_CODE_SUBJECT_DETAIL);
                            }
                        });

                mSubjectDialog = builder.create();
                mSubjectDialog.show();
            }
        });
    }

    private void updateLinkedSubject() {
        mSubjectText.setText(mSubject.getName());
        mSubjectText.setTextColor(ContextCompat.getColor(
                ClassEditActivity.this, R.color.mdu_text_black));

        Color color = new Color(mSubject.getColorId());
        UiUtils.setBarColors(color, this, mAppBarLayout, mToolbar, mTabLayout);
    }

    private void setupDateTexts() {
        mStartDateText = (TextView) findViewById(R.id.textView_start_date);
        mEndDateText = (TextView) findViewById(R.id.textView_end_date);

        if (!mIsNew && mClass.hasStartEndDates()) {
            mStartDate = mClass.getStartDate();
            mEndDate = mClass.getEndDate();
            updateDateTexts();
        }

        mStartDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // note: -1 and +1s in code because Android month values are from 0-11 (to
                // correspond with java.util.Calendar) but LocalDate month values are from 1-12

                DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        mStartDate = LocalDate.of(year, month + 1, dayOfMonth);
                        updateDateTexts();
                    }
                };

                boolean useNowTime = mIsNew || !mClass.hasStartEndDates();

                new DatePickerDialog(
                        ClassEditActivity.this,
                        listener,
                        useNowTime ? LocalDate.now().getYear() : mStartDate.getYear(),
                        useNowTime ? LocalDate.now().getMonthValue() - 1 : mStartDate.getMonthValue() - 1,
                        useNowTime ? LocalDate.now().getDayOfMonth() : mStartDate.getDayOfMonth()
                ).show();
            }
        });

        mEndDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        mEndDate = LocalDate.of(year, month + 1, dayOfMonth);
                        updateDateTexts();
                    }
                };

                boolean useNowTime = mIsNew || !mClass.hasStartEndDates();

                new DatePickerDialog(
                        ClassEditActivity.this,
                        listener,
                        useNowTime ? LocalDate.now().getYear() : mEndDate.getYear(),
                        useNowTime ? LocalDate.now().getMonthValue() - 1 : mEndDate.getMonthValue() - 1,
                        useNowTime ? LocalDate.now().getDayOfMonth() : mEndDate.getDayOfMonth()
                ).show();
            }
        });
    }

    private void updateDateTexts() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        if (mStartDate != null) {
            mStartDateText.setText(mStartDate.format(formatter));
            mStartDateText.setTextColor(ContextCompat.getColor(
                    getBaseContext(), R.color.mdu_text_black));
        }
        if (mEndDate != null) {
            mEndDateText.setText(mEndDate.format(formatter));
            mEndDateText.setTextColor(ContextCompat.getColor(
                    getBaseContext(), R.color.mdu_text_black));
        }
    }

    private void setupDateSwitch() {
        Switch datesSwitch = (Switch) findViewById(R.id.dates_switch);
        final View datesSection = findViewById(R.id.dates_section);

        datesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    datesSection.setVisibility(View.VISIBLE);
                    if (!mIsNew && mClass.hasStartEndDates()) {
                        mStartDate = mClass.getStartDate();
                        mEndDate = mClass.getEndDate();
                    } else {
                        LocalDate today = LocalDate.now();
                        mStartDate = today;
                        mEndDate = today.plusMonths(1);
                    }
                    updateDateTexts();
                } else {
                    datesSection.setVisibility(View.GONE);
                    mStartDate = null;
                    mEndDate = null;
                }
            }
        });

        if (!mIsNew && mClass.hasStartEndDates()) {
            datesSwitch.setChecked(true);
        }
    }

    private void setupExpandToggle() {
        final View detailSection = findViewById(R.id.linearLayout_details);

        View expandToggle = findViewById(R.id.expand_toggle);
        final ImageView expandIcon = (ImageView) findViewById(R.id.expand_icon);

        expandToggle.setOnClickListener(new View.OnClickListener() {
            boolean mIsExpanded = false;

            @Override
            public void onClick(View v) {
                int drawableResId;
                int sectionVisibility;

                if (mIsExpanded) {
                    // We should condense the detail section
                    drawableResId = R.drawable.ic_expand_more_black_24dp;
                    sectionVisibility = View.GONE;
                } else {
                    // We should expand the detail section
                    drawableResId = R.drawable.ic_expand_less_black_24dp;
                    sectionVisibility = View.VISIBLE;
                }

                detailSection.setVisibility(sectionVisibility);
                expandIcon.setImageResource(drawableResId);

                mIsExpanded = !mIsExpanded;
            }
        });
    }

    private void addDetailTab(ClassDetail classDetail, boolean placeHolder) {
        boolean isNewDetail = classDetail == null;

        // MUST be final so that it doesn't change as adapter count updates
        final int pagerCount = mPagerAdapter.getCount();

        final int classDetailId = isNewDetail ?
                ClassUtils.getHighestClassDetailId(this) + mNewDetailIdCount :
                classDetail.getId();
        mClassDetailIds.add(classDetailId);

        View page = getLayoutInflater().inflate(R.layout.fragment_class_edit, null);

        EditText room = (EditText) page.findViewById(R.id.editText_room);
        if (!isNewDetail) {
            room.setText(classDetail.getRoom());
        }

        final EditText building = (EditText) page.findViewById(R.id.editText_building);
        if (!isNewDetail) {
            building.setText(classDetail.getBuilding());
        }

        EditText teacher = (EditText) page.findViewById(R.id.editText_teacher);
        if (!isNewDetail) {
            teacher.setText(classDetail.getTeacher());
        }

        ArrayList<ClassTime> classTimes = isNewDetail ? new ArrayList<ClassTime>() :
                ClassUtils.getClassTimesForDetail(this, classDetail.getId());
        final ArrayList<ClassTimeGroup> classTimeGroups = sortAndGroupTimes(classTimes);
        mAllClassTimeGroups.add(classTimeGroups);

        ClassTimesAdapter adapter = new ClassTimesAdapter(this, classTimeGroups);
        adapter.setOnEntryClickListener(new ClassTimesAdapter.OnEntryClickListener() {
            @Override
            public void onEntryClick(View view, int position) {
                ClassTimeGroup classTimeGroup = classTimeGroups.get(position);

                Intent intent = new Intent(ClassEditActivity.this, ClassTimeEditActivity.class);
                intent.putExtra(ClassTimeEditActivity.EXTRA_CLASS_TIME, classTimeGroup.getClassTimes());
                intent.putExtra(ClassTimeEditActivity.EXTRA_CLASS_DETAIL_ID, classDetailId);
                intent.putExtra(ClassTimeEditActivity.EXTRA_TAB_POSITION, pagerCount);

                Bundle bundle = null;
                if (UiUtils.isApi21()) {
                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ClassEditActivity.this,
                                    view,
                                    getString(R.string.transition_2));
                    bundle = options.toBundle();
                }

                ActivityCompat.startActivityForResult(
                        ClassEditActivity.this, intent, REQUEST_CODE_CLASS_TIME_DETAIL, bundle);
            }
        });
        mAdapters.add(adapter);

        RecyclerView recyclerView = (RecyclerView) page.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        recyclerView.setAdapter(adapter);

        Button btnAddTime = (Button) page.findViewById(R.id.button_add_time);
        btnAddTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ClassEditActivity.this, ClassTimeEditActivity.class);
                intent.putExtra(ClassTimeEditActivity.EXTRA_CLASS_DETAIL_ID, classDetailId);
                intent.putExtra(ClassTimeEditActivity.EXTRA_TAB_POSITION, pagerCount);

                Bundle bundle = null;
                if (UiUtils.isApi21()) {
                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ClassEditActivity.this,
                                    view,
                                    getString(R.string.transition_2));
                    bundle = options.toBundle();
                }

                ActivityCompat.startActivityForResult(
                        ClassEditActivity.this, intent, REQUEST_CODE_CLASS_TIME_DETAIL, bundle);
            }
        });

        if (placeHolder) {
            final ViewGroup content = (ViewGroup) page.findViewById(R.id.content);
            content.setVisibility(View.GONE);

            final Button btnAddTab = (Button) page.findViewById(R.id.button_add_tab);
            btnAddTab.setVisibility(View.VISIBLE);
            btnAddTab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    content.setVisibility(View.VISIBLE);
                    btnAddTab.setVisibility(View.GONE);
                    addDetailTab(null, true);
                }
            });
        }

        mPagerAdapter.addViewWithTitle(page, "Detail " + (pagerCount + 1));

        if (isNewDetail) {
            mNewDetailIdCount++;
        }
    }

    private ArrayList<ClassTimeGroup> sortAndGroupTimes(ArrayList<ClassTime> classTimes) {
        if (classTimes.isEmpty()) {
            return new ArrayList<>();
        }

        Collections.sort(classTimes, new Comparator<ClassTime>() {
            @Override
            public int compare(ClassTime ct1, ClassTime ct2) {
                // sort by start time, then end time, then days, then week numbers
                int startTimeCompare = ct1.getStartTime().compareTo(ct2.getStartTime());

                if (startTimeCompare == 0) {
                    // start times are equal, so compare end times
                    int endTimeCompare = ct1.getEndTime().compareTo(ct2.getEndTime());

                    if (endTimeCompare == 0) {
                        // end times are equal, so compare days
                        int dayCompare = ct1.getDay().compareTo(ct2.getDay());

                        if (dayCompare == 0) {
                            // days are equal, so compare week numbers
                            return ct1.getWeekNumber() - ct2.getWeekNumber();
                        } else {
                            return dayCompare;
                        }
                    } else {
                        return endTimeCompare;
                    }
                } else {
                    return startTimeCompare;
                }
            }
        });

        ArrayList<ClassTimeGroup> classTimeGroups = new ArrayList<>();

        ClassTimeGroup currentGroup = null;

        for (ClassTime classTime : classTimes) {
            if (currentGroup == null) {
                currentGroup = new ClassTimeGroup(classTime.getStartTime(), classTime.getEndTime());
                currentGroup.addClassTime(classTime);
                continue;
            }

            if (currentGroup.canAdd(classTime)) {
                currentGroup.addClassTime(classTime);
            } else {
                classTimeGroups.add(currentGroup); // add what has been produced up to this point
                currentGroup = new ClassTimeGroup(classTime.getStartTime(), classTime.getEndTime());
                currentGroup.addClassTime(classTime);
            }
        }
        classTimeGroups.add(currentGroup); // add what has was made in the last round of iterations

        return classTimeGroups;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SUBJECT_DETAIL) {
            if (resultCode == Activity.RESULT_OK) {
                mSubject = data.getParcelableExtra(SubjectEditActivity.EXTRA_SUBJECT);
                mSubjectDialog.dismiss();
                updateLinkedSubject();
            }

        } else if (requestCode == REQUEST_CODE_CLASS_TIME_DETAIL) {
            if (resultCode == Activity.RESULT_OK) {
                int tabIndex = data.getIntExtra(ClassTimeEditActivity.EXTRA_TAB_POSITION, -1);

                Log.i(LOG_TAG, "Reloading class times list for tab index " + tabIndex);

                ArrayList<ClassTimeGroup> thisTabTimeGroups = mAllClassTimeGroups.get(tabIndex);

                ArrayList<ClassTime> classTimes = ClassUtils.getClassTimesForDetail(
                        this, mClassDetailIds.get(tabIndex));

                thisTabTimeGroups.clear();
                thisTabTimeGroups.addAll(sortAndGroupTimes(classTimes));

                mAdapters.get(tabIndex).notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_item_edit, menu);
        UiUtils.tintMenuIcons(this, menu, R.id.action_done);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mIsNew) {
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
    }

    private void handleCloseAction() {
        setResult(Activity.RESULT_CANCELED);
        supportFinishAfterTransition();
    }

    private void handleDoneAction() {
        // Validate subject and start/end dates
        if (mSubject == null) {
            Snackbar.make(findViewById(R.id.rootView), R.string.message_subject_required,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }
        if ((mStartDate == null && mEndDate != null)
                || (mStartDate != null && mEndDate == null)) {
            Snackbar.make(findViewById(R.id.rootView), R.string.message_both_dates_required,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (mStartDate != null && mStartDate.isAfter(mEndDate)) {
            Snackbar.make(findViewById(R.id.rootView), R.string.message_start_date_after_end_date,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        String moduleName = TextUtilsKt.title(mEditTextModule.getText().toString());

        // go through each page and only collect data first - so any validation
        // errors can be resolved without any data being written or saved

        ArrayList<View> pages = mPagerAdapter.getAllViews();

        ArrayList<Integer> classDetailIds = new ArrayList<>();
        ArrayList<String> rooms = new ArrayList<>();
        ArrayList<String> buildings = new ArrayList<>();
        ArrayList<String> teachers = new ArrayList<>();

        // note the - 1 from .size() to exclude the placeholder tab
        for (int i = 0; i < pages.size() - 1; i++) {
            Log.d(LOG_TAG, "Collecting values at tab index " + i);

            View page = pages.get(i);

            int classDetailId = mClassDetailIds.get(i);

            EditText roomText = (EditText) page.findViewById(R.id.editText_room);
            String room = TextUtilsKt.title(roomText.getText().toString());
            Log.d(LOG_TAG, "room: " + room);

            EditText buildingText = (EditText) page.findViewById(R.id.editText_building);
            String building = TextUtilsKt.title(buildingText.getText().toString());
            Log.d(LOG_TAG, "building: " + building);

            EditText teacherText = (EditText) page.findViewById(R.id.editText_teacher);
            String teacher = TextUtilsKt.title(teacherText.getText().toString());
            Log.d(LOG_TAG, "teacher: " + teacher);

            ArrayList<ClassTimeGroup> classTimeGroups = mAllClassTimeGroups.get(i);
            if (classTimeGroups.isEmpty()) {
                Log.d(LOG_TAG, "class times list is empty!");
                if (room.trim().equals("") && teacher.trim().equals("")) {
                    // this is an empty detail page: room, teacher and times are empty
                    Snackbar.make(findViewById(R.id.rootView),
                            R.string.message_empty_detail, Snackbar.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "completely empty detail page");
                    return;
                } else {
                    // this has a room or teacher but not times (which it needs)
                    Snackbar.make(findViewById(R.id.rootView),
                            R.string.message_missing_time_for_detail, Snackbar.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "room and/or teacher, but no times");
                    return;
                }
            }

            classDetailIds.add(classDetailId);
            rooms.add(room);
            buildings.add(building);
            teachers.add(teacher);
        }

        if (rooms.size() == 0) {
            // if nothing has been added
            Log.d(LOG_TAG, "nothing entered");
            handleCloseAction();
            return;
        }

        // now write the data (replace class detail values)

        int classId = mIsNew ? ClassUtils.getHighestClassId(this) + 1 : mClass.getId();

        for (int i = 0; i < rooms.size(); i++) {
            int classDetailId = classDetailIds.get(i);
            String room = rooms.get(i);
            String building = buildings.get(i);
            String teacher = teachers.get(i);

            ClassDetail classDetail =
                    new ClassDetail(classDetailId, classId, room, building, teacher);

            ClassUtils.replaceClassDetail(this, classDetailId, classDetail);
        }

        Timetable timetable = ((TimetableApplication) getApplication()).getCurrentTimetable();
        assert timetable != null;

        LocalDate dbStartDate = mStartDate;
        LocalDate dbEndDate = mEndDate;
        if (mStartDate == null) {
            dbStartDate = Class.NO_DATE;
        }
        if (mEndDate == null) {
            dbEndDate = Class.NO_DATE;
        }

        mClass = new Class(classId,
                timetable.getId(),
                mSubject.getId(),
                moduleName,
                dbStartDate,
                dbEndDate);

        if (mIsNew) {
            ClassUtils.addClass(this, mClass);
        } else {
            ClassUtils.replaceClass(this, mClass.getId(), mClass);
        }

        setResult(Activity.RESULT_OK);
        supportFinishAfterTransition();
    }

    private void handleDeleteAction() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_class)
                .setMessage(R.string.delete_confirmation_class)
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClassUtils.completelyDeleteClass(getBaseContext(), mClass);
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

}
