package com.llamadroid.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.llamadroid.app.AppGraph
import com.llamadroid.ui.benchmark.BenchmarkScreen
import com.llamadroid.ui.chat.ChatRoute
import com.llamadroid.ui.conversations.ConversationsScreen
import com.llamadroid.ui.models.ModelsScreen
import com.llamadroid.ui.settings.SettingsScreen

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Chat("chat", com.llamadroid.R.string.chat, Icons.Outlined.ChatBubbleOutline),
    Conversations("conversations", com.llamadroid.R.string.conversations, Icons.Outlined.FolderOpen),
    Models("models", com.llamadroid.R.string.models, Icons.Outlined.Storage),
    Settings("settings", com.llamadroid.R.string.settings, Icons.Outlined.Settings),
    Benchmark("benchmark", com.llamadroid.R.string.benchmark, Icons.Outlined.QueryStats),
}

@Composable
fun LlamaDroidRoot(graph: AppGraph) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val destination = backStack?.destination
                Tab.entries.forEach { tab ->
                    val label = stringResource(tab.label)
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController = navController, startDestination = Tab.Chat.route) {
            composable(Tab.Chat.route) { ChatRoute(graph = graph, contentPadding = padding) }
            composable(Tab.Conversations.route) {
                ConversationsScreen(
                    graph = graph,
                    contentPadding = padding,
                    onOpenChat = {
                        graph.selectedChatId.value = it
                        navController.navigate(Tab.Chat.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Tab.Models.route) { ModelsScreen(graph = graph, contentPadding = padding) }
            composable(Tab.Settings.route) { SettingsScreen(graph = graph, contentPadding = padding) }
            composable(Tab.Benchmark.route) { BenchmarkScreen(graph = graph, contentPadding = padding) }
        }
    }
}
