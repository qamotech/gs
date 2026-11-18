package com.qamotech.gemstorm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * Music: two composed loops (menu / battle) cross-switched by game state.
 * SFX: SoundPool one-shots, all synthesized for GEMSTORM (see gen_audio.py).
 */
class AudioManager(private val context: Context, private val save: SaveData) {

    private var musicPlayer: MediaPlayer? = null
    private var currentTrack: Int = 0

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
    }

    /** track: R.raw.music_menu or R.raw.music_battle; 0 stops music. */
    fun playMusic(track: Int) {
        if (track == currentTrack && musicPlayer != null) return
        stopMusic()
        currentTrack = track
        if (track == 0) return
        musicPlayer = MediaPlayer.create(context, track)?.apply {
            isLooping = true
            setVolume(save.musicVol, save.musicVol)
            start()
        }
    }

    fun stopMusic() {
        musicPlayer?.let {
            try { if (it.isPlaying) it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        musicPlayer = null
        currentTrack = 0
    }

    fun applyMusicVolume() {
        musicPlayer?.setVolume(save.musicVol, save.musicVol)
    }

    fun sfx(name: String, rate: Float = 1.0f) {
        val id = ids[name] ?: return
        val vol = save.sfxVol
        if (vol <= 0.01f) return
        soundPool.play(id, vol, vol, 1, 0, rate)
    }

    fun release() {
        stopMusic()
        soundPool.release()
    }
}
