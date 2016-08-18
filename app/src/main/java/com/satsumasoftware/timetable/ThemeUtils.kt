package com.satsumasoftware.timetable

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.annotation.MenuRes
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.satsumasoftware.timetable.framework.Color

fun setBarColors(color: Color, activity: Activity, vararg views: View) {
    for (view in views) {
        view.setBackgroundColor(ContextCompat.getColor(activity, color.getPrimaryColorResId(activity)))
    }
    if (Build.VERSION.SDK_INT >= 21) {
        activity.window.statusBarColor =
                ContextCompat.getColor(activity, color.getPrimaryDarkColorResId(activity))
    }
}

@JvmOverloads
fun tintDrawable(context: Context, @DrawableRes drawableRes: Int, @ColorRes colorRes: Int = R.color.mdu_white): Drawable {
    return tintDrawable(context, ContextCompat.getDrawable(context, drawableRes), colorRes)
}

@JvmOverloads
fun tintDrawable(context: Context, d: Drawable, @ColorRes colorRes: Int = R.color.mdu_white): Drawable {
    val drawable = DrawableCompat.wrap(d)
    DrawableCompat.setTint(drawable, ContextCompat.getColor(context, colorRes))
    return drawable
}

fun tintMenuIcons(context: Context, menu: Menu, vararg @IdRes menuItems: Int) {
    for (@IdRes menuItem in menuItems) {
        val icon = menu.findItem(menuItem).icon
        icon?.let { tintDrawable(context, icon) }
    }
}
