/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.movtery.zalithlauncher.music.LocalMusicLibrary
import com.movtery.zalithlauncher.music.MusicManager
import com.movtery.zalithlauncher.music.ShizukuAccess
import com.movtery.zalithlauncher.music.ShizukuState
import com.movtery.zalithlauncher.music.hasRecordAudioPermission
import com.movtery.zalithlauncher.setting.enums.MusicSourceType
import com.movtery.zalithlauncher.ui.screens.content.home.music.MusicCardManager
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.EnumSettingsCard
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.movtery.zalithlauncher.ui.screens.content.home.overlay.OverlayCardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.contract.MediaPickerContract
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.GlassLazyColumn
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.lazyScrollWithBar
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCardColumn
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsCard
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers

/** Settings of the features added by RayLauncher */
@Composable
fun RaySettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Ray, settingsScreenKey, false)
    ) { isVisible ->
        val listState = rememberLazyListState()

        GlassLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .lazyScrollWithBar(listState),
            state = listState,
            isVisible = isVisible,
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            animatedItem { ctx ->
                SettingsCardColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                ) {
                    SwitchSettingsCard(
                        modifier = Modifier.fillMaxWidth(),
                        position = CardPosition.Single,
                        unit = AllSettings.launcherLiquidGlass,
                        title = stringResource(R.string.settings_launcher_liquid_glass_title),
                        summary = stringResource(R.string.settings_launcher_liquid_glass_summary)
                    )
                }
            }

            animatedItem { ctx ->
                StartupAnimationSettings(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    submitError = submitError
                )
            }

            //Decoration overlay (image/video from the gallery above the UI)
            animatedItem { ctx ->
                OverlaySettings(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx),
                    submitError = submitError
                )
            }

            //Music card
            animatedItem { ctx ->
                MusicSettings(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animated(ctx)
                )
            }
        }
    }
}

@Composable
private fun OverlaySettings(
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cards by OverlayCardManager.cards.collectAsStateWithLifecycle()

    val importErrorText = stringResource(R.string.error_import_image)
    val gifUnsupportedText = stringResource(R.string.settings_launcher_overlay_gif_unsupported)
    val importingText = stringResource(R.string.settings_launcher_overlay_importing)
    val filePicker = rememberLauncherForActivityResult(
        contract = MediaPickerContract(
            allowImages = true,
            allowVideos = true,
            allowMultiple = false
        )
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(androidText(importingText))
                        OverlayCardManager.addFromUri(context, result[0], gifUnsupportedText)
                    },
                    onError = { th ->
                        submitError(
                            ErrorViewModel.ThrowableMessage(
                                title = androidText(importErrorText),
                                message = androidText(th.getMessageOrToString())
                            )
                        )
                    }
                )
            )
        }
    }

    SettingsCardColumn(modifier = modifier) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            title = stringResource(R.string.settings_launcher_overlay_import),
            summary = stringResource(R.string.settings_launcher_overlay_import_summary),
            onClick = { filePicker.launch(Unit) }
        )

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.settings_launcher_overlay_remove_all),
            summary = if (cards.isEmpty()) stringResource(R.string.settings_launcher_overlay_none) else null,
            enabled = cards.isNotEmpty(),
            onClick = { OverlayCardManager.removeAll() }
        )
    }
}

@Composable
private fun MusicSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val cardId by MusicCardManager.cardId.collectAsStateWithLifecycle()
    val tracks by LocalMusicLibrary.tracks.collectAsStateWithLifecycle()
    val hasCard = cardId != null
    val source = AllSettings.musicCardSource.state

    //Permissions are granted outside of the app, so they are read again when the user comes back
    var shizukuState by remember { mutableStateOf(ShizukuAccess.state(context)) }
    var recordAudio by remember { mutableStateOf(hasRecordAudioPermission(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                shizukuState = ShizukuAccess.state(context)
                recordAudio = hasRecordAudioPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        //Shizuku can start, stop or answer the permission dialog while this screen is open
        val subscription = ShizukuAccess.subscribe { shizukuState = ShizukuAccess.state(context) }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            subscription.close()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch { LocalMusicLibrary.importUris(context, uris) }
        }
    }
    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        recordAudio = granted
        MusicManager.refresh()
    }

    SettingsCardColumn(modifier = modifier) {
        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Top,
            title = stringResource(R.string.settings_launcher_music_add),
            summary = stringResource(
                if (hasCard) R.string.settings_launcher_music_added else R.string.settings_launcher_music_add_summary
            ),
            enabled = !hasCard,
            onClick = { MusicCardManager.addCard() }
        )

        EnumSettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Middle,
            unit = AllSettings.musicCardSource,
            entries = MusicSourceType.entries,
            title = stringResource(R.string.settings_launcher_music_source),
            summary = stringResource(R.string.settings_launcher_music_source_summary),
            getRadioText = { stringResource(it.textRes) },
            getRadioEnable = { hasCard }
        )

        when (source) {
            MusicSourceType.LOCAL -> {
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    title = stringResource(R.string.settings_launcher_music_import),
                    summary = stringResource(R.string.settings_launcher_music_import_summary),
                    enabled = hasCard,
                    onClick = { audioPicker.launch(arrayOf("audio/*")) }
                )
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    title = stringResource(R.string.settings_launcher_music_clear),
                    summary = if (tracks.isEmpty()) {
                        stringResource(R.string.settings_launcher_music_none)
                    } else {
                        stringResource(R.string.settings_launcher_music_count, tracks.size)
                    },
                    enabled = hasCard && tracks.isNotEmpty(),
                    onClick = { LocalMusicLibrary.clear(context) }
                )
            }
            MusicSourceType.BACKGROUND -> {
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    title = stringResource(R.string.settings_launcher_music_shizuku),
                    summary = stringResource(
                        when (shizukuState) {
                            ShizukuState.NOT_INSTALLED -> R.string.settings_launcher_music_shizuku_not_installed
                            ShizukuState.NOT_RUNNING -> R.string.settings_launcher_music_shizuku_not_running
                            ShizukuState.PERMISSION_NEEDED -> R.string.settings_launcher_music_shizuku_permission
                            ShizukuState.GRANTED -> R.string.settings_launcher_music_shizuku_granted
                        }
                    ),
                    enabled = hasCard,
                    onClick = {
                        if (shizukuState == ShizukuState.PERMISSION_NEEDED) {
                            ShizukuAccess.requestPermission(context)
                        } else {
                            ShizukuAccess.openShizuku(context)
                        }
                    }
                )
                SettingsCard(
                    modifier = Modifier.fillMaxWidth(),
                    position = CardPosition.Middle,
                    title = stringResource(R.string.settings_launcher_music_visualizer_permission),
                    summary = stringResource(
                        if (recordAudio) R.string.settings_launcher_music_visualizer_permission_granted
                        else R.string.settings_launcher_music_visualizer_permission_summary
                    ),
                    enabled = hasCard && !recordAudio,
                    onClick = { recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }
        }

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            position = CardPosition.Bottom,
            title = stringResource(R.string.settings_launcher_music_remove),
            enabled = hasCard,
            onClick = { MusicCardManager.removeCard() }
        )
    }
}
