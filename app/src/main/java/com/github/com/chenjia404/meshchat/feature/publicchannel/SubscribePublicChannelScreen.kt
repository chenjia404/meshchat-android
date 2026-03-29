package com.github.com.chenjia404.meshchat.feature.publicchannel

import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.github.com.chenjia404.meshchat.core.util.PublicChannelIdValidator
import kotlinx.coroutines.launch

@HiltViewModel
class SubscribePublicChannelViewModel @Inject constructor(
    private val publicChannelRepository: PublicChannelRepository,
) : ViewModel() {
    fun subscribe(channelId: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { publicChannelRepository.subscribe(channelId) })
        }
    }
}

@Composable
fun SubscribePublicChannelScreen(
    onBackClick: () -> Unit,
    onSubscribed: (String) -> Unit,
    viewModel: SubscribePublicChannelViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var channelId by remember { mutableStateOf("") }

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
                Text(stringResource(R.string.subscribe_public_channel_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.subscribe_public_channel_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = channelId,
                onValueChange = { channelId = it },
                label = { Text(stringResource(R.string.label_channel_uuid)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val id = channelId.trim()
                    if (!PublicChannelIdValidator.isValid(id)) {
                        Toast.makeText(context, context.getString(R.string.error_invalid_channel_uuid), Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    viewModel.subscribe(id) { result ->
                        result.onSuccess { onSubscribed(id) }
                            .onFailure { e ->
                                Toast.makeText(
                                    context,
                                    e.message ?: context.getString(R.string.error_request_failed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.subscribe_public_channel_button))
            }
        }
    }
}
