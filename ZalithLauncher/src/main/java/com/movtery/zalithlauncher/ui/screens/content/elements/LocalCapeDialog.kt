/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.elements

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.wardrobe.AccountCapeCollection
import com.movtery.zalithlauncher.game.account.wardrobe.CapeEntry
import com.movtery.zalithlauncher.game.account.wardrobe.MAX_CAPE_FILE_BYTES
import com.movtery.zalithlauncher.game.account.wardrobe.convertToCapePng
import com.movtery.zalithlauncher.ui.components.Button
import com.movtery.zalithlauncher.ui.components.FilledTonalButton
import com.movtery.zalithlauncher.ui.components.GlassDialog as Dialog
import com.movtery.zalithlauncher.ui.components.IconButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.Surface
import com.movtery.zalithlauncher.ui.components.fadeEdge
import com.movtery.zalithlauncher.ui.components.glassRadioColors
import com.movtery.zalithlauncher.ui.components.rememberDialogMaxHeight
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private const val MAX_CAPE_NAME_LENGTH = 40

/**
 * Cape collection of an offline account: pick the active cape, import a new one or delete one.
 * Every change is saved immediately.
 *
 * @param onCapeChanged called after the active cape or the collection changed
 */
@Composable
fun LocalCapeDialog(
    accountUUID: String,
    onDismiss: () -> Unit,
    onCapeChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var manifest by remember(accountUUID) {
        mutableStateOf(AccountCapeCollection.loadManifest(accountUUID))
    }
    var importing by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<CapeEntry?>(null) }

    fun reload() {
        manifest = AccountCapeCollection.loadManifest(accountUUID)
        onCapeChanged()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { importCape(context, accountUUID, uri) }
            }
            importing = false
            result.onSuccess { imported ->
                if (imported) {
                    reload()
                    Toast.makeText(context, R.string.account_capes_import_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.account_capes_import_invalid, Toast.LENGTH_LONG).show()
                }
            }.onFailure { th ->
                val msg = context.getString(R.string.account_capes_import_failed, th.message ?: th.javaClass.simpleName)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight()
                .fillMaxWidth(0.6f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 3.dp)
                    .heightIn(max = (maxHeight - 6.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = cardColor(false),
                contentColor = onCardColor(),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.account_change_cape_select_cape),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    val listState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fadeEdge(state = listState)
                            .weight(1f, fill = false),
                        state = listState
                    ) {
                        item(key = "none") {
                            CapeRow(
                                selected = manifest.activeCapeId == null,
                                entry = null,
                                accountUUID = accountUUID,
                                name = stringResource(R.string.cape_name_none),
                                onClick = {
                                    AccountCapeCollection.clearActiveCape(accountUUID)
                                    reload()
                                }
                            )
                        }
                        //newest first
                        items(manifest.capes.asReversed(), key = { it.id }) { entry ->
                            CapeRow(
                                selected = manifest.activeCapeId == entry.id,
                                entry = entry,
                                accountUUID = accountUUID,
                                name = entry.name,
                                onClick = {
                                    AccountCapeCollection.setActiveCape(accountUUID, entry.id)
                                    reload()
                                },
                                onDelete = { pendingDelete = entry }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        ) {
                            Text(text = stringResource(R.string.generic_close))
                        }
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !importing,
                            onClick = {
                                picker.launch(arrayOf("image/*"))
                            }
                        ) {
                            if (importing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = stringResource(R.string.account_capes_import))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        SimpleAlertDialog(
            title = stringResource(R.string.generic_delete),
            text = stringResource(R.string.account_capes_delete_message, entry.name),
            onConfirm = {
                AccountCapeCollection.removeCape(accountUUID, entry.id)
                pendingDelete = null
                reload()
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun CapeRow(
    selected: Boolean,
    entry: CapeEntry?,
    accountUUID: String,
    name: String,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val thumbWidth = 25.dp
    val thumbHeight = 40.dp
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { thumbWidth.roundToPx() }
    val thumbHeightPx = with(density) { thumbHeight.roundToPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            colors = glassRadioColors(),
            selected = selected,
            onClick = onClick
        )

        if (entry != null) {
            val thumb by produceState<ImageBitmap?>(initialValue = null, entry.id, thumbWidthPx, thumbHeightPx) {
                value = withContext(Dispatchers.IO) {
                    loadCapeFront(
                        file = AccountCapeCollection.getCapeTextureFile(accountUUID, entry.id),
                        width = thumbWidthPx,
                        height = thumbHeightPx
                    )
                }
            }
            thumb?.let {
                Image(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(width = thumbWidth, height = thumbHeight),
                    bitmap = it,
                    contentDescription = null,
                    filterQuality = FilterQuality.None
                )
            } ?: Spacer(modifier = Modifier.width(thumbWidth + 8.dp))
        }

        Text(
            modifier = Modifier.weight(1f),
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.ic_delete_outlined),
                    contentDescription = stringResource(R.string.generic_delete)
                )
            }
        }
    }
}

/**
 * Crop the front of the cape out of its texture, for the list thumbnail.
 * The front is a 10x16 region at (1, 1) in a 64x32 texture.
 */
private fun loadCapeFront(file: java.io.File, width: Int, height: Int): ImageBitmap? {
    if (!file.exists()) return null
    val source = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inScaled = false })
        ?: return null
    return try {
        val scale = source.width / 64f
        val x = scale.toInt()
        val w = (10 * scale).toInt()
        val h = (16 * scale).toInt()
        if (w <= 0 || h <= 0 || x + w > source.width || x + h > source.height) return null
        val front = Bitmap.createBitmap(source, x, x, w, h)
        val scaled = Bitmap.createScaledBitmap(front, width, height, false)
        if (front !== scaled) front.recycle()
        scaled.asImageBitmap()
    } finally {
        source.recycle()
    }
}

/**
 * Read the picked image, validate it and add it to the account's collection as the active cape.
 *
 * @return false if the file is not a valid cape texture
 */
private fun importCape(context: Context, accountUUID: String, uri: Uri): Boolean {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (out.size() + read > MAX_CAPE_FILE_BYTES) return false
            out.write(buffer, 0, read)
        }
        out.toByteArray()
    } ?: return false

    val png = convertToCapePng(bytes) ?: return false
    val name = queryDisplayName(context, uri)
        ?: "Cape ${AccountCapeCollection.loadManifest(accountUUID).capes.size + 1}"
    AccountCapeCollection.addCape(
        accountUUID = accountUUID,
        name = name,
        pngBytes = png
    )
    return true
}

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}.getOrNull()
    ?.substringBeforeLast('.')
    ?.trim()
    ?.take(MAX_CAPE_NAME_LENGTH)
    ?.takeIf { it.isNotEmpty() }
