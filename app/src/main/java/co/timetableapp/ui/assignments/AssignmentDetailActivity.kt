/*
 * Copyright 2017 Farbod Salamat-Zadeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.timetableapp.ui.assignments

import android.app.Activity
import android.content.Intent
import android.support.v4.app.ActivityCompat
import android.support.v7.widget.Toolbar
import android.widget.SeekBar
import android.widget.TextView
import co.timetableapp.R
import co.timetableapp.data.handler.AssignmentHandler
import co.timetableapp.model.Assignment
import co.timetableapp.model.Class
import co.timetableapp.model.Color
import co.timetableapp.model.Subject
import co.timetableapp.ui.base.ItemDetailActivity
import co.timetableapp.util.DateUtils
import co.timetableapp.util.UiUtils

/**
 * Shows the details of an assignment.
 *
 * @see Assignment
 * @see AssignmentEditActivity
 * @see ItemDetailActivity
 */
class AssignmentDetailActivity : ItemDetailActivity<Assignment>() {

    override fun initializeDataHandler() = AssignmentHandler(this)

    override fun getLayoutResource() = R.layout.activity_assignment_detail

    override fun onNullExtras() {
        val intent = Intent(this, AssignmentEditActivity::class.java)
        ActivityCompat.startActivityForResult(this, intent, REQUEST_CODE_ITEM_EDIT, null)
    }

    override fun setupLayout() {
        setupToolbar()

        val dateFormatter = DateUtils.FORMATTER_FULL_DATE
        (findViewById(R.id.textView_date) as TextView).text = mItem.dueDate.format(dateFormatter)

        val progressText = findViewById(R.id.textView_progress) as TextView
        progressText.text = getString(R.string.property_progress, mItem.completionProgress)

        val seekBar = findViewById(R.id.seekBar) as SeekBar
        with(seekBar) {
            max = 20 // so it goes up in 5s
            progress = mItem.completionProgress / 5

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    mItem.completionProgress = progress * 5
                    progressText.text =
                            getString(R.string.property_progress, mItem.completionProgress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }

        val detailText = findViewById(R.id.textView_detail) as TextView
        UiUtils.formatNotesTextView(this, detailText, mItem.detail)
    }

    private fun setupToolbar() {
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        toolbar.navigationIcon = UiUtils.tintDrawable(this, R.drawable.ic_arrow_back_black_24dp)
        toolbar.setNavigationOnClickListener { saveEditsAndClose() }

        val cls = Class.create(this, mItem.classId)
        val subject = Subject.create(this, cls.subjectId)

        val textViewTitle = findViewById(R.id.title) as TextView
        textViewTitle.text = mItem.title

        val textViewSubtitle = findViewById(R.id.subtitle) as TextView
        textViewSubtitle.text = subject.name

        val color = Color(subject.colorId)
        UiUtils.setBarColors(color, this, toolbar)
    }

    override fun onMenuEditClick() {
        val intent = Intent(this, AssignmentEditActivity::class.java)
        intent.putExtra(AssignmentEditActivity.EXTRA_ASSIGNMENT, mItem)
        ActivityCompat.startActivityForResult(this, intent, REQUEST_CODE_ITEM_EDIT, null)
    }

    override fun cancelAndClose() {
        setResult(Activity.RESULT_CANCELED)
        supportFinishAfterTransition()
    }

    override fun saveEditsAndClose() {
        // Overwrite db values as completionProgress may have changed
        mDataHandler.replaceItem(mItem.id, mItem)

        val intent = Intent().putExtra(EXTRA_ITEM, mItem)

        setResult(Activity.RESULT_OK, intent) // to reload any changes in AssignmentsActivity
        supportFinishAfterTransition()
    }

    override fun saveDeleteAndClose() {
        setResult(Activity.RESULT_OK) // to reload any changes in AssignmentsActivity
        finish()
    }

}
