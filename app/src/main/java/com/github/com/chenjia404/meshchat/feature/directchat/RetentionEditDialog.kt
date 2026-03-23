package com.github.com.chenjia404.meshchat.feature.directchat

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.core.util.RetentionTimeUnit
import com.github.com.chenjia404.meshchat.core.util.minutesToPickerPrefill
import com.github.com.chenjia404.meshchat.core.util.retentionQuantityToMinutes

/**
 * 与 Quark `dialog_chat_retention` 一致：**先选时间单位，再输入数量**，并显示折合分钟。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetentionEditDialog(
    visible: Boolean,
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (minutes: Int) -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val prefill = remember(visible, currentMinutes) { minutesToPickerPrefill(currentMinutes) }
    var unit by remember(visible, currentMinutes) { mutableStateOf(prefill.second) }
    var valueStr by remember(visible, currentMinutes) { mutableStateOf(prefill.first.toString()) }

    val parsedMinutes: Long? = run {
        val v = valueStr.trim()
        if (v.isEmpty()) return@run null
        val num = v.toLongOrNull() ?: return@run null
        if (num < 0) return@run null
        retentionQuantityToMinutes(num, unit)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.retention_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.retention_time_unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                var unitMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = stringResource(unit.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false },
                    ) {
                        RetentionTimeUnit.entries.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(stringResource(u.labelRes)) },
                                onClick = {
                                    unit = u
                                    unitMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.retention_quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = valueStr,
                    onValueChange = { new ->
                        if (new.isEmpty() || new.all { it.isDigit() }) {
                            valueStr = new
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.retention_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when {
                        parsedMinutes == null -> if (valueStr.isBlank()) {
                            stringResource(R.string.retention_hint_wait_input)
                        } else {
                            stringResource(R.string.retention_hint_invalid)
                        }
                        parsedMinutes > Int.MAX_VALUE -> stringResource(R.string.retention_hint_too_large)
                        else -> stringResource(R.string.retention_hint_approx_minutes, parsedMinutes.toInt())
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val v = valueStr.trim()
                    if (v.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.toast_enter_quantity), Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val num = v.toLongOrNull()
                    if (num == null || num < 0) {
                        Toast.makeText(context, context.getString(R.string.toast_invalid_quantity), Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    val minutes = retentionQuantityToMinutes(num, unit)
                    if (minutes > Int.MAX_VALUE) {
                        Toast.makeText(context, context.getString(R.string.retention_hint_too_large), Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    onConfirm(minutes.toInt())
                },
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
