package com.kr0us.michirun.game

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class RunnerEngineTest {
    private fun runFor(engine: RunnerEngine, seconds: Float) {
        repeat((seconds * 120).toInt()) { engine.update(RunnerEngine.STEP) }
    }

    @Test fun `una partida no avanza antes de empezar`() {
        val engine = RunnerEngine(Random(7))
        runFor(engine, 3f)
        assertEquals(RunState.READY, engine.state)
        assertEquals(0, engine.score)
        assertEquals(0f, engine.distance, 0f)
    }

    @Test fun `tocar empieza y no permite un segundo salto en el aire`() {
        val engine = RunnerEngine()
        assertTrue(engine.jump())
        runFor(engine, 0.2f)
        assertTrue(engine.elevation > 0)
        assertFalse(engine.jump())
        runFor(engine, 0.8f)
        assertEquals(0f, engine.elevation, 0.01f)
        assertTrue(engine.jump())
    }

    @Test fun `soltar produce un salto mas bajo que mantener`() {
        val held = RunnerEngine()
        val tapped = RunnerEngine()
        held.jump(); tapped.jump(); tapped.releaseJump()
        var heldPeak = 0f
        var tappedPeak = 0f
        repeat(120) {
            held.update(RunnerEngine.STEP); tapped.update(RunnerEngine.STEP)
            heldPeak = maxOf(heldPeak, held.elevation)
            tappedPeak = maxOf(tappedPeak, tapped.elevation)
        }
        assertTrue(heldPeak > 140)
        assertTrue(tappedPeak in 70f..90f)
    }

    @Test fun `una colision detiene la partida y el puntaje`() {
        val engine = RunnerEngine()
        engine.start()
        engine.obstacles += Obstacle(RunnerEngine.CAT_X, 50f, 60f, RunnerEngine.GROUND, ObstacleKind.CACTUS)
        engine.update(RunnerEngine.STEP)
        assertEquals(RunState.GAME_OVER, engine.state)
        val distance = engine.distance
        runFor(engine, 2f)
        assertEquals(distance, engine.distance, 0f)
        assertFalse(engine.jump())
    }

    @Test fun `un salto despeja un cactus`() {
        val engine = RunnerEngine()
        engine.jump()
        runFor(engine, 0.3f)
        engine.obstacles += Obstacle(RunnerEngine.CAT_X + 20, 40f, 60f, RunnerEngine.GROUND, ObstacleKind.CACTUS)
        runFor(engine, 0.4f)
        assertEquals(RunState.RUNNING, engine.state)
    }

    @Test fun `agacharse evita un pajaro bajo pero estar de pie no`() {
        fun collide(duck: Boolean): RunState {
            val engine = RunnerEngine()
            engine.start(); engine.duck(duck)
            engine.obstacles += Obstacle(RunnerEngine.CAT_X, 64f, 34f, RunnerEngine.GROUND - 42, ObstacleKind.BIRD)
            engine.update(RunnerEngine.STEP)
            return engine.state
        }
        assertEquals(RunState.GAME_OVER, collide(false))
        assertEquals(RunState.RUNNING, collide(true))
    }

    @Test fun `agacharse no evita un cactus y soltar restaura la altura`() {
        val engine = RunnerEngine()
        engine.start()
        val fullHeight = engine.catHitBox()
        engine.duck(true)
        assertTrue(engine.catHitBox().top > fullHeight.top)
        assertFalse(engine.jump())
        engine.duck(false)
        assertEquals(fullHeight, engine.catHitBox())
        engine.duck(true)
        engine.obstacles += Obstacle(RunnerEngine.CAT_X, 40f, 60f, RunnerEngine.GROUND, ObstacleKind.CACTUS)
        engine.update(RunnerEngine.STEP)
        assertEquals(RunState.GAME_OVER, engine.state)
    }

    @Test fun `la pausa congela la fisica y continuar la recupera`() {
        val engine = RunnerEngine()
        engine.jump(); runFor(engine, 0.2f); engine.duck(true); engine.pause()
        val snapshot = engine.snapshot()
        runFor(engine, 2f)
        assertEquals(snapshot, engine.snapshot())
        assertFalse(engine.ducking)
        engine.resume(); runFor(engine, 0.1f)
        assertTrue(engine.distance > snapshot.distance)
    }

    @Test fun `la fisica es igual a 30 y 120 cuadros por segundo`() {
        val slow = RunnerEngine(Random(9))
        val fast = RunnerEngine(Random(9))
        slow.jump(); fast.jump()
        repeat(36) { slow.update(1f / 30) }
        repeat(144) { fast.update(1f / 120) }
        assertEquals(fast.distance, slow.distance, 0.01f)
        assertEquals(fast.elevation, slow.elevation, 0.01f)
        assertEquals(fast.score, slow.score)
    }

    @Test fun `restaurar tras recrear conserva la partida en pausa sin compartir obstaculos`() {
        val original = RunnerEngine()
        original.start(); runFor(original, 0.5f)
        original.obstacles += Obstacle(600f, 40f, 60f, RunnerEngine.GROUND, ObstacleKind.CACTUS)
        val restored = RunnerEngine()
        restored.restore(original.snapshot())
        assertEquals(RunState.PAUSED, restored.state)
        assertEquals(original.distance, restored.distance, 0f)
        assertEquals(original.score, restored.score)
        original.obstacles[0].x = 5f
        assertEquals(600f, restored.obstacles[0].x, 0f)
        restored.resume(); runFor(restored, 0.1f)
        assertTrue(restored.obstacles[0].x < 600f)
    }

    @Test fun `reiniciar limpia el estado anterior`() {
        val engine = RunnerEngine()
        engine.jump(); runFor(engine, 0.2f)
        engine.obstacles += Obstacle(400f, 40f, 60f, RunnerEngine.GROUND, ObstacleKind.CACTUS)
        engine.start()
        assertEquals(0, engine.score)
        assertEquals(0f, engine.elevation, 0f)
        assertEquals(0f, engine.distance, 0f)
        assertTrue(engine.obstacles.isEmpty())
        assertFalse(engine.ducking)
    }

    @Test fun `saltos de tiempo del sistema no teletransportan el mundo`() {
        val engine = RunnerEngine()
        engine.start()
        engine.update(Float.NaN); engine.update(Float.POSITIVE_INFINITY); engine.update(-1f)
        assertEquals(0f, engine.elapsed, 0f)
        engine.update(20f)
        assertTrue(engine.elapsed <= 0.101f)
    }

    @Test fun `la velocidad maxima y el ciclo nocturno se conservan en partidas largas`() {
        val engine = RunnerEngine()
        engine.restore(RunSnapshot(RunState.PAUSED, 50000f, 140f, 0f, 0f, 1.5f, emptyList()))
        assertEquals(RunnerEngine.MAX_SPEED, engine.speed, 0f)
        assertTrue(engine.night)
        engine.restore(RunSnapshot(RunState.PAUSED, 50000f, 180f, 0f, 0f, 1.5f, emptyList()))
        assertFalse(engine.night)
    }
}
