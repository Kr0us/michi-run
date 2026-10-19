package com.kr0us.michirun.ui

import android.graphics.Canvas
import android.graphics.Paint

object Scenery {
    val paper = 0xFFF8F5EE.toInt()
    val ink = 0xFF30362F.toInt()
    val muted = 0xFF74796B.toInt()
    val accent = 0xFFB7402C.toInt()

    fun draw(canvas: Canvas, paint: Paint, width: Float, height: Float, ground: Float, distance: Float, night: Boolean) {
        canvas.drawColor(if (night) 0xFF242F32.toInt() else paper)
        paint.color = if (night) 0xFF526367.toInt() else 0xFFE5DFD1.toInt()
        for (i in 0..9) {
            val x = ((i * 137f - distance * 0.12f) % (width + 140) + width + 140) % (width + 140) - 70
            val roof = ground - 40 - (i % 4) * 18
            canvas.drawRect(x, roof, x + 76, ground, paint)
            canvas.drawRect(x + 18, roof - 13, x + 25, roof, paint)
        }
        paint.color = if (night) 0xFFE6D8A1.toInt() else 0xFFD9A26A.toInt()
        canvas.drawCircle(width * 0.81f, height * 0.24f, 27f, paint)
        if (night) {
            paint.color = 0xFF242F32.toInt()
            canvas.drawCircle(width * 0.81f - 13, height * 0.24f - 8, 24f, paint)
            paint.color = 0xFFA6B2AB.toInt()
            for (i in 0..14) {
                val x = (i * 179f + 21) % width
                val y = (i * 47f) % (ground * 0.5f) + 30
                canvas.drawRect(x, y, x + 3, y + 3, paint)
            }
        }
        paint.color = if (night) 0xFF6B7C7C.toInt() else 0xFFD6D2C7.toInt()
        for (i in 0..3) {
            val x = ((i * 299f + 72 - distance * 0.23f) % (width + 180) + width + 180) % (width + 180) - 90
            val y = 75f + (i % 3) * 45
            canvas.drawRect(x, y, x + 78, y + 3, paint)
            canvas.drawRect(x + 15, y - 13, x + 19, y, paint)
            canvas.drawRect(x + 19, y - 16, x + 47, y - 13, paint)
            canvas.drawRect(x + 47, y - 12, x + 52, y, paint)
        }
        paint.color = if (night) 0xFF9EAEA2.toInt() else 0xFF8B927E.toInt()
        canvas.drawRect(0f, ground, width, ground + 2, paint)
        paint.color = if (night) 0xFF526367.toInt() else 0xFFD4D0C2.toInt()
        for (i in 0..30) {
            val x = ((i * 61f - distance) % (width + 70) + width + 70) % (width + 70) - 35
            val y = ground + 13 + (i % 5) * 9
            canvas.drawRect(x, y, x + 3 + i % 7 * 2, y + 2, paint)
        }
    }
}
