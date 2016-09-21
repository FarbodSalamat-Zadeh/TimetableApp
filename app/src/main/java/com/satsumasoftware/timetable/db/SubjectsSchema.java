package com.satsumasoftware.timetable.db;

import android.provider.BaseColumns;

import com.satsumasoftware.timetable.db.util.SchemaUtilsKt;

public final class SubjectsSchema implements BaseColumns {

    public static final String TABLE_NAME = "subjects";
    public static final String COL_TIMETABLE_ID = "timetable_id";
    public static final String COL_NAME = "name";
    public static final String COL_ABBREVIATION = "abbreviation";
    public static final String COL_COLOR_ID = "color_id";

    static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + "( " +
            _ID + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.PRIMARY_KEY_AUTOINCREMENT + SchemaUtilsKt.COMMA_SEP +
            COL_TIMETABLE_ID + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_NAME + SchemaUtilsKt.TEXT_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_ABBREVIATION + SchemaUtilsKt.TEXT_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_COLOR_ID + SchemaUtilsKt.INTEGER_TYPE +
            " )";

}
