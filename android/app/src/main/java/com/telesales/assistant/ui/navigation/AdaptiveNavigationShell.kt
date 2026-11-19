package com.telesales.assistant.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.telesales.assistant.R

private data class NavigationDestination(
    val route: String,
    val label: Int,
    val glyph: String,
)

private val destinations = listOf(
    NavigationDestination("dashboard", R.string.nav_dashboard, "⌂"),
    NavigationDestination("settings", R.string.nav_settings, "⋯"),
)

@Composable
fun AdaptiveNavigationShell(
    windowSizeClass: WindowSizeClass,
    selectedRoute: String?,
    visible: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val compact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    if (visible && compact) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.94f)) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = selectedRoute == destination.route,
                            onClick = { onNavigate(destination.route) },
                            icon = { NavigationGlyph(destination.glyph, destination.label) },
                            label = { NavigationLabel(destination.label) },
                        )
                    }
                }
            },
        ) { padding ->
            content(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding))
        }
    } else if (visible) {
        Row(modifier.fillMaxSize()) {
            NavigationRail(containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f)) {
                destinations.forEach { destination ->
                    NavigationRailItem(
                        selected = selectedRoute == destination.route,
                        onClick = { onNavigate(destination.route) },
                        icon = { NavigationGlyph(destination.glyph, destination.label) },
                        label = { NavigationLabel(destination.label) },
                        alwaysShowLabel = true,
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { content(Modifier.fillMaxSize()) }
        }
    } else {
        content(modifier.fillMaxSize())
    }
}

@Composable
private fun NavigationGlyph(glyph: String, label: Int) {
    val description = stringResource(label)
    Text(glyph, fontSize = 22.sp, modifier = Modifier.padding(vertical = 2.dp).semantics {
        contentDescription = description
    },
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

@Composable
private fun NavigationLabel(label: Int) {
    Text(
        stringResource(label),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
