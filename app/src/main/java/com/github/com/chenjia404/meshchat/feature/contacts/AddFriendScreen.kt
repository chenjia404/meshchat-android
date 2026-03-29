package com.github.com.chenjia404.meshchat.feature.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.util.extractPeerIdFromScanString
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun AddFriendScreen(
    onBackClick: () -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var peerId by remember { mutableStateOf("") }
    var introText by remember { mutableStateOf("") }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents ?: return@rememberLauncherForActivityResult
        val extracted = extractPeerIdFromScanString(raw)
        if (extracted.isNotBlank()) {
            peerId = extracted
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scanLauncher.launch(buildAddFriendScanOptions(context))
        }
    }

    fun openQrScanner() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                scanLauncher.launch(buildAddFriendScanOptions(context))
            }
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = peerId,
                    onValueChange = { peerId = it },
                    label = { Text(stringResource(R.string.label_target_peer_id)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { openQrScanner() }) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = stringResource(R.string.cd_scan_peer_id),
                    )
                }
            }
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

private fun buildAddFriendScanOptions(context: Context): ScanOptions {
    return ScanOptions().apply {
        setDesiredBarcodeFormats(listOf("QR_CODE"))
        setPrompt(context.getString(R.string.add_friend_scan_prompt))
        setBeepEnabled(false)
    }
}
