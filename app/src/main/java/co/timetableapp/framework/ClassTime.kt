package co.timetableapp.framework

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import co.timetableapp.R
import co.timetableapp.TimetableApplication
import co.timetableapp.data.TimetableDbHelper
import co.timetableapp.data.schema.ClassTimesSchema
import co.timetableapp.util.PrefUtils
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalTime

/**
 * A collection of data relating to a time that the class takes place.
 *
 * Data about when the class takes place can be organized into `ClassTime` objects. Each of these
 * has properties for a day of the week, which weeks it takes place, and of course, start and end
 * times.
 *
 * So each `ClassTime` only includes data for one occurrence of the class (e.g. 12:00 to 13:00 on
 * Mondays on Week 2s).
 *
 * Note that a `ClassTime` is linked to a [ClassDetail] and not a [Class] so that we know the times
 * for each different class detail (e.g. when the student gets taught by teacher A and when they
 * get taught by teacher B).
 *
 * @property classDetailId the identifier of the associated [ClassDetail]
 * @property day a day of the week (Monday to Sunday) that the class takes place
 * @property weekNumber the number of a week rotation where the class takes place
 * @property startTime a start time of the class
 * @property endTime an end time of the class
 */
class ClassTime(override val id: Int, override val timetableId: Int, val classDetailId: Int,
                val day: DayOfWeek, val weekNumber: Int, val startTime: LocalTime,
                val endTime: LocalTime) : TimetableItem, Comparable<ClassTime> {

    companion object {

        /**
         * Constructs a [ClassTime] using column values from the cursor provided
         *
         * @param cursor a query of the class times table
         * @see [ClassTimesSchema]
         */
        @JvmStatic
        fun from(cursor: Cursor): ClassTime {
            val dayOfWeek =
                    DayOfWeek.of(cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_DAY)))

            val startTime = LocalTime.of(
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_START_TIME_HRS)),
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_START_TIME_MINS)))

            val endTime = LocalTime.of(
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_END_TIME_HRS)),
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_END_TIME_MINS)))

            return ClassTime(
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema._ID)),
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_TIMETABLE_ID)),
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_CLASS_DETAIL_ID)),
                    dayOfWeek,
                    cursor.getInt(cursor.getColumnIndex(ClassTimesSchema.COL_WEEK_NUMBER)),
                    startTime,
                    endTime)
        }

        @JvmStatic
        fun create(context: Context, classTimeId: Int): ClassTime {
            val db = TimetableDbHelper.getInstance(context).readableDatabase
            val cursor = db.query(
                    ClassTimesSchema.TABLE_NAME,
                    null,
                    "${ClassTimesSchema._ID}=?",
                    arrayOf(classTimeId.toString()),
                    null, null, null)
            cursor.moveToFirst()
            val classTime = ClassTime.from(cursor)
            cursor.close()
            return classTime
        }

        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<ClassTime> = object : Parcelable.Creator<ClassTime> {
            override fun createFromParcel(source: Parcel): ClassTime = ClassTime(source)
            override fun newArray(size: Int): Array<ClassTime?> = arrayOfNulls(size)
        }

        /**
         * @return the string to be displayed indicating the week rotation (e.g. Week 1, Week C).
         */
        @JvmOverloads
        @JvmStatic
        fun getWeekText(activity: Activity, weekNumber: Int, fullText: Boolean = true): String {
            val timetable = (activity.application as TimetableApplication).currentTimetable!!
            if (timetable.hasFixedScheduling()) {
                return ""
            } else {
                val weekChar = if (PrefUtils.isWeekRotationShownWithNumbers(activity)) {
                    weekNumber.toString()
                } else {
                    when(weekNumber) {
                        1 -> "A"
                        2 -> "B"
                        3 -> "C"
                        4 -> "D"
                        else -> throw IllegalArgumentException("invalid week number '$weekNumber'")
                    }
                }
                return if (fullText) activity.getString(R.string.week_item, weekChar) else weekChar
            }
        }
    }

    /**
     * @return the string to be displayed indicating the week rotation (e.g. Week 1, Week C).
     *
     * @see Companion.getWeekText
     */
    fun getWeekText(activity: Activity) = Companion.getWeekText(activity, weekNumber)

    override fun compareTo(other: ClassTime): Int {
        // Sort by day, then by time
        val dayComparison = day.compareTo(other.day)
        return if (dayComparison != 0) {
            dayComparison
        } else {
            startTime.compareTo(other.startTime)
        }
    }

    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt(),
            source.readInt(),
            source.readSerializable() as DayOfWeek,
            source.readInt(),
            source.readSerializable() as LocalTime,
            source.readSerializable() as LocalTime)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeInt(timetableId)
        dest?.writeInt(classDetailId)
        dest?.writeSerializable(day)
        dest?.writeInt(weekNumber)
        dest?.writeSerializable(startTime)
        dest?.writeSerializable(endTime)
    }

}