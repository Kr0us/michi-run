package com.kr0us.michirun.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.kr0us.michirun.game.CatBreed

class CatPreviewView @JvmOverloads constructor(context: Context, var breed: CatBreed = CatBreed.SIAMESE, private val scene: Boolean = false) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scene) {
            canvas.save()
            canvas.scale(width / 900f, height / 400f)
            Scenery.draw(canvas, paint, 900f, 400f, 325f, 0f, false)
            paint.color = 0xFFE5DED0.toInt()
            canvas.drawOval(307f, 315f, 527f, 337f, paint)
            CatPainter.draw(canvas, paint, breed, 320f, 325f, 7.5f)
            canvas.restore()
        } else {
            val scale = minOf(width / 42f, height / 30f)
            CatPainter.draw(canvas, paint, breed, (width - 29 * scale) / 2, height * 0.92f, scale)
        }
    }
}
