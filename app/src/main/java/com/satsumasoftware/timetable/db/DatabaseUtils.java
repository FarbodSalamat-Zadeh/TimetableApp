package com.satsumasoftware.timetable.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.satsumasoftware.timetable.framework.Subject;

public final class DatabaseUtils {

    public static void addSubject(Context context, Subject subject) {
        ContentValues values = new ContentValues();
        values.put(SubjectsSchema.COL_ID, subject.getId());
        values.put(SubjectsSchema.COL_NAME, subject.getName());

        SQLiteDatabase db = TimetableDbHelper.getInstance(context).getWritableDatabase();
        db.insert(SubjectsSchema.TABLE_NAME, null, values);
    }

}
