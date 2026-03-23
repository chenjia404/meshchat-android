package com.github.com.chenjia404.meshchat.feature.contacts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.com.chenjia404.meshchat.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.MaterialTheme

@Composable
fun AddFriendScreen(
    onBackClick: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    var peerId by remember { mutableStateOf("") }
    var introText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.add_friend_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.add_friend_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = peerId,
                onValueChange = { peerId = it },
                label = { Text(stringResource(R.string.label_target_peer_id)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = introText,
                onValueChange = { introText = it },
                label = { Text(stringResource(R.string.label_intro)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (peerId.isBlank()) return@Button
                    viewModel.sendRequest(peerId, introText) { ok ->
                        if (ok) {
                            peerId = ""
                            introText = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.send_friend_request))
            }
        }
    }
}

