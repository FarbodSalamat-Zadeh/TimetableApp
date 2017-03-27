package co.timetableapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import co.timetableapp.R;
import co.timetableapp.data.handler.ClassHandler;
import co.timetableapp.data.handler.TimetableItemHandler;
import co.timetableapp.framework.Class;
import co.timetableapp.framework.Subject;
import co.timetableapp.ui.adapter.ClassesAdapter;
import co.timetableapp.util.UiUtils;

/**
 * An activity for displaying a list of classes to the user.
 *
 * If there are no classes to display, a placeholder background will be shown instead.
 *
 * Clicking on a class will allow the user to view its details in {@link ClassDetailActivity}.
 * The user can also choose to create a new class in which case {@link ClassDetailActivity}
 * will also be invoked but with no intent extra data.
 *
 * @see Class
 * @see ClassDetailActivity
 * @see ClassEditActivity
 */
public class ClassesActivity extends ItemListActivity<Class> {

    private static final int REQUEST_CODE_CLASS_DETAIL = 1;

    private boolean mShowAll = false;

    @Override
    TimetableItemHandler<Class> instantiateDataHandler() {
        return new ClassHandler(this);
    }

    @Override
    void onFabButtonClick() {
        Intent intent = new Intent(ClassesActivity.this, ClassEditActivity.class);
        startActivityForResult(intent, REQUEST_CODE_CLASS_DETAIL);
    }

    @Override
    RecyclerView.Adapter setupAdapter() {
        ClassesAdapter adapter = new ClassesAdapter(this, mItems);
        adapter.setOnEntryClickListener(new ClassesAdapter.OnEntryClickListener() {
            @Override
            public void onEntryClick(View view, int position) {
                Intent intent = new Intent(ClassesActivity.this, ClassDetailActivity.class);
                intent.putExtra(ClassDetailActivity.EXTRA_ITEM, mItems.get(position));

                Bundle bundle = null;
                if (UiUtils.isApi21()) {
                    ActivityOptionsCompat options =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    ClassesActivity.this,
                                    view,
                                    getString(R.string.transition_1));
                    bundle = options.toBundle();
                }

                ActivityCompat.startActivityForResult(
                        ClassesActivity.this, intent, REQUEST_CODE_CLASS_DETAIL, bundle);
            }
        });

        return adapter;
    }

    @Override
    ArrayList<Class> getItems() {
        return ((ClassHandler) mDataHandler).getCurrentClasses(getApplication(), mShowAll);
    }

    @Override
    void sortList() {
        Collections.sort(mItems, new Comparator<Class>() {
            @Override
            public int compare(Class c1, Class c2) {
                Subject s1 = Subject.create(getBaseContext(), c1.getSubjectId());
                Subject s2 = Subject.create(getBaseContext(), c2.getSubjectId());
                assert s1 != null;
                assert s2 != null;
                return s1.getName().compareTo(s2.getName());
            }
        });
    }

    @Override
    View getPlaceholderView() {
        return UiUtils.makePlaceholderView(this,
                R.drawable.ic_class_black_24dp,
                R.string.placeholder_classes,
                R.color.mdu_blue_400,
                R.color.mdu_white,
                R.color.mdu_white,
                true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CLASS_DETAIL) {
            if (resultCode == RESULT_OK) {
                refreshList();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_classes, menu);
        UiUtils.tintMenuIcons(this, menu, R.id.action_manage_subjects);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_manage_subjects:
                startActivity(new Intent(this, SubjectsActivity.class));
                break;

            case R.id.action_show_all:
                if (mShowAll) {
                    item.setChecked(false);
                    mShowAll = false;
                } else {
                    item.setChecked(true);
                    mShowAll = true;
                }
                refreshList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_CLASSES;
    }

}