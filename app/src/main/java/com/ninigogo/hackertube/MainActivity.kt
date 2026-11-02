package com.ninigogo.hackertube

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val HackerOrange = Color(0xFFFF6600)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LibraryStore(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = HackerOrange)) {
                HackerTubeApp(store)
            }
        }
    }
}

enum class Tab { Home, Browse, Playlists, Library, Search }

fun playVideo(context: Context, store: LibraryStore, video: Video) {
    store.addHistory(video)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)).apply {
        setPackage("com.google.android.youtube")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

@Composable
fun HackerTubeApp(store: LibraryStore) {
    val context = LocalContext.current
    val isTv = (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
        Configuration.UI_MODE_TYPE_TELEVISION
    var tab by remember { mutableStateOf(Tab.Home) }
    var detail by remember { mutableStateOf<Video?>(null) }
    var category by remember { mutableStateOf<BrowseCategory?>(null) }
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var libVersion by remember { mutableIntStateOf(0) }

    BackHandler(enabled = detail != null || category != null || playlist != null) {
        when {
            detail != null -> detail = null
            playlist != null -> playlist = null
            category != null -> category = null
        }
    }

    Scaffold(
        bottomBar = {
            if (!isTv && detail == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == Tab.Home, onClick = { tab = Tab.Home },
                        icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Home") })
                    NavigationBarItem(
                        selected = tab == Tab.Browse, onClick = { tab = Tab.Browse },
                        icon = { Icon(Icons.Filled.GridView, null) }, label = { Text("Browse") })
                    NavigationBarItem(
                        selected = tab == Tab.Playlists, onClick = { tab = Tab.Playlists },
                        icon = { Icon(Icons.Filled.PlaylistPlay, null) }, label = { Text("Playlists") })
                    NavigationBarItem(
                        selected = tab == Tab.Library, onClick = { tab = Tab.Library },
                        icon = { Icon(Icons.Filled.Favorite, null) }, label = { Text("Library") })
                    NavigationBarItem(
                        selected = tab == Tab.Search, onClick = { tab = Tab.Search },
                        icon = { Icon(Icons.Filled.Search, null) }, label = { Text("Search") })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                detail != null -> VideoDetail(
                    video = detail!!, store = store, libVersion = libVersion,
                    onBump = { libVersion++ },
                    onPlay = { playVideo(context, store, it); libVersion++ },
                    onOpen = { detail = it },
                )
                playlist != null -> PlaylistScreen(
                    playlist = playlist!!, store = store,
                    onOpen = { detail = it },
                )
                category != null -> CategoryScreen(
                    category = category!!, store = store,
                    onOpen = { detail = it },
                )
                else -> when (tab) {
                    Tab.Home -> HomeScreen(store = store, isTv = isTv, onOpen = { detail = it })
                    Tab.Browse -> BrowseScreen(onCategory = { category = it })
                    Tab.Playlists -> PlaylistsScreen(onPlaylist = { playlist = it })
                    Tab.Library -> LibraryScreen(store = store, libVersion = libVersion, onOpen = { detail = it })
                    Tab.Search -> SearchScreen(store = store, onOpen = { detail = it })
                }
            }
            if (isTv && detail == null) {
                // TV: simple top tab row, D-pad focusable through Compose focus.
                Row(
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tab.values().forEach { t ->
                        FilterChip(selected = tab == t, onClick = { tab = t; detail = null; category = null; playlist = null },
                            label = { Text(t.name) })
                    }
                }
            }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFAAAAAA))
    }
}

@Composable
fun VideoCard(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = video.thumbnail, contentDescription = video.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Text(
                    video.durationLabel,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.White,
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${video.channel} · ${video.viewsLabel}",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (video.recommenders > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text("▲ ${video.recommenders} HN recommendations",
                        style = MaterialTheme.typography.labelSmall, color = HackerOrange)
                }
            }
        }
    }
}

@Composable
fun VideoGrid(videos: List<Video>, isTv: Boolean, onOpen: (Video) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isTv) 3 else 2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(videos, key = { it.vid }) { v -> VideoCard(v) { onOpen(v) } }
    }
}

@Composable
fun LoadingOrError(loading: Boolean, error: String?, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            loading -> CircularProgressIndicator(color = HackerOrange)
            error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Could not load HackerTube.", color = Color.White)
                Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA))
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
fun HomeScreen(store: LibraryStore, isTv: Boolean, onOpen: (Video) -> Unit) {
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var offline by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        loading = true; error = null
        try {
            val hidden = store.hiddenIds()
            videos = HackerTubeApi.feed().filter { it.vid !in hidden }
            offline = false
        } catch (e: Exception) {
            videos = FallbackData.videos()
            offline = true
            if (videos.isEmpty()) error = e.message
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = if (isTv) 48.dp else 0.dp)) {
        Header("HackerTube", "YouTube, built from videos recommended by Hacker News — no algorithm, no slop.")
        if (offline) {
            Text("Offline mode: showing HN classics bundled with the app.",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall, color = HackerOrange)
        }
        if (loading || (error != null && videos.isEmpty())) {
            LoadingOrError(loading, error) { attempt++ }
        } else {
            VideoGrid(videos, isTv, onOpen)
        }
    }
}

@Composable
fun BrowseScreen(onCategory: (BrowseCategory) -> Unit) {
    var areas by remember { mutableStateOf<List<BrowseArea>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        loading = true; error = null
        try { areas = HackerTubeApi.browse() } catch (e: Exception) { error = e.message }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header("Browse", "150 topics, grouped the way Hacker News recommends them.")
        if (loading || error != null) {
            LoadingOrError(loading, error) { attempt++ }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(areas) { area ->
                    Column {
                        Text("${area.name} · ${area.count}", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(area.categories) { cat ->
                                AssistChip(onClick = { onCategory(cat) },
                                    label = { Text("${cat.name} (${cat.count})") })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryScreen(category: BrowseCategory, store: LibraryStore, onOpen: (Video) -> Unit) {
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(category.id, attempt) {
        loading = true; error = null
        try {
            val all = HackerTubeApi.list()
            val filtered = all.filter { v -> v.categories.any { it.id == category.id } }
            videos = if (filtered.isNotEmpty()) filtered else all
        } catch (e: Exception) { error = e.message }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header(category.name, "${category.count} videos recommended on Hacker News")
        if (loading || error != null) LoadingOrError(loading, error) { attempt++ }
        else VideoGrid(videos, false, onOpen)
    }
}

@Composable
fun PlaylistsScreen(onPlaylist: (Playlist) -> Unit) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        loading = true; error = null
        try { playlists = HackerTubeApi.playlists() } catch (e: Exception) { error = e.message }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header("Playlists", "Curated paths through the HN canon.")
        if (loading || error != null) {
            LoadingOrError(loading, error) { attempt++ }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(playlists, key = { it.slug }) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onPlaylist(p) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            if (p.cover.isNotEmpty()) {
                                AsyncImage(
                                    model = "https://i.ytimg.com/vi/${p.cover.first()}/mqdefault.jpg",
                                    contentDescription = p.title,
                                    modifier = Modifier.size(110.dp, 62.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            Column {
                                Text(p.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(p.pitch, style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAAAAA), maxLines = 3)
                                Text("${p.count} videos · ${p.minutes} min",
                                    style = MaterialTheme.typography.labelSmall, color = HackerOrange)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistScreen(playlist: Playlist, store: LibraryStore, onOpen: (Video) -> Unit) {
    var videos by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(playlist.slug, attempt) {
        loading = true; error = null
        try { videos = HackerTubeApi.playlist(playlist.slug) } catch (e: Exception) { error = e.message }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Header(playlist.title, playlist.pitch)
        if (loading || (error != null && videos.isEmpty())) LoadingOrError(loading, error) { attempt++ }
        else VideoGrid(videos, false, onOpen)
    }
}

@Composable
fun LibraryScreen(store: LibraryStore, libVersion: Int, onOpen: (Video) -> Unit) {
    var section by remember { mutableStateOf("Liked") }
    val videos = remember(libVersion, section) {
        when (section) {
            "Liked" -> store.liked()
            "Watch later" -> store.watchLater()
            else -> store.history()
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Header("Library", "Stored only on this device. No account, no sync, no tracking.")
        Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Liked", "Watch later", "History").forEach { s ->
                FilterChip(selected = section == s, onClick = { section = s }, label = { Text(s) })
            }
        }
        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing here yet.", color = Color(0xFFAAAAAA))
            }
        } else {
            VideoGrid(videos, false, onOpen)
        }
    }
}

@Composable
fun SearchScreen(store: LibraryStore, onOpen: (Video) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Video>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Header("Search", "Search the 6,454 videos Hacker News actually recommends.")
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text("e.g. rust, lisp, 3blue1brown") },
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (query.isBlank()) return@Button
                loading = true; searched = true
                scope.launch {
                    results = try { HackerTubeApi.search(query.trim()) } catch (e: Exception) { emptyList() }
                    loading = false
                }
            }) { Text("Go") }
        }
        when {
            loading -> LoadingOrError(true, null) {}
            searched && results.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No HN-recommended videos for that search.", color = Color(0xFFAAAAAA))
            }
            results.isNotEmpty() -> VideoGrid(results, false, onOpen)
        }
    }
}


@Composable
fun YouTubePlayer(vid: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun exitFullscreen() {
        val decor = activity?.window?.decorView as? ViewGroup
        fullscreenView?.let { decor?.removeView(it) }
        fullscreenView = null
        webView?.visibility = View.VISIBLE
    }

    val chromeClient = remember {
        object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                val decor = activity?.window?.decorView as? ViewGroup ?: return
                fullscreenView = view
                webView?.visibility = View.INVISIBLE
                decor.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )
            }

            override fun onHideCustomView() = exitFullscreen()
        }
    }

    DisposableEffect(vid) {
        onDispose {
            exitFullscreen()
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            webView = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webView = this
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                // Present as plain Chrome: YouTube refuses WebView-UA ("; wv") embeds with error 152-4.
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                webChromeClient = chromeClient
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                // Mirror hackertube.org's own working embed: a youtube-nocookie
                // iframe inside a host page (tap-to-play facade). A top-level
                // /embed/ navigation gets YouTube errors 153 / 152-4 instead.
                val html = "<!DOCTYPE html><html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                    "<style>html,body{margin:0;padding:0;background:#000;height:100%}iframe{border:0;width:100%;height:100%;display:block}</style>" +
                    "</head><body>" +
                    "<iframe src='https://www.youtube-nocookie.com/embed/$vid?rel=0&playsinline=1' " +
                    "allow='autoplay; encrypted-media; picture-in-picture; fullscreen' allowfullscreen></iframe>" +
                    "</body></html>"
                // Base64 encoding is required: raw "%" in the CSS silently
                // breaks loadDataWithBaseURL rendering (all-black player).
                val encodedHtml = android.util.Base64.encodeToString(
                    html.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP,
                )
                loadDataWithBaseURL("https://hackertube.org", encodedHtml, "text/html", "base64", null)
            }
        },
    )
}

@Composable
fun VideoDetail(
    video: Video,
    store: LibraryStore,
    libVersion: Int,
    onBump: () -> Unit,
    onPlay: (Video) -> Unit,
    onOpen: (Video) -> Unit,
) {
    val liked = remember(libVersion, video.vid) { store.isLiked(video.vid) }
    val later = remember(libVersion, video.vid) { store.isWatchLater(video.vid) }
    var related by remember { mutableStateOf<List<Video>>(emptyList()) }

    LaunchedEffect(video.vid) {
        store.addHistory(video)
        onBump()
        related = try { HackerTubeApi.related(video.vid) } catch (e: Exception) { emptyList() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // In-app playback: official YouTube IFrame embed, no app switch.
        // Kept outside the LazyColumn: a WebView inside a scrolling lazy
        // container does not composite its cross-origin iframe on device.
        YouTubePlayer(
            vid = video.vid,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(video.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("${video.channel} · ${video.viewsLabel} · ${video.durationLabel}",
                    style = MaterialTheme.typography.bodyMedium, color = Color(0xFFAAAAAA))
                if (video.recommenders > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text("▲ Recommended by ${video.recommenders} people on Hacker News (${video.mentions} mentions)",
                        color = HackerOrange, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { store.toggleLiked(video); onBump() }) {
                        Icon(Icons.Filled.ThumbUp, null); Spacer(Modifier.width(4.dp))
                        Text(if (liked) "Liked" else "Like")
                    }
                    OutlinedButton(onClick = { store.toggleWatchLater(video); onBump() }) {
                        Icon(Icons.Filled.Bookmark, null); Spacer(Modifier.width(4.dp))
                        Text(if (later) "Saved" else "Watch later")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onPlay(video) }) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp))
                        Text("Open in YouTube app")
                    }
                    OutlinedButton(onClick = { store.hide(video.vid); onBump() }) { Text("Not for me") }
                }

                if (video.whyHn.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Why HN recommends it", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(video.whyHn, style = MaterialTheme.typography.bodyMedium)
                }
                if (video.summary.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("About this video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(video.summary, style = MaterialTheme.typography.bodyMedium)
                }
                if (video.whys.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    video.whys.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
                if (video.quoteText.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("\"${video.quoteText}\" — HN, ${video.quoteYear}",
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCCCCCC))
                }
                if (video.tags.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(video.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
            }
        }
        if (related.isNotEmpty()) {
            item {
                Text("Up next from HN", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }
            items(related.chunked(2)) { rowVideos ->
                Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowVideos.forEach { v ->
                        Box(modifier = Modifier.weight(1f)) { VideoCard(v) { onOpen(v) } }
                    }
                    if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(Icons.Filled.History, null, tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Plays in-app via the official YouTube embedded player. Curation: hackertube.org / Hacker News.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
            }
        }
        }
    }
}
