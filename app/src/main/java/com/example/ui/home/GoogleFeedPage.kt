package com.example.ui.home

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.glass.GlassSurface
import com.example.ui.glass.glassPressable

/**
 * Dedicated Left-Side Home Page for Google Feed & Discover.
 * Seamlessly integrates Google Search and Discover via official Android intents.
 * Clean minimal Glassmorphism architecture with zero fake content and zero scraping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoogleFeedPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Detect whether Google app is installed
    val hasGoogleApp = remember(context) {
        try {
            context.packageManager.getPackageInfo("com.google.android.googlequicksearchbox", 0) != null
        } catch (_: Exception) {
            false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Minimal Frosted Glass Google Search Capsule
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            cornerRadius = 27.dp,
            opacity = 0.72f,
            elevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { launchGoogleSearch(context) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Google "G" emblem
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4285F4), // Blue
                                    Color(0xFFEA4335), // Red
                                    Color(0xFFFBBC05), // Yellow
                                    Color(0xFF34A853)  // Green
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Search or type URL",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )

                // Voice Search Button
                IconButton(
                    onClick = { launchVoiceSearch(context) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Google Voice Search",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Google Lens / Camera Search Button
                IconButton(
                    onClick = { launchGoogleLens(context) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Google Lens",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Google Discover Portal Card
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            opacity = 0.70f,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Feed,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google Discover",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hasGoogleApp) "Connected" else "Web Feed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Browse your personalized stories, breaking headlines, and interest topics powered directly by Google.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.80f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Button: Open Google Discover
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .glassPressable { launchGoogleDiscover(context) },
                    cornerRadius = 23.dp,
                    opacity = 0.85f,
                    elevation = 4.dp,
                    tintColor = Color(0xFF38BDF8)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Open Discover Feed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Topic Stream Hub
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            opacity = 0.68f,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Explore Topics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Quick shortcuts to official Google topic channels",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.70f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopicChip(
                        label = "Top Stories",
                        icon = Icons.Default.Newspaper,
                        onClick = { launchTopicUrl(context, "https://news.google.com") }
                    )
                    TopicChip(
                        label = "Weather",
                        icon = Icons.Default.Cloud,
                        onClick = { launchGoogleSearchQuery(context, "weather today") }
                    )
                    TopicChip(
                        label = "Technology",
                        icon = Icons.Default.Public,
                        onClick = { launchGoogleSearchQuery(context, "technology news") }
                    )
                    TopicChip(
                        label = "Sports",
                        icon = Icons.Default.SportsScore,
                        onClick = { launchGoogleSearchQuery(context, "sports scores and news") }
                    )
                    TopicChip(
                        label = "Finance",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        onClick = { launchTopicUrl(context, "https://www.google.com/finance") }
                    )
                    TopicChip(
                        label = "Trending",
                        icon = Icons.Default.Tv,
                        onClick = { launchTopicUrl(context, "https://trends.google.com") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun TopicChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .glassPressable(onClick = onClick),
        cornerRadius = 16.dp,
        opacity = 0.65f,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

private fun launchGoogleSearch(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    }
}

private fun launchVoiceSearch(context: Context) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        launchGoogleSearch(context)
    }
}

private fun launchGoogleLens(context: Context) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage("com.google.ar.lens")
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        context.startActivity(intent)
    } catch (_: Exception) {
        launchGoogleSearch(context)
    }
}

private fun launchGoogleDiscover(context: Context) {
    try {
        val googleIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
        if (googleIntent != null) {
            googleIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(googleIntent)
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    } catch (_: Exception) {
        launchGoogleSearch(context)
    }
}

private fun launchGoogleSearchQuery(context: Context, query: String) {
    try {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

private fun launchTopicUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback
    }
}
