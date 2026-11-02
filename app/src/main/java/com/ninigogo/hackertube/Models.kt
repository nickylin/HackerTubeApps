package com.ninigogo.hackertube

import org.json.JSONArray
import org.json.JSONObject

data class CategoryRef(val id: String, val name: String)

data class Video(
    val vid: String,
    val title: String,
    val channel: String,
    val published: String,
    val durationS: Int,
    val views: Long,
    val likes: Long,
    val thumbnail: String,
    val summary: String,
    val whyHn: String,
    val contentType: String,
    val level: String,
    val primaryTopic: String,
    val tags: List<String>,
    val categories: List<CategoryRef>,
    val quoteText: String,
    val quoteYear: String,
    val mentions: Int,
    val recommenders: Int,
    val score: Double,
    val whys: List<String>,
    val reason: String,
) {
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$vid"
    val durationLabel: String get() {
        val h = durationS / 3600; val m = (durationS % 3600) / 60; val s = durationS % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
    val viewsLabel: String get() = when {
        views >= 1_000_000 -> "%.1fM views".format(views / 1_000_000.0)
        views >= 1_000 -> "%.1fK views".format(views / 1_000.0)
        else -> "$views views"
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("vid", vid); put("title", title); put("channel", channel)
        put("published", published); put("duration_s", durationS)
        put("views", views); put("likes", likes); put("thumbnail", thumbnail)
        put("summary", summary); put("why_hn", whyHn)
        put("content_type", contentType); put("level", level)
        put("primary_topic", primaryTopic)
        put("tags", JSONArray(tags)); put("whys", JSONArray(whys))
        put("categories", JSONArray(categories.map { JSONObject().apply { put("id", it.id); put("name", it.name) } }))
        put("quote", JSONObject().apply { put("text", quoteText); put("year", quoteYear) })
        put("hn", JSONObject().apply { put("mentions", mentions); put("recommenders", recommenders) })
        put("score", score); put("reason", reason)
    }

    companion object {
        fun fromJson(o: JSONObject): Video {
            val cats = mutableListOf<CategoryRef>()
            o.optJSONArray("categories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    cats += CategoryRef(c.optString("id"), c.optString("name"))
                }
            }
            fun strList(key: String): List<String> {
                val out = mutableListOf<String>()
                o.optJSONArray(key)?.let { arr -> for (i in 0 until arr.length()) out += arr.optString(i) }
                return out
            }
            val hn = o.optJSONObject("hn") ?: JSONObject()
            val quote = o.optJSONObject("quote") ?: JSONObject()
            return Video(
                vid = o.optString("vid"),
                title = o.optString("title"),
                channel = o.optString("channel"),
                published = o.optString("published"),
                durationS = o.optInt("duration_s"),
                views = o.optLong("views"),
                likes = o.optLong("likes"),
                thumbnail = o.optString("thumbnail").ifBlank { "https://i.ytimg.com/vi/${o.optString("vid")}/mqdefault.jpg" },
                summary = o.optString("summary"),
                whyHn = o.optString("why_hn"),
                contentType = o.optString("content_type"),
                level = o.optString("level"),
                primaryTopic = o.optString("primary_topic"),
                tags = strList("tags"),
                categories = cats,
                quoteText = quote.optString("text"),
                quoteYear = quote.optString("year"),
                mentions = hn.optInt("mentions"),
                recommenders = hn.optInt("recommenders"),
                score = o.optDouble("score", 0.0),
                whys = strList("whys"),
                reason = o.optString("reason"),
            )
        }

        fun listFromJsonArray(arr: JSONArray): List<Video> {
            val out = mutableListOf<Video>()
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { out += fromJson(it) }
            return out
        }
    }
}

data class BrowseCategory(val id: String, val name: String, val count: Int)
data class BrowseArea(val id: String, val name: String, val count: Int, val categories: List<BrowseCategory>)

data class Playlist(
    val slug: String, val title: String, val pitch: String,
    val minutes: Int, val count: Int, val cover: List<String>,
)
