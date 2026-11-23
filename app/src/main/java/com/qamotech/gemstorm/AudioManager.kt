package com.qamotech.gemstorm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.ArrayDeque
import kotlin.math.min

/**
 * Music: composed loops (menu / battle / boss / shop / endless) cross-switched by game state.
 * SFX: SoundPool one-shots, all synthesized for GEMSTORM (see gen_audio.py).
 * Mixing: distance volume (#281), a simple master limiter (#282), music ducking
 * under boss roars (#283), audio-focus pause/resume (#293).
 */
class AudioManager(private val context: Context, private val save: SaveData) {

    private var musicPlayer: MediaPlayer? = null
    private var currentTrack: Int = 0
    private var focusPaused = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sysAudio: android.media.AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager

    private val focusListener = android.media.AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            android.media.AudioManager.AUDIOFOCUS_LOSS,
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                try { if (musicPlayer?.isPlaying == true) { musicPlayer?.pause(); focusPaused = true } } catch (_: Exception) { }
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                if (focusPaused && currentTrack != 0 && save.musicOn) {
                    try { musicPlayer?.start() } catch (_: IllegalStateException) { }
                    focusPaused = false
                }
            }
        }
    }

    private fun requestFocus() {
        val am = sysAudio ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(focusListener)
                    .build()
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(focusListener, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN)
            }
        } catch (_: Exception) { }
    }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<String, Int>()
    private val recentPlays = ArrayDeque<Long>()

    init {
        ids["shoot"] = soundPool.load(context, R.raw.sfx_shoot, 1)
        ids["hit"] = soundPool.load(context, R.raw.sfx_hit, 1)
        ids["explosion"] = soundPool.load(context, R.raw.sfx_explosion, 1)
        ids["pickup"] = soundPool.load(context, R.raw.sfx_pickup, 1)
        ids["levelup"] = soundPool.load(context, R.raw.sfx_levelup, 1)
        ids["dash"] = soundPool.load(context, R.raw.sfx_dash, 1)
        ids["hurt"] = soundPool.load(context, R.raw.sfx_hurt, 1)
        ids["click"] = soundPool.load(context, R.raw.sfx_click, 1)
        ids["victory"] = soundPool.load(context, R.raw.sfx_victory, 1)
        ids["gameover"] = soundPool.load(context, R.raw.sfx_gameover, 1)
        ids["boss"] = soundPool.load(context, R.raw.sfx_boss, 1)
        // Batch K sound library (#251-#274)
        ids["zap"] = soundPool.load(context, R.raw.sfx_zap, 1)
        ids["meteor"] = soundPool.load(context, R.raw.sfx_meteor, 1)
        ids["poison"] = soundPool.load(context, R.raw.sfx_poison, 1)
        ids["sizzle"] = soundPool.load(context, R.raw.sfx_sizzle, 1)
        ids["shieldhit"] = soundPool.load(context, R.raw.sfx_shieldhit, 1)
        ids["shieldbreak"] = soundPool.load(context, R.raw.sfx_shieldbreak, 1)
        ids["revive"] = soundPool.load(context, R.raw.sfx_revive, 1)
        ids["horn"] = soundPool.load(context, R.raw.sfx_horn, 1)
        ids["sting"] = soundPool.load(context, R.raw.sfx_sting, 1)
        ids["dice"] = soundPool.load(context, R.raw.sfx_dice, 1)
        ids["poof"] = soundPool.load(context, R.raw.sfx_poof, 1)
        ids["purchase"] = soundPool.load(context, R.raw.sfx_purchase, 1)
        ids["error"] = soundPool.load(context, R.raw.sfx_error, 1)
        ids["waveclear"] = soundPool.load(context, R.raw.sfx_waveclear, 1)
        ids["roar"] = soundPool.load(context, R.raw.sfx_roar, 1)
        ids["heart"] = soundPool.load(context, R.raw.sfx_heart, 1)
        ids["merge"] = soundPool.load(context, R.raw.sfx_merge, 1)
        ids["focus"] = soundPool.load(context, R.raw.sfx_focus, 1)
        ids["whoosh"] = soundPool.load(context, R.raw.sfx_whoosh, 1)
        ids["warp"] = soundPool.load(context, R.raw.sfx_warp, 1)
        ids["necropop"] = soundPool.load(context, R.raw.sfx_necropop, 1)
        ids["gnome"] = soundPool.load(context, R.raw.sfx_gnome, 1)
        ids["heartbeat"] = soundPool.load(context, R.raw.sfx_heartbeat, 1)
        // Batch L (#294-#298)
        ids["count"] = soundPool.load(context, R.raw.sfx_count, 1)
        ids["spawn"] = soundPool.load(context, R.raw.sfx_spawn, 1)
        ids["levelchord"] = soundPool.load(context, R.raw.sfx_levelchord, 1)
    }

    /** track: an R.raw music loop; 0 stops music. Honors the music toggle (#290) and mute-all (#292). */
    fun playMusic(track: Int) { // v2.7.2: the media state machine can never crash the game
        try { playMusicInner(track) } catch (_: Exception) { }
    }

    private fun playMusicInner(track: Int) {
        if (track != 0 && !save.musicOn) { stopMusic(); return }
        if (track == currentTrack && musicPlayer != null) return
        stopMusic()
        currentTrack = track
        if (track == 0) return
        requestFocus()
        musicPlayer = MediaPlayer.create(context, track)?.apply {
            isLooping = true
            val v = if (save.muteAll) 0f else save.musicVol
            setVolume(v, v)
            start()
        }
    }

    fun stopMusic() {
        musicPlayer?.let {
            try { if (it.isPlaying) it.stop() } catch (_: IllegalStateException) { }
            try { it.release() } catch (_: Exception) { }
        }
        musicPlayer = null
        currentTrack = 0
        focusPaused = false
    }

    fun applyMusicVolume() {
        try {
            if (save.muteAll) musicPlayer?.setVolume(0f, 0f)
            else musicPlayer?.setVolume(save.musicVol, save.musicVol)
        } catch (_: Exception) { }
    }

    /** #346 crossfade from the current track into a new one (menu -> battle on run start). */
    fun crossfadeTo(track: Int) {
        val old = musicPlayer
        if (old == null || currentTrack == track || !save.musicOn) { playMusic(track); return }
        val steps = 8
        for (s in 1..steps) {
            mainHandler.postDelayed({
                try { if (!save.muteAll) old.setVolume(save.musicVol * (1f - s.toFloat() / steps), save.musicVol * (1f - s.toFloat() / steps)) } catch (_: Exception) { }
                if (s == steps) { playMusic(track); fadeIn() }
            }, s * 55L)
        }
    }

    private fun fadeIn() {
        val mp = musicPlayer ?: return
        val steps = 8
        for (s in 1..steps) {
            mainHandler.postDelayed({
                try { mp.setVolume(if (save.muteAll) 0f else save.musicVol * (s.toFloat() / steps), if (save.muteAll) 0f else save.musicVol * (s.toFloat() / steps)) } catch (_: Exception) { }
            }, s * 55L)
        }
    }

    /** #292 mute-all: silences the live music player immediately; SFX gate lives in sfx(). */
    fun setMuted(m: Boolean) {
        try { if (m) musicPlayer?.setVolume(0f, 0f) else applyMusicVolume() } catch (_: Exception) { }
    }

    /** #283 duck the music to 70% briefly (boss roar). */
    private fun duckMusic() {
        val mp = musicPlayer ?: return
        if (save.muteAll || !save.musicOn) return
        try { mp.setVolume(save.musicVol * 0.7f, save.musicVol * 0.7f) } catch (_: Exception) { return }
        mainHandler.postDelayed({ applyMusicVolume() }, 750)
    }

    fun sfx(name: String, rate: Float = 1.0f, volMul: Float = 1.0f) {
        if (released) return // #848 v2.7: never play into a released SoundPool (throws on some devices)
        if (save.muteAll) return
        val id = ids[name] ?: return
        if (save.sfxVol <= 0.01f) return
        if (name == "roar") duckMusic()
        // #282 master limiter: the more one-shots stack inside 100 ms, the more each is pulled down
        val now = System.currentTimeMillis()
        while (recentPlays.isNotEmpty() && now - recentPlays.first() > 100) recentPlays.removeFirst()
        recentPlays.addLast(now)
        var vol = save.sfxVol * volMul
        val crowd = recentPlays.size
        if (crowd > 4) vol *= 1f - min(0.45f, (crowd - 4) * 0.06f)
        vol = min(vol, 0.9f)
        try { soundPool.play(id, vol, vol, 1, 0, rate) } catch (_: Exception) { } // #849 v2.7.4: SoundPool.play throws on some devices — a sound must never kill a run
    }

    private var released = false // #848 v2.7

    fun release() { // #848 v2.7: idempotent — a double onDestroy can never double-release the pool
        if (released) return
        released = true
        stopMusic()
        try { soundPool.release() } catch (_: Exception) { }
    }
}
