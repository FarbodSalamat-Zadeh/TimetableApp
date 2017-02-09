package com.satsumasoftware.timetable.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.satsumasoftware.timetable.db.schema.ExamsSchema
import com.satsumasoftware.timetable.framework.Exam
import com.satsumasoftware.timetable.receiver.AlarmReceiver
import com.satsumasoftware.timetable.util.DateUtils

class ExamUtils : TimetableItemUtils<Exam> {

    override val tableName = ExamsSchema.TABLE_NAME

    override val itemIdCol = ExamsSchema._ID

    override val timetableIdCol = ExamsSchema.COL_TIMETABLE_ID

    override fun createFromCursor(cursor: Cursor) = Exam.from(cursor)

    override fun propertiesAsContentValues(item: Exam): ContentValues {
        val values = ContentValues()
        with(values) {
            put(ExamsSchema._ID, item.id)
            put(ExamsSchema.COL_TIMETABLE_ID, item.timetableId)
            put(ExamsSchema.COL_SUBJECT_ID, item.subjectId)
            put(ExamsSchema.COL_MODULE, item.moduleName)
            put(ExamsSchema.COL_DATE_DAY_OF_MONTH, item.date.dayOfMonth)
            put(ExamsSchema.COL_DATE_MONTH, item.date.monthValue)
            put(ExamsSchema.COL_DATE_YEAR, item.date.year)
            put(ExamsSchema.COL_START_TIME_HRS, item.startTime.hour)
            put(ExamsSchema.COL_START_TIME_MINS, item.startTime.minute)
            put(ExamsSchema.COL_DURATION, item.duration)
            put(ExamsSchema.COL_SEAT, item.seat)
            put(ExamsSchema.COL_ROOM, item.room)
            put(ExamsSchema.COL_IS_RESIT, if (item.resit) 1 else 0)
        }
        return values
    }

    override fun deleteItem(context: Context, itemId: Int) {
        super.deleteItem(context, itemId)

        AlarmReceiver().cancelAlarm(context, AlarmReceiver.Type.EXAM, itemId)
    }

    companion object {
        @JvmStatic
        fun addAlarmForExam(context: Context, exam: Exam) {
            val remindDate = exam.makeDateTimeObject().minusMinutes(30)
            AlarmReceiver().setAlarm(context,
                    AlarmReceiver.Type.EXAM,
                    DateUtils.asCalendar(remindDate),
                    exam.id)
        }
    }

}
