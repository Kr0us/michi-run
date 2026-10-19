package com.kr0us.michirun

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Build
import android.os.LocaleList
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.kr0us.michirun.game.*
import com.kr0us.michirun.ui.*
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("michi", MODE_PRIVATE) }
    private var breed = CatBreed.SIAMESE
    private var engine: RunnerEngine? = null
    private var runner: RunnerView? = null
    private var scoreLabel: TextView? = null
    private var bestLabel: TextView? = null
    private var overlay: FrameLayout? = null
    private var tone: ToneGenerator? = null
    private var recordAtStart = 0
    private val cards = mutableMapOf<CatBreed, LinearLayout>()
    private var hero: CatPreviewView? = null
    private var trait: TextView? = null
    private val best: Int get() = prefs.getInt("best", 0)
    private val sound: Boolean get() = prefs.getBoolean("sound", true)

    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences("michi", MODE_PRIVATE).getString("language", "es") ?: "es"
        val config = Configuration(newBase.resources.configuration)
        if (language != "system") config.setLocales(LocaleList(Locale.forLanguageTag(language)))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (engine != null) pauseOrHome() else finish()
            }
        }
        breed = CatBreed.fromId(prefs.getString("breed", null))
        if (savedInstanceState?.getBoolean("in_game") == true) {
            val restored = RunnerEngine()
            val flat = savedInstanceState.getFloatArray("obstacles") ?: floatArrayOf()
            val obstacles = flat.toList().chunked(5).filter { it.size == 5 }.map {
                Obstacle(it[0], it[1], it[2], it[3], ObstacleKind.entries[it[4].toInt()])
            }
            restored.restore(RunSnapshot(
                RunState.valueOf(savedInstanceState.getString("run_state") ?: "READY"),
                savedInstanceState.getFloat("distance"), savedInstanceState.getFloat("elapsed"),
                savedInstanceState.getFloat("elevation"), savedInstanceState.getFloat("velocity"),
                savedInstanceState.getFloat("spawn_in"), obstacles))
            recordAtStart = savedInstanceState.getInt("record_at_start", best)
            showGame(restored)
        } else showHome()
    }

    @Suppress("DEPRECATION") // Compatibilidad con los insets de Android 8 a 10.
    private fun base(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Scenery.paper)
        }
        // API 35 obliga a dibujar hasta los bordes; los controles respetan barras y recortes.
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            } else if (Build.VERSION.SDK_INT >= 28) {
                val cutout = insets.displayCutout
                v.setPadding(maxOf(insets.systemWindowInsetLeft, cutout?.safeInsetLeft ?: 0),
                    maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0),
                    maxOf(insets.systemWindowInsetRight, cutout?.safeInsetRight ?: 0),
                    maxOf(insets.systemWindowInsetBottom, cutout?.safeInsetBottom ?: 0))
            } else v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        setContentView(root)
        root.requestApplyInsets()
        return root
    }

    private fun showHome() {
        saveRecord()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        engine = null
        runner = null
        overlay = null
        scoreLabel = null
        bestLabel = null
        cards.clear()
        val root = base()
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        root.addView(scroll, LinearLayout.LayoutParams(-1, -1))
        val content = column().apply { setPadding(dp(24), dp(12), dp(24), dp(22)) }
        scroll.addView(content, FrameLayout.LayoutParams(-1, -2))

        val header = row()
        header.addView(label(getString(R.string.edition), 10f, true, Scenery.muted).apply { letterSpacing = 0.12f },
            LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button(getString(R.string.settings_symbol), false) { showSettings() }.apply {
            contentDescription = getString(R.string.settings)
            textSize = 22f
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        content.addView(header)
        content.addView(label(getString(R.string.app_name), 48f, true).apply { letterSpacing = -0.05f }, margins(top = 12))
        content.addView(label(getString(R.string.tagline), 15f, color = Scenery.muted), margins(top = 2))

        val scene = FrameLayout(this).apply { background = rounded(0xFFF0ECDF.toInt(), 18f); clipToOutline = true }
        hero = CatPreviewView(this, breed, true)
        scene.addView(hero, FrameLayout.LayoutParams(-1, -1))
        val badge = label(getString(R.string.offline_badge), 9f, true, Scenery.muted).apply {
            letterSpacing = 0.1f; setPadding(dp(12), dp(10), 0, 0)
        }
        scene.addView(badge, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        content.addView(scene, margins(height = 150, top = 20))
        val recordRow = row().apply { setPadding(0, dp(12), 0, 0) }
        trait = label(getString(breed.traitRes), 12f, color = Scenery.muted)
        recordRow.addView(trait, LinearLayout.LayoutParams(0, -2, 1f))
        recordRow.addView(label("${getString(R.string.best)}  ${number(best)}", 11f, true).apply { typeface = Typeface.MONOSPACE })
        content.addView(recordRow)
        content.addView(label(getString(R.string.choose_cat), 22f, true), margins(top = 20))
        content.addView(label(getString(R.string.choose_hint), 12f, color = Scenery.muted), margins(top = 5))

        for (pair in CatBreed.entries.chunked(3)) {
            val breedRow = row()
            for ((index, cat) in pair.withIndex()) {
                val card = column().apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(5), dp(10), dp(5), dp(8))
                    isClickable = true; isFocusable = true
                    setOnClickListener { selectBreed(cat) }
                }
                card.addView(CatPreviewView(this, cat), LinearLayout.LayoutParams(dp(65), dp(46)))
                card.addView(label(getString(cat.titleRes), 12f, true).apply { gravity = Gravity.CENTER }, margins(top = 7))
                val params = LinearLayout.LayoutParams(0, -2, 1f).apply { if (index > 0) marginStart = dp(9) }
                breedRow.addView(card, params)
                cards[cat] = card
            }
            content.addView(breedRow, margins(top = 10))
        }
        selectBreed(breed)
        content.addView(button(getString(R.string.play), true) { recordAtStart = best; showGame(RunnerEngine()) }, margins(height = 58, top = 22))
        content.addView(label(getString(R.string.instructions_short), 11f, color = Scenery.muted).apply { gravity = Gravity.CENTER }, margins(top = 12))
        content.addView(label(getString(R.string.footer), 9f, color = Scenery.muted).apply { gravity = Gravity.CENTER; letterSpacing = 0.12f }, margins(top = 26))
    }

    private fun selectBreed(selected: CatBreed) {
        breed = selected
        prefs.edit().putString("breed", breed.name).apply()
        cards.forEach { (cat, card) ->
            val active = cat == breed
            card.background = ripple(rounded(if (active) 0xFFE8EDDF.toInt() else 0xFFF1EDE4.toInt(), 12f,
                if (active) Scenery.ink else 0xFFE1DDCF.toInt(), if (active) 2 else 1))
            card.isSelected = active
            card.contentDescription = getString(if (active) R.string.selected_cat else R.string.select_cat, getString(cat.titleRes))
        }
        hero?.breed = breed
        hero?.invalidate()
        trait?.text = getString(breed.traitRes)
    }

    private fun showGame(run: RunnerEngine) {
        engine = run
        hero = null
        trait = null
        val root = base()
        val compact = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val header = row().apply { setPadding(dp(16), dp(if (compact) 0 else 10), dp(16), dp(if (compact) 0 else 10)) }
        header.addView(button(getString(R.string.back_symbol), false) { pauseOrHome() }.apply {
            contentDescription = getString(R.string.home_description)
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val title = column().apply { gravity = Gravity.CENTER }
        title.addView(label(getString(R.string.app_name), 20f, true))
        title.addView(label(getString(breed.titleRes), 11f, color = Scenery.muted))
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button(getString(R.string.pause_symbol), false) {
            if (run.state == RunState.RUNNING) { run.pause(); setAwake(false); showOverlay() }
        }.apply { contentDescription = getString(R.string.pause) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)
        val scores = row().apply { setPadding(dp(28), dp(if (compact) 0 else 12), dp(28), 0) }
        val current = column()
        current.addView(label(getString(R.string.score), 10f, true, Scenery.muted).apply { letterSpacing = 0.16f })
        scoreLabel = label(number(run.score), if (compact) 22f else 36f, true).apply { typeface = Typeface.MONOSPACE }
        current.addView(scoreLabel)
        scores.addView(current, LinearLayout.LayoutParams(0, -2, 1f))
        val high = column().apply { gravity = Gravity.END }
        high.addView(label(getString(R.string.best), 10f, true, Scenery.muted).apply { letterSpacing = 0.16f })
        bestLabel = label(number(best), if (compact) 20f else 28f, true, Scenery.muted).apply { typeface = Typeface.MONOSPACE }
        high.addView(bestLabel)
        scores.addView(high)
        root.addView(scores)

        val field = FrameLayout(this)
        val view = RunnerView(this, run, breed)
        runner = view
        field.addView(view, FrameLayout.LayoutParams(-1, -1))
        overlay = FrameLayout(this).apply { visibility = View.GONE }
        field.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        root.addView(field, LinearLayout.LayoutParams(-1, 0, 1f))
        view.onJump = { setAwake(true); beep(ToneGenerator.TONE_PROP_BEEP, 65) }
        view.onProgress = {
            scoreLabel?.text = number(run.score)
            bestLabel?.text = number(maxOf(best, run.score))
            if (run.score > 0 && run.score % 100 == 0) beep(ToneGenerator.TONE_PROP_ACK, 100)
        }
        view.onGameOver = {
            if (engine === run) {
                saveRecord()
                setAwake(false)
                beep(ToneGenerator.TONE_PROP_NACK, 180)
                showOverlay()
            }
        }
        if (!compact) root.addView(label(getString(R.string.game_hint), 12f, color = Scenery.muted).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(12))
        })
        val controls = row().apply { setPadding(dp(24), 0, dp(24), dp(if (compact) 4 else 22)) }
        val duckButton = button(getString(R.string.duck), false) {}
        duckButton.onHold = { pressed -> run.duck(pressed) }
        // Un clic de accesibilidad mantiene la postura hasta el siguiente clic.
        duckButton.action = { run.duck(!run.ducking) }
        val jumpButton = button(getString(R.string.jump), true) { view.jump() }
        jumpButton.onHold = { pressed -> if (pressed) view.jump() else run.releaseJump() }
        controls.addView(duckButton, LinearLayout.LayoutParams(0, dp(if (compact) 48 else 62), 1f))
        controls.addView(jumpButton, LinearLayout.LayoutParams(0, dp(if (compact) 48 else 62), 1f).apply { marginStart = dp(12) })
        root.addView(controls)
        if (run.state == RunState.PAUSED || run.state == RunState.GAME_OVER) showOverlay()
    }

    private fun showOverlay() {
        val run = engine ?: return
        val container = overlay ?: return
        val over = run.state == RunState.GAME_OVER
        container.removeAllViews()
        container.visibility = View.VISIBLE
        container.setBackgroundColor(0xEBF8F5EE.toInt())
        container.isClickable = true
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val panel = column().apply { gravity = Gravity.CENTER; setPadding(dp(24), dp(12), dp(24), dp(12)) }
        scroll.addView(panel, FrameLayout.LayoutParams(-1, -2))
        container.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        panel.addView(label(getString(if (over) R.string.game_over else R.string.paused), 25f, true).apply { gravity = Gravity.CENTER })
        panel.addView(label(getString(if (over) R.string.game_over_hint else R.string.paused_hint), 12f, color = Scenery.muted).apply { gravity = Gravity.CENTER }, margins(top = 8))
        if (over && run.score > recordAtStart) panel.addView(label(getString(R.string.new_best), 12f, true, Scenery.accent), margins(top = 12))
        panel.addView(button(getString(if (over) R.string.retry else R.string.resume), true) {
            if (over) { recordAtStart = best; showGame(RunnerEngine()) }
            else { container.visibility = View.GONE; run.resume(); setAwake(true); runner?.refresh() }
        }, margins(height = 54, top = 20))
        panel.addView(button(getString(R.string.home), false) { showHome() }, margins(height = 48, top = 8))
        panel.announceForAccessibility(getString(if (over) R.string.game_over else R.string.paused) + " " +
            getString(R.string.score_announcement, run.score, best))
    }

    private fun showSettings() {
        val panel = column().apply { setPadding(dp(24), dp(12), dp(24), dp(8)) }
        val soundSwitch = Switch(this).apply {
            text = getString(R.string.sound); setTextColor(Scenery.ink); textSize = 16f
            isChecked = sound; minHeight = dp(56)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("sound", checked).apply() }
        }
        panel.addView(soundSwitch)
        val dialog = AlertDialog.Builder(this).setTitle(R.string.settings).setView(panel).setPositiveButton(R.string.close, null).create()
        panel.addView(button(getString(R.string.language), false) { dialog.dismiss(); chooseLanguage() }, margins(height = 52, top = 8))
        panel.addView(button(getString(R.string.how_to), false) {
            AlertDialog.Builder(this).setTitle(R.string.how_to).setMessage(R.string.how_to_body).setPositiveButton(R.string.close, null).show()
        }, margins(height = 52, top = 8))
        dialog.show()
    }

    private fun chooseLanguage() {
        val codes = arrayOf("es", "en", "system")
        val options = arrayOf(getString(R.string.language_es), getString(R.string.language_en), getString(R.string.language_system))
        val selected = codes.indexOf(prefs.getString("language", "es"))
        AlertDialog.Builder(this).setTitle(R.string.language).setSingleChoiceItems(options, selected) { dialog, which ->
            prefs.edit().putString("language", codes[which]).apply()
            dialog.dismiss()
            recreate()
        }.setNegativeButton(R.string.close, null).show()
    }

    private fun pauseOrHome() {
        val run = engine ?: return
        when (run.state) {
            RunState.RUNNING -> { run.pause(); setAwake(false); showOverlay() }
            RunState.PAUSED, RunState.READY, RunState.GAME_OVER -> showHome()
        }
    }

    @Deprecated("Back compatible con Android 8 y posteriores")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (engine != null) pauseOrHome() else super.onBackPressed()
    }

    override fun onPause() {
        engine?.let { if (it.state == RunState.RUNNING) { it.pause(); showOverlay() } }
        saveRecord()
        setAwake(false)
        tone?.release(); tone = null
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        engine?.snapshot()?.let { snapshot ->
            outState.putBoolean("in_game", true)
            outState.putString("run_state", snapshot.state.name)
            outState.putFloat("distance", snapshot.distance)
            outState.putFloat("elapsed", snapshot.elapsed)
            outState.putFloat("elevation", snapshot.elevation)
            outState.putFloat("velocity", snapshot.velocity)
            outState.putFloat("spawn_in", snapshot.spawnIn)
            outState.putInt("record_at_start", recordAtStart)
            outState.putFloatArray("obstacles", snapshot.obstacles.flatMap { listOf(it.x, it.width, it.height, it.bottom, it.kind.ordinal.toFloat()) }.toFloatArray())
        }
        super.onSaveInstanceState(outState)
    }

    private fun saveRecord() {
        val score = engine?.score ?: return
        if (score > best) prefs.edit().putInt("best", score).apply()
    }

    private fun beep(kind: Int, duration: Int) {
        if (!sound) return
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 35)
            tone?.startTone(kind, duration)
        } catch (_: RuntimeException) { /* El juego funciona también sin salida de audio. */ }
    }

    private fun setAwake(enabled: Boolean) {
        if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun label(value: String, size: Float, bold: Boolean = false, color: Int = Scenery.ink) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }

    private fun button(value: String, primary: Boolean, action: () -> Unit) = ActionButton(this).apply {
        text = value; textSize = 14f; isAllCaps = false
        setTextColor(if (primary) Scenery.paper else Scenery.ink)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        background = ripple(rounded(if (primary) Scenery.accent else 0xFFF0ECDF.toInt(), 12f))
        setPadding(dp(10), dp(8), dp(10), dp(8))
        minimumHeight = dp(48); minHeight = dp(48); minWidth = 0
        stateListAnimator = null
        this.action = action
    }

    private fun rounded(color: Int, radius: Float, border: Int? = null, width: Int = 1) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (border != null) setStroke(dp(width), border)
    }
    private fun ripple(background: GradientDrawable) = RippleDrawable(ColorStateList.valueOf(0x2230362F), background, null)
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun margins(height: Int = -2, top: Int = 0) = LinearLayout.LayoutParams(-1, if (height < 0) height else dp(height)).apply { topMargin = dp(top) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun dp(value: Float) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun number(value: Int) = String.format(Locale.ROOT, "%05d", value)
}
