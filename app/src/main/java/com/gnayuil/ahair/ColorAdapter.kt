package com.gnayuil.ahair

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class ColorAdapter(
    var context: Context,
    var colorList: ArrayList<ColorBlock>,
    var listener: (Int) -> Unit
) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder: ColorViewHolder
        val itemView: View

        if (convertView == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            itemView = inflater.inflate(R.layout.color_entry, parent, false)
            viewHolder = ColorViewHolder(itemView)
            itemView.tag = viewHolder
        } else {
            itemView = convertView
            viewHolder = itemView.tag as ColorViewHolder
        }

        val imageDrawable: Drawable
        if (position < colorList.size) {
            imageDrawable = ColorDrawable(colorList[position].color)
            viewHolder.colorView.elevation = dpToPx(5F, context)
        } else {
            imageDrawable = context.getDrawable(R.mipmap.color_more)!!
            viewHolder.colorView.elevation = 0F
        }
        viewHolder.colorView.setImageDrawable(imageDrawable)
        viewHolder.colorView.setOnClickListener { listener(position) }

        return itemView
    }

    private fun dpToPx(dp: Float, context: Context): Float {
        return dp * (context.resources
            .displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }

    override fun getItem(position: Int): ColorBlock {
        return colorList[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return colorList.size + 1
    }

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        internal val colorView = itemView.findViewById<ImageView>(R.id.imgColor)
    }

}