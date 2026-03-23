package com.github.com.chenjia404.meshchat.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.com.chenjia404.meshchat.feature.chatlist.ChatListScreen
import com.github.com.chenjia404.meshchat.feature.contacts.AddFriendScreen
import com.github.com.chenjia404.meshchat.feature.contacts.ContactDetailScreen
import com.github.com.chenjia404.meshchat.feature.contacts.ContactsScreen
import com.github.com.chenjia404.meshchat.feature.directchat.DirectChatScreen
import com.github.com.chenjia404.meshchat.feature.groups.GroupsScreen
import com.github.com.chenjia404.meshchat.feature.groups.CreateGroupScreen
import com.github.com.chenjia404.meshchat.feature.groupchat.GroupChatScreen
import com.github.com.chenjia404.meshchat.feature.media.AudioPlayerScreen
import com.github.com.chenjia404.meshchat.feature.media.ImagePreviewScreen
import com.github.com.chenjia404.meshchat.feature.media.VideoPlayerScreen
import com.github.com.chenjia404.meshchat.feature.settings.SettingsScreen
import androidx.navigation.navArgument

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun MeshChatNavHost() {
    val navController = rememberNavController()
    val destinations = listOf(
        BottomDestination("chat_list", "会话") { Icon(Icons.Outlined.ChatBubbleOutline, null) },
        BottomDestination("contacts", "联系人") { Icon(Icons.Outlined.PersonOutline, null) },
        BottomDestination("groups", "群组") { Icon(Icons.Outlined.Groups, null) },
        BottomDestination("settings", "设置") { Icon(Icons.Outlined.Settings, null) },
    )
    val fullScreenRoutes = setOf(
        "direct_chat/{conversationId}/{entryUnread}",
        "group_chat/{groupId}",
        "contact_detail/{peerId}",
        "add_friend",
        "create_group",
        "image_preview/{url}/{title}",
        "video_player/{url}/{title}",
        "audio_player/{url}/{title}",
    )

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            val currentRoute = currentDestination?.route
            if (currentRoute !in fullScreenRoutes) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "chat_list",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("chat_list") {
                ChatListScreen(
                    onConversationClick = { conversationId, entryUnread ->
                        navController.navigate("direct_chat/$conversationId/$entryUnread")
                    },
                )
            }
            composable("contacts") {
                ContactsScreen(
                    onContactClick = { peerId ->
                        navController.navigate("contact_detail/$peerId")
                    },
                    onAddFriendClick = {
                        navController.navigate("add_friend")
                    },
                    onCreateGroupClick = {
                        navController.navigate("create_group")
                    },
                )
            }
            composable("groups") {
                GroupsScreen(
                    onGroupClick = { groupId ->
                        navController.navigate("group_chat/$groupId")
                    },
                )
            }
            composable("settings") { SettingsScreen() }
            composable("add_friend") {
                AddFriendScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable("create_group") {
                CreateGroupScreen(
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable(
                route = "contact_detail/{peerId}",
                arguments = listOf(navArgument("peerId") { type = NavType.StringType }),
            ) { backStackEntry ->
                ContactDetailScreen(
                    peerId = backStackEntry.arguments?.getString("peerId").orEmpty(),
                    onBackClick = { navController.popBackStack() },
                    onConversationClick = { conversationId ->
                        navController.navigate("direct_chat/$conversationId/0")
                    },
                )
            }
            composable(
                route = "direct_chat/{conversationId}/{entryUnread}",
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                    navArgument("entryUnread") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) {
                DirectChatScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenContactProfile = { peerId ->
                        navController.navigate("contact_detail/$peerId")
                    },
                    onOpenImage = { url, title ->
                        navController.navigate("image_preview/${Uri.encode(url)}/${Uri.encode(title)}")
                    },
                    onOpenVideo = { url, title ->
                        navController.navigate("video_player/${Uri.encode(url)}/${Uri.encode(title)}")
                    },
                    onOpenAudio = { url, title ->
                        navController.navigate("audio_player/${Uri.encode(url)}/${Uri.encode(title)}")
                    },
                )
            }
            composable(
                route = "group_chat/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) {
                GroupChatScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenImage = { url, title ->
                        navController.navigate("image_preview/${Uri.encode(url)}/${Uri.encode(title)}")
                    },
                    onOpenVideo = { url, title ->
                        navController.navigate("video_player/${Uri.encode(url)}/${Uri.encode(title)}")
                    },
                    onOpenAudio = { url, title ->
                        navController.navigate("audio_player/${Uri.encode(url)}/${Uri.encode(title)}")
                    },
                )
            }
            composable(
                route = "image_preview/{url}/{title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                ImagePreviewScreen(
                    onBackClick = { navController.popBackStack() },
                    url = Uri.decode(backStackEntry.arguments?.getString("url").orEmpty()),
                    title = Uri.decode(backStackEntry.arguments?.getString("title").orEmpty()),
                )
            }
            composable(
                route = "video_player/{url}/{title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                VideoPlayerScreen(
                    onBackClick = { navController.popBackStack() },
                    url = Uri.decode(backStackEntry.arguments?.getString("url").orEmpty()),
                    title = Uri.decode(backStackEntry.arguments?.getString("title").orEmpty()),
                )
            }
            composable(
                route = "audio_player/{url}/{title}",
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                AudioPlayerScreen(
                    onBackClick = { navController.popBackStack() },
                    url = Uri.decode(backStackEntry.arguments?.getString("url").orEmpty()),
                    title = Uri.decode(backStackEntry.arguments?.getString("title").orEmpty()),
                )
            }
        }
    }
}
