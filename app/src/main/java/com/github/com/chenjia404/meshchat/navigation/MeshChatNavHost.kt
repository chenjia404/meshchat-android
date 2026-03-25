package com.github.com.chenjia404.meshchat.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.com.chenjia404.meshchat.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.com.chenjia404.meshchat.feature.appupdate.AppUpdateViewModel
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.com.chenjia404.meshchat.feature.chatlist.ChatListNavigateTarget
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
import com.github.com.chenjia404.meshchat.feature.publicchannel.CreatePublicChannelScreen
import com.github.com.chenjia404.meshchat.feature.publicchannel.PublicChannelDetailScreen
import com.github.com.chenjia404.meshchat.feature.publicchannel.PublicChannelScreen
import com.github.com.chenjia404.meshchat.feature.publicchannel.SubscribePublicChannelScreen
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
    val appUpdateViewModel: AppUpdateViewModel = hiltViewModel()
    val appUpdateInfo by appUpdateViewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        appUpdateViewModel.checkForAppUpdate(context)
    }
    val destinations = listOf(
        BottomDestination("chat_list", stringResource(R.string.nav_chat_list)) { Icon(Icons.Outlined.ChatBubbleOutline, null) },
        BottomDestination("contacts", stringResource(R.string.nav_contacts)) { Icon(Icons.Outlined.PersonOutline, null) },
        BottomDestination("groups", stringResource(R.string.nav_groups)) { Icon(Icons.Outlined.Groups, null) },
        BottomDestination("settings", stringResource(R.string.nav_settings)) { Icon(Icons.Outlined.Settings, null) },
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
        "public_channel/{channelId}",
        "public_channel_detail/{channelId}",
        "create_public_channel",
        "subscribe_public_channel",
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onNavigate = { target ->
                        when (target) {
                            is ChatListNavigateTarget.DirectChat ->
                                navController.navigate("direct_chat/${target.conversationId}/${target.entryUnread}")
                            is ChatListNavigateTarget.PublicChannel ->
                                navController.navigate("public_channel/${target.channelId}")
                        }
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
                    onCreatePublicChannelClick = {
                        navController.navigate("create_public_channel")
                    },
                    onSubscribePublicChannelClick = {
                        navController.navigate("subscribe_public_channel")
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
            composable("create_public_channel") {
                CreatePublicChannelScreen(
                    onBackClick = { navController.popBackStack() },
                    onCreated = { channelId ->
                        navController.navigate("public_channel/$channelId") {
                            popUpTo("create_public_channel") { inclusive = true }
                        }
                    },
                )
            }
            composable("subscribe_public_channel") {
                SubscribePublicChannelScreen(
                    onBackClick = { navController.popBackStack() },
                    onSubscribed = { channelId ->
                        navController.navigate("public_channel/$channelId") {
                            popUpTo("subscribe_public_channel") { inclusive = true }
                        }
                    },
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
                route = "public_channel/{channelId}",
                arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
            ) {
                PublicChannelScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenChannelProfile = { id ->
                        navController.navigate("public_channel_detail/$id")
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
                route = "public_channel_detail/{channelId}",
                arguments = listOf(navArgument("channelId") { type = NavType.StringType }),
            ) {
                PublicChannelDetailScreen(
                    onBackClick = { navController.popBackStack() },
                    onUnsubscribed = { navController.popBackStack() },
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

    appUpdateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { appUpdateViewModel.dismissAppUpdatePrompt() },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.update_available_body,
                        info.currentVersion,
                        info.latestVersion,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                        appUpdateViewModel.dismissAppUpdatePrompt()
                    },
                ) {
                    Text(stringResource(R.string.update_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { appUpdateViewModel.dismissAppUpdatePrompt() }) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )
    }
    }
}
