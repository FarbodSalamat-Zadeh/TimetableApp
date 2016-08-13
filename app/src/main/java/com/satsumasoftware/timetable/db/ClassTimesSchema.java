package com.satsumasoftware.timetable.db;

import android.provider.BaseColumns;

public final class ClassTimesSchema implements BaseColumns {

    public static final String TABLE_NAME = "class_times";
    public static final String COL_DAY = "day";
    public static final String COL_START_TIME_HRS = "start_time_hrs";
    public static final String COL_START_TIME_MINS = "start_time_mins";
    public static final String COL_END_TIME_HRS = "end_time_hrs";
    public static final String COL_END_TIME_MINS = "end_time_mins";

    protected static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + "( " +
            _ID + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.PRIMARY_KEY_AUTOINCREMENT + SchemaUtilsKt.COMMA_SEP +
            COL_DAY + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_START_TIME_HRS + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_START_TIME_MINS + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_END_TIME_HRS + SchemaUtilsKt.INTEGER_TYPE + SchemaUtilsKt.COMMA_SEP +
            COL_END_TIME_MINS + SchemaUtilsKt.INTEGER_TYPE +
            " )";

    protected static final String SQL_DELETE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

}