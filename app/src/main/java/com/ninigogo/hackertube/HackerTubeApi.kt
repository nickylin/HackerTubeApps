package com.ninigogo.hackertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Client for the public JSON API of hackertube.org — the same API the
 * website itself uses. No API key, no account, no tracking.
 *
 * Endpoints (verified live on 2026-11-02):
 *   GET /api/feed                 -> [Video]
 *   GET /api/list                 -> [Video]
 *   GET /api/search?q=<query>     -> { weak, results: [Video] }
 *   GET /api/video?vid=<id>       -> { video: Video }
 *   GET /api/related?vid=<id>     -> [Video] (shape tolerated)
 *   GET /api/browse               -> [Area]
 *   GET /api/playlists            -> [Playlist]
 *   GET /api/playlist?slug=<slug> -> [Video] or { videos: [Video] } (shape tolerated)
 */
object HackerTubeApi {
    private const val BASE = "https://hackertube.org"

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("User-Agent", "HackerTubeApps/0.1.0 (Android; open-source)")
        conn.setRequestProperty("Accept", "application/json")
        try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun feed(): List<Video> = Video.listFromJsonArray(JSONArray(get("/api/feed")))

    suspend fun list(): List<Video> = Video.listFromJsonArray(JSONArray(get("/api/list")))

    suspend fun search(query: String): List<Video> {
        val body = get("/api/search?q=${enc(query)}").trim()
        return if (body.startsWith("[")) {
            Video.listFromJsonArray(JSONArray(body))
        } else {
            val obj = JSONObject(body)
            obj.optJSONArray("results")?.let { Video.listFromJsonArray(it) } ?: emptyList()
        }
    }

    suspend fun video(vid: String): Video? {
        val obj = JSONObject(get("/api/video?vid=${enc(vid)}"))
        val v = obj.optJSONObject("video") ?: obj
        return Video.fromJson(v)
    }

    suspend fun related(vid: String): List<Video> {
        val body = get("/api/related?vid=${enc(vid)}").trim()
        return when {
            body.startsWith("[") -> Video.listFromJsonArray(JSONArray(body))
            body.startsWith("{") -> {
                val obj = JSONObject(body)
                val arr = obj.optJSONArray("results") ?: obj.optJSONArray("videos") ?: obj.optJSONArray("related")
                arr?.let { Video.listFromJsonArray(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    suspend fun browse(): List<BrowseArea> {
        val arr = JSONArray(get("/api/browse"))
        val out = mutableListOf<BrowseArea>()
        for (i in 0 until arr.length()) {
            val a = arr.optJSONObject(i) ?: continue
            val cats = mutableListOf<BrowseCategory>()
            a.optJSONArray("categories")?.let { ca ->
                for (j in 0 until ca.length()) {
                    val c = ca.optJSONObject(j) ?: continue
                    cats += BrowseCategory(c.optString("id"), c.optString("name"), c.optInt("count"))
                }
            }
            out += BrowseArea(a.optString("id"), a.optString("name"), a.optInt("count"), cats)
        }
        return out
    }

    suspend fun playlists(): List<Playlist> {
        val arr = JSONArray(get("/api/playlists"))
        val out = mutableListOf<Playlist>()
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            val cover = mutableListOf<String>()
            p.optJSONArray("cover")?.let { ca -> for (j in 0 until ca.length()) cover += ca.optString(j) }
            out += Playlist(
                slug = p.optString("slug"), title = p.optString("title"),
                pitch = p.optString("pitch"), minutes = p.optInt("minutes"),
                count = p.optInt("count"), cover = cover,
            )
        }
        return out
    }

    suspend fun playlist(slug: String): List<Video> {
        val body = get("/api/playlist?slug=${enc(slug)}").trim()
        return when {
            body.startsWith("[") -> Video.listFromJsonArray(JSONArray(body))
            body.startsWith("{") -> {
                val obj = JSONObject(body)
                val arr = obj.optJSONArray("videos") ?: obj.optJSONArray("results")
                arr?.let { Video.listFromJsonArray(it) } ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
