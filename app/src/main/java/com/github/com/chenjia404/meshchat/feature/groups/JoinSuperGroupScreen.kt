package com.github.com.chenjia404.meshchat.feature.groups

import android.content.Context
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
import com.github.com.chenjia404.meshchat.core.util.MeshchatHttpErrors
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class JoinSuperGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
) : ViewModel() {
    fun join(rawUrl: String, onFinished: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onFinished(runCatching { groupRepository.joinSuperGroupByInviteUrl(rawUrl) })
        }
    }
}

private fun formatJoinSuperGroupError(context: Context, e: Throwable): String {
    MeshchatHttpErrors.messageForToast(e)?.let { return it }
    return when {
        e is IllegalStateException && e.message == "super_group_base_mismatch" ->
            context.getString(R.string.error_super_group_base_mismatch)
        e is IllegalArgumentException -> when (e.message) {
            "invalid_url" -> context.getString(R.string.error_super_group_invalid_url)
            "missing_groups_path" -> context.getString(R.string.error_super_group_missing_groups_path)
            "invalid_group_id" -> context.getString(R.string.error_super_group_invalid_group_id)
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.error_super_group_invalid_url)
        }
        else -> e.message?.takeIf { it.isNotBlank() } ?: context.getString(R.string.error_request_failed)
    }
}

@Composable
fun JoinSuperGroupScreen(
    onBackClick: () -> Unit,
    onJoined: (groupId: String) -> Unit,
    viewModel: JoinSuperGroupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }

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
                Text(stringResource(R.string.join_super_group_title), style = MaterialTheme.typography.titleLarge)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text(stringResource(R.string.join_super_group_label_url)) },
                placeholder = { Text(stringResource(R.string.join_super_group_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                singleLine = false,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val raw = urlText.trim()
                    if (raw.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.error_super_group_empty), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.join(raw) { result ->
                        result.onSuccess { groupId -> onJoined(groupId) }
                            .onFailure { e ->
                                Toast.makeText(
                                    context,
                                    formatJoinSuperGroupError(context, e),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.join_super_group_button))
            }
        }
    }
}
