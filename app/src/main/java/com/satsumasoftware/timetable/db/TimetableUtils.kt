package com.satsumasoftware.timetable.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.satsumasoftware.timetable.TimetableApplication
import com.satsumasoftware.timetable.db.query.Filters
import com.satsumasoftware.timetable.db.query.Query
import com.satsumasoftware.timetable.db.schema.SubjectsSchema
import com.satsumasoftware.timetable.db.schema.TermsSchema
import com.satsumasoftware.timetable.db.schema.TimetablesSchema
import com.satsumasoftware.timetable.framework.Timetable

class TimetableUtils : DataUtils<Timetable> {

    override val tableName = TimetablesSchema.TABLE_NAME

    override val itemIdCol = TimetablesSchema._ID

    override fun createFromCursor(cursor: Cursor) = Timetable.from(cursor)

    override fun propertiesAsContentValues(item: Timetable): ContentValues {
        val values = ContentValues()
        with(values) {
            put(TimetablesSchema._ID, item.id)
            put(TimetablesSchema.COL_NAME, item.name)
            put(TimetablesSchema.COL_START_DATE_DAY_OF_MONTH, item.startDate.dayOfMonth)
            put(TimetablesSchema.COL_START_DATE_MONTH, item.startDate.monthValue)
            put(TimetablesSchema.COL_START_DATE_YEAR, item.startDate.year)
            put(TimetablesSchema.COL_END_DATE_DAY_OF_MONTH, item.endDate.dayOfMonth)
            put(TimetablesSchema.COL_END_DATE_MONTH, item.endDate.monthValue)
            put(TimetablesSchema.COL_END_DATE_YEAR, item.endDate.year)
            put(TimetablesSchema.COL_WEEK_ROTATIONS, item.weekRotations)
        }
        return values
    }

    override fun replaceItem(context: Context, oldItemId: Int, newItem: Timetable) {
        super.replaceItem(context, oldItemId, newItem)

        // Refresh alarms in case start/end dates have changed
        val application = context.applicationContext as TimetableApplication
        application.refreshAlarms(context)
    }

    override fun deleteItemWithReferences(context: Context, itemId: Int) {
        super.deleteItemWithReferences(context, itemId)

        // Note that we only need to delete subjects, terms and their references since classes,
        // assignments, exams, and everything else are linked to subjects.

        val subjectsQuery = Query.Builder()
                .addFilter(Filters.equal(SubjectsSchema.COL_TIMETABLE_ID, itemId.toString()))
                .build()

        for (subject in SubjectUtils().getAllItems(context, subjectsQuery)) {
            SubjectUtils().deleteItemWithReferences(context, subject.id)
        }

        val termsQuery = Query.Builder()
                .addFilter(Filters.equal(TermsSchema.COL_TIMETABLE_ID, itemId.toString()))
                .build()

        val termUtils = TermUtils()
        termUtils.getAllItems(context, termsQuery).forEach {
            termUtils.deleteItem(context, it.id)
        }
    }

}