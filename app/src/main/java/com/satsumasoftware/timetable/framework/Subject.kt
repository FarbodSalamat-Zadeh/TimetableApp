package com.satsumasoftware.timetable.framework

import android.content.Context
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import com.satsumasoftware.timetable.db.SubjectsSchema
import com.satsumasoftware.timetable.db.TimetableDbHelper

/**
 * An object representing a subject the student is studying.
 *
 * @property id an integer identifier for this subject
 * @property timetableId the identifier of the [Timetable] this subject is in
 * @property name the name of the subject (e.g. Mathematics, Computer Science, Music)
 * @property abbreviation an optional abbreviation for the subject (e.g. Ma, CS, Mus)
 * @property colorId the identifier of the [Color] used when displaying this subject
 */
class Subject(val id: Int, val timetableId: Int, var name: String, var abbreviation: String,
              var colorId: Int) : Parcelable {

    companion object {

        /**
         * Constructs a [Subject] using column values from the cursor provided
         *
         * @param cursor a query of the subjects table
         * @see [SubjectsSchema]
         */
        @JvmStatic
        fun from(cursor: Cursor): Subject {
            return Subject(cursor.getInt(cursor.getColumnIndex(SubjectsSchema._ID)),
                    cursor.getInt(cursor.getColumnIndex(SubjectsSchema.COL_TIMETABLE_ID)),
                    cursor.getString(cursor.getColumnIndex(SubjectsSchema.COL_NAME)),
                    cursor.getString(cursor.getColumnIndex(SubjectsSchema.COL_ABBREVIATION)),
                    cursor.getInt(cursor.getColumnIndex(SubjectsSchema.COL_COLOR_ID)))
        }

        @JvmStatic
        fun create(context: Context, subjectId: Int): Subject? {
            val db = TimetableDbHelper.getInstance(context).readableDatabase
            val cursor = db.query(
                    SubjectsSchema.TABLE_NAME,
                    null,
                    "${SubjectsSchema._ID}=?",
                    arrayOf(subjectId.toString()),
                    null, null, null)
            cursor.moveToFirst()
            if (cursor.count == 0) {
                cursor.close()
                return null
            }
            val subject = Subject.from(cursor)
            cursor.close()
            return subject
        }

        @JvmField val CREATOR: Parcelable.Creator<Subject> = object : Parcelable.Creator<Subject> {
            override fun createFromParcel(source: Parcel): Subject = Subject(source)
            override fun newArray(size: Int): Array<Subject?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
            source.readInt(),
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readInt())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeInt(timetableId)
        dest?.writeString(name)
        dest?.writeString(abbreviation)
        dest?.writeInt(colorId)
    }

}
