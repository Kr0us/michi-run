package com.kr0us.michirun.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.kr0us.michirun.R
import com.kr0us.michirun.game.CatBreed
import com.kr0us.michirun.game.Obstacle
import com.kr0us.michirun.game.ObstacleKind
import com.kr0us.michirun.game.RunState
import com.kr0us.michirun.game.RunnerEngine
import kotlin.math.min

class RunnerView @JvmOverloads constructor(context: Context, val engine: RunnerEngine = RunnerEngine(), private val breed: CatBreed = CatBreed.SIAMESE) : View(context) {
    var onProgress: (() -> Unit)? = null
    var onGameOver: (() -> Unit)? = null
    var onJump: (() -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastFrame = 0L
    private var lastScore = -1
    private var reportedGameOver = false

    init {
        contentDescription = context.getString(R.string.game_field)
        isClickable = true
        isFocusable = true
    }

    fun jump() {
        if (engine.jump()) onJump?.invoke()
        invalidate()
    }

    fun refresh() { lastFrame = 0L; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        if (lastFrame != 0L) engine.update((now - lastFrame) / 1_000_000_000f)
        lastFrame = now
        val zoom = min(width / RunnerEngine.WORLD_WIDTH, height / RunnerEngine.WORLD_HEIGHT)
        canvas.drawColor(if (engine.night) 0xFF242F32.toInt() else Scenery.paper)
        canvas.save()
        canvas.translate((width - RunnerEngine.WORLD_WIDTH * zoom) / 2, (height - RunnerEngine.WORLD_HEIGHT * zoom) / 2)
        canvas.scale(zoom, zoom)
        Scenery.draw(canvas, paint, 900f, 520f, RunnerEngine.GROUND, engine.distance, engine.night)
        paint.color = if (engine.night) 0xFF1D2729.toInt() else 0xFFE5DED0.toInt()
        canvas.drawOval(130f, 417f, 218f, 432f, paint)
        CatPainter.draw(canvas, paint, breed, RunnerEngine.CAT_X, RunnerEngine.GROUND - engine.elevation,
            RunnerEngine.CAT_WIDTH / 29f, (engine.distance / 45).toInt(), engine.ducking && engine.elevation == 0f)
        for (obstacle in engine.obstacles) drawObstacle(canvas, obstacle)
        if (engine.state == RunState.READY) {
            paint.color = Scenery.ink
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            paint.textSize = 38f
            canvas.drawText(context.getString(R.string.ready), 450f, 180f, paint)
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            paint.textSize = 25f
            canvas.drawText(context.getString(R.string.ready_hint), 450f, 226f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
        canvas.restore()
        if (lastScore != engine.score) {
            lastScore = engine.score
            onProgress?.invoke()
        }
        if (engine.state == RunState.GAME_OVER && !reportedGameOver) {
            reportedGameOver = true
            post { onGameOver?.invoke() }
        }
        if (engine.state == RunState.RUNNING) postInvalidateOnAnimation()
        else lastFrame = 0L
    }

    private fun drawObstacle(canvas: Canvas, obstacle: Obstacle) {
        val x = obstacle.x
        val y = obstacle.bottom - obstacle.height
        paint.color = if (engine.night) 0xFFAAB89B.toInt() else 0xFF69765D.toInt()
        if (obstacle.kind == ObstacleKind.CACTUS) {
            val center = x + obstacle.width / 2
            canvas.drawRect(center - 8, y, center + 8, obstacle.bottom, paint)
            canvas.drawRect(x, y + 18, x + 9, y + 40, paint)
            canvas.drawRect(x, y + 34, center, y + 43, paint)
            canvas.drawRect(x + obstacle.width - 9, y + 10, x + obstacle.width, y + 31, paint)
            canvas.drawRect(center, y + 25, x + obstacle.width, y + 34, paint)
            paint.color = if (engine.night) 0xFF71846B.toInt() else 0xFF99A084.toInt()
            canvas.drawRect(center - 3, y + 7, center, obstacle.bottom - 6, paint)
        } else {
            paint.color = if (engine.night) 0xFFDDCFB7.toInt() else Scenery.ink
            canvas.drawRect(x + 13, y + 15, x + 47, y + 29, paint)
            canvas.drawRect(x + 44, y + 9, x + 58, y + 22, paint)
            canvas.drawRect(x + 56, y + 16, x + 64, y + 21, paint)
            canvas.drawRect(x, y + 12, x + 19, y + 20, paint)
            val wingY = if ((engine.elapsed * 8).toInt() % 2 == 0) y else y + 23
            canvas.drawRect(x + 20, wingY, x + 33, wingY + 12, paint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        jump()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { jump(); return true }
            MotionEvent.ACTION_UP -> { engine.releaseJump(); super.performClick(); return true }
            MotionEvent.ACTION_CANCEL -> { engine.releaseJump(); return true }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP -> { if (event.repeatCount == 0) jump(); true }
        KeyEvent.KEYCODE_DPAD_DOWN -> { engine.duck(true); true }
        else -> super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_UP -> { engine.releaseJump(); true }
        KeyEvent.KEYCODE_DPAD_DOWN -> { engine.duck(false); true }
        else -> super.onKeyUp(keyCode, event)
    }
}
