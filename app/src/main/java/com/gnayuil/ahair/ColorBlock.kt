package com.gnayuil.ahair

import android.graphics.Color

class ColorBlock {
    var color: Int
    var rgb: String

    constructor(rgb: String) {
        this.color = Color.parseColor(rgb)
        this.rgb = rgb
    }
}