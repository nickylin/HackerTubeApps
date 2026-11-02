package com.ninigogo.hackertube

import android.content.Context
import org.json.JSONArray

/** Local-only library: likes, watch later and history. Nothing leaves the device. */
class LibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    private fun read(key: String): List<Video> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try { Video.listFromJsonArray(JSONArray(raw)) } catch (e: Exception) { emptyList() }
    }

    private fun write(key: String, videos: List<Video>) {
        val arr = JSONArray()
        videos.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun liked(): List<Video> = read("liked")
    fun watchLater(): List<Video> = read("watch_later")
    fun history(): List<Video> = read("history")
    fun hiddenIds(): Set<String> = prefs.getStringSet("hidden", emptySet()) ?: emptySet()

    fun isLiked(vid: String) = liked().any { it.vid == vid }
    fun isWatchLater(vid: String) = watchLater().any { it.vid == vid }

    fun toggleLiked(video: Video) {
        val cur = liked()
        write("liked", if (cur.any { it.vid == video.vid }) cur.filter { it.vid != video.vid } else listOf(video) + cur)
    }

    fun toggleWatchLater(video: Video) {
        val cur = watchLater()
        write("watch_later", if (cur.any { it.vid == video.vid }) cur.filter { it.vid != video.vid } else listOf(video) + cur)
    }

    fun addHistory(video: Video) {
        val cur = history().filter { it.vid != video.vid }
        write("history", (listOf(video) + cur).take(100))
    }

    fun hide(vid: String) {
        prefs.edit().putStringSet("hidden", hiddenIds() + vid).apply()
    }
}
