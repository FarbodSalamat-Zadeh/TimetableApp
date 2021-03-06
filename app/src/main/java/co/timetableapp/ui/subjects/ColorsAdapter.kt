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

package co.timetableapp.ui.subjects

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import co.timetableapp.R
import co.timetableapp.model.Color
import co.timetableapp.ui.OnItemClick
import de.hdodenhof.circleimageview.CircleImageView

/**
 * A RecyclerView adapter for displaying a list of colors.
 */
class ColorsAdapter(
        private val context: Context,
        private val colors: List<Color>
) : RecyclerView.Adapter<ColorsAdapter.ColorViewHolder>() {

    private var onItemClick: OnItemClick? = null

    fun onItemClick(action: OnItemClick) {
        onItemClick = action
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ColorViewHolder {
        val itemView = LayoutInflater.from(parent!!.context)
                .inflate(R.layout.item_color, parent, false)
        return ColorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ColorViewHolder?, position: Int) {
        val color = colors[position]
        holder!!.imageView.setImageResource(color.getPrimaryColorResId(context))
    }

    override fun getItemCount() = colors.size

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView = itemView.findViewById(R.id.imageView) as CircleImageView

        init {
            itemView.setOnClickListener { onItemClick?.invoke(it, layoutPosition) }
        }

    }

}
