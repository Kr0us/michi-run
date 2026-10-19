package com.kr0us.michirun.ui

import android.graphics.Canvas
import android.graphics.Paint
import com.kr0us.michirun.game.CatBreed

/** Sprite original dibujado con rectángulos, sin imágenes ni descargas. */
object CatPainter {
    private val ink = 0xFF30362F.toInt()
    private val pink = 0xFFD58F86.toInt()

    fun draw(canvas: Canvas, paint: Paint, breed: CatBreed, x: Float, feet: Float, scale: Float, frame: Int = 0, duck: Boolean = false) {
        canvas.save()
        canvas.translate(x, feet - 24 * scale * if (duck) 0.58f else 1f)
        canvas.scale(scale, scale * if (duck) 0.58f else 1f)
        paint.isAntiAlias = false
        fun block(l: Int, t: Int, r: Int, b: Int, color: Int) {
            paint.color = color
            canvas.drawRect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat(), paint)
        }
        // Cola, cuerpo, cabeza y orejas, con silueta continua.
        block(0, 8, 3, 16, ink)
        block(2, 13, 7, 19, ink)
        block(5, 11, 22, 22, ink)
        block(15, 4, 27, 16, ink)
        block(15, 0, 19, 6, ink)
        block(23, 0, 27, 6, ink)
        block(1, 9, 2, 15, breed.fur)
        block(3, 14, 7, 17, breed.fur)
        block(6, 12, 21, 21, breed.fur)
        block(16, 5, 26, 15, breed.fur)
        block(16, 1, 18, 6, breed.fur)
        block(24, 1, 26, 6, breed.fur)
        block(17, 2, 18, 5, pink)
        block(24, 2, 25, 5, pink)
        when (breed) {
            CatBreed.SIAMESE -> {
                block(18, 6, 26, 14, breed.markings)
                block(0, 8, 3, 12, breed.markings)
                block(16, 1, 18, 4, breed.markings)
                block(24, 1, 26, 4, breed.markings)
            }
            CatBreed.GRAY, CatBreed.TABBY -> {
                block(9, 12, 11, 17, breed.markings)
                block(14, 12, 16, 16, breed.markings)
                block(18, 5, 20, 8, breed.markings)
                block(22, 5, 24, 8, breed.markings)
                block(1, 10, 3, 12, breed.markings)
            }
            CatBreed.PERSIAN -> {
                block(4, 14, 7, 20, breed.fur)
                block(7, 10, 17, 13, breed.fur)
                block(14, 8, 17, 16, breed.fur)
                block(18, 13, 25, 16, breed.markings)
            }
            CatBreed.TUXEDO -> {
                block(18, 11, 26, 15, breed.markings)
                block(17, 15, 21, 21, breed.markings)
                block(22, 6, 24, 12, breed.markings)
            }
            CatBreed.CALICO -> {
                block(6, 12, 11, 17, breed.markings)
                block(14, 14, 18, 20, ink)
                block(16, 5, 20, 9, breed.markings)
                block(24, 4, 26, 8, ink)
                block(0, 8, 3, 12, ink)
            }
        }
        val stride = frame % 2
        block(7 + stride * 2, 20, 11 + stride * 2, 24, ink)
        block(18 - stride * 2, 20, 22 - stride * 2, 24, ink)
        block(8 + stride * 2, 21, 10 + stride * 2, 23, breed.markings)
        block(19 - stride * 2, 21, 21 - stride * 2, 23, breed.markings)
        block(19, 8, 21, 11, ink)
        block(24, 8, 26, 11, ink)
        block(20, 8, 21, 10, breed.eyes)
        block(25, 8, 26, 10, breed.eyes)
        block(22, 11, 24, 12, pink)
        block(22, 13, 25, 14, ink)
        block(26, 11, 29, 12, ink)
        paint.isAntiAlias = true
        canvas.restore()
    }
}
