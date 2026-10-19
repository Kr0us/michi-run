package com.kr0us.michirun.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class RunState { READY, RUNNING, PAUSED, GAME_OVER }
enum class ObstacleKind { CACTUS, BIRD }

data class HitBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun overlaps(other: HitBox): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top
}

data class Obstacle(var x: Float, val width: Float, val height: Float, val bottom: Float, val kind: ObstacleKind) {
    fun hitBox() = HitBox(x + 5, bottom - height + 4, x + width - 5, bottom - 3)
}

data class RunSnapshot(val state: RunState, val distance: Float, val elapsed: Float, val elevation: Float,
    val velocity: Float, val spawnIn: Float, val obstacles: List<Obstacle>)

/** Motor sin dependencias Android; coordenadas y pasos fijos compartidos por todos los dispositivos. */
class RunnerEngine(private val random: Random = Random.Default) {
    companion object {
        const val WORLD_WIDTH = 900f
        const val WORLD_HEIGHT = 520f
        const val GROUND = 424f
        const val CAT_X = 132f
        const val CAT_WIDTH = 76f
        const val CAT_HEIGHT = 66f
        const val STEP = 1f / 120f
        const val GRAVITY = 1700f
        const val JUMP_SPEED = 730f
        const val MAX_SPEED = 640f
    }

    var state = RunState.READY
        private set
    var distance = 0f
        private set
    var elapsed = 0f
        private set
    var elevation = 0f
        private set
    var velocity = 0f
        private set
    var ducking = false
        private set
    val obstacles = mutableListOf<Obstacle>()
    val score: Int get() = (elapsed * 10).toInt()
    val speed: Float get() = min(MAX_SPEED, 320f + elapsed * 3.6f)
    val night: Boolean get() = (elapsed / 45).toInt() % 2 == 1
    private var accumulator = 0f
    private var spawnIn = 1.6f

    fun start() {
        state = RunState.RUNNING
        distance = 0f
        elapsed = 0f
        elevation = 0f
        velocity = 0f
        ducking = false
        obstacles.clear()
        accumulator = 0f
        spawnIn = 1.6f
    }

    fun jump(): Boolean {
        if (state == RunState.READY) start()
        if (state != RunState.RUNNING || elevation > 0f || velocity > 0f || ducking) return false
        velocity = JUMP_SPEED
        return true
    }

    fun releaseJump() {
        if (velocity > 520f) velocity = 520f
    }

    fun duck(pressed: Boolean) {
        ducking = pressed && state == RunState.RUNNING
    }

    fun pause() {
        if (state == RunState.RUNNING) {
            state = RunState.PAUSED
            ducking = false
            accumulator = 0f
        }
    }

    fun resume() {
        if (state == RunState.PAUSED) state = RunState.RUNNING
    }

    fun snapshot() = RunSnapshot(state, distance, elapsed, elevation, velocity, spawnIn, obstacles.map { it.copy() })

    fun restore(snapshot: RunSnapshot) {
        state = if (snapshot.state == RunState.RUNNING) RunState.PAUSED else snapshot.state
        distance = snapshot.distance
        elapsed = snapshot.elapsed
        elevation = snapshot.elevation
        velocity = snapshot.velocity
        spawnIn = snapshot.spawnIn
        ducking = false
        accumulator = 0f
        obstacles.clear()
        obstacles.addAll(snapshot.obstacles.map { it.copy() })
    }

    fun catHitBox(): HitBox {
        val height = if (ducking && elevation == 0f) 30f else CAT_HEIGHT
        return HitBox(CAT_X + 13, GROUND - elevation - height + 8, CAT_X + CAT_WIDTH - 6, GROUND - elevation - 4)
    }

    /** Limita interrupciones largas; una pausa del SO no teletransporta obstáculos. */
    fun update(seconds: Float) {
        if (state != RunState.RUNNING || !seconds.isFinite() || seconds <= 0f) return
        accumulator += min(seconds, 0.1f)
        while (accumulator + 0.000001f >= STEP && state == RunState.RUNNING) {
            accumulator = max(0f, accumulator - STEP)
            tick()
        }
    }

    private fun tick() {
        elapsed += STEP
        distance += speed * STEP
        if (elevation > 0f || velocity > 0f) {
            velocity -= GRAVITY * STEP * if (ducking && velocity < 0) 2.4f else 1f
            elevation = max(0f, elevation + velocity * STEP)
            if (elevation == 0f) velocity = 0f
        }
        for (obstacle in obstacles) obstacle.x -= speed * STEP
        obstacles.removeAll { it.x + it.width < -20 }
        spawnIn -= STEP
        if (spawnIn <= 0f) {
            spawn()
            // Un salto completo y un margen de reacción entre obstáculos, incluso a velocidad máxima.
            spawnIn = 1.15f + random.nextFloat() * 0.7f
        }
        if (obstacles.any { catHitBox().overlaps(it.hitBox()) }) {
            state = RunState.GAME_OVER
            ducking = false
        }
    }

    private fun spawn() {
        if (elapsed > 20 && random.nextFloat() < 0.3f) {
            val bottom = if (random.nextBoolean()) GROUND - 42 else GROUND - 100
            obstacles += Obstacle(WORLD_WIDTH + 20, 64f, 34f, bottom, ObstacleKind.BIRD)
        } else {
            val width = if (elapsed > 10 && random.nextBoolean()) 72f else 40f
            obstacles += Obstacle(WORLD_WIDTH + 20, width, 50f + random.nextFloat() * 20f, GROUND, ObstacleKind.CACTUS)
        }
    }
}
