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

package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.ui.components.Button
import androidx.compose.material3.Icon
import com.movtery.zalithlauncher.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import com.movtery.zalithlauncher.ui.components.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.ApkPlugin
import com.movtery.zalithlauncher.game.plugin.PluginLoader
import com.movtery.zalithlauncher.game.plugin.appCacheIcon
import com.movtery.zalithlauncher.launcherName
import com.movtery.zalithlauncher.library.LibraryInfo
import com.movtery.zalithlauncher.library.libraryData
import com.movtery.zalithlauncher.path.URL_COMMUNITY
import com.movtery.zalithlauncher.path.URL_MCMOD
import com.movtery.zalithlauncher.path.URL_SUPPORT
import com.movtery.zalithlauncher.path.URL_WEBLATE
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.GlassLazyColumn
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.LiquidGlassSurface
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.liquidGlassBackdrop
import com.movtery.zalithlauncher.ui.components.CardTitleLayout
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.CardPosition
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsCard
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.rememberSettingsCardShape
import com.movtery.zalithlauncher.ui.theme.itemColor
import com.movtery.zalithlauncher.ui.theme.onItemColor

/** Gap between two sections, on top of the 2dp the list already puts between any two cards. */
private val SectionGap = 8.dp

@Composable
fun AboutInfoScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: TitledNavKey?,
    mainScreenKey: TitledNavKey?,
    checkUpdate: () -> Unit,
    onViewSponsors: () -> Unit,
    openLicense: (raw: Int) -> Unit,
    openLink: (url: String) -> Unit
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.AboutInfo, settingsScreenKey, false)
    ) { isVisible ->
        //Every row is a glass surface, so every row is its own lazy item: only the ones near the viewport
        //are composed and blurred while scrolling. A section is a small glass title card plus its rows.
        GlassLazyColumn(
            modifier = Modifier.fillMaxSize(),
            isVisible = isVisible,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(all = 12.dp)
        ) {
            /** Section title card. All items of a section share [section]'s entrance delay. */
            fun title(itemKey: String, section: Int, text: @Composable () -> String) {
                animatedItem(key = itemKey, delay = section * 50) { ctx ->
                    SectionTitleCard(
                        modifier = Modifier.animated(ctx),
                        title = text()
                    )
                }
            }

            /**
             * One row of a section; [content] receives the modifier that carries the entrance offset.
             * Rows are [CardPosition.Middle] unless they close their section ([last]).
             */
            fun row(itemKey: String, section: Int, last: Boolean = false, content: @Composable (Modifier, CardPosition) -> Unit) {
                animatedItem(key = itemKey, delay = section * 50) { ctx ->
                    content(
                        Modifier
                            .fillMaxWidth()
                            .animated(ctx),
                        if (last) CardPosition.Bottom else CardPosition.Middle
                    )
                }
            }

            fun gap(itemKey: String) {
                item(key = itemKey, contentType = "gap") { Spacer(modifier = Modifier.height(SectionGap)) }
            }

            //Section 0: launcher and its authors
            title("launcher_title", 0) { stringResource(R.string.about_launcher_title) }
            row("launcher", 0) { modifier, position ->
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_launcher),
                    title = BuildKeys.LAUNCHER_NAME,
                    text = stringResource(R.string.about_launcher_version, BuildConfig.VERSION_NAME),
                    //Lite: tombol “Cek pembaruan” dan “Link Project” ditahan sampai fork siap dirilis
                    button = {}
                )
            }
            row("author_movtery", 0) { modifier, position ->
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_avatar_movtery),
                    title = stringResource(R.string.about_launcher_author_movtery_title),
                    text = stringResource(R.string.about_launcher_author_movtery_text, "Zalith Launcher 2"),
                    button = {
                        ScalingActionButton(
                            onClick = { openLink(URL_SUPPORT) }
                        ) {
                            Text(text = stringResource(R.string.about_sponsor))
                        }
                    }
                )
            }
            //Lite: maintainer fork ini; tombol sponsor dikosongkan dulu
            row("maintainer_keda", 0) { modifier, position ->
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_avatar_keda),
                    title = "Keda",
                    text = stringResource(R.string.about_launcher_maintainer_keda_text),
                    button = {}
                )
            }
            row("contributor_fazry", 0, last = true) { modifier, position ->
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_avatar_fazry),
                    title = "Fazry",
                    text = stringResource(R.string.about_launcher_fazry_text),
                    button = {}
                )
            }

            gap("gap_0")

            //Section 1: acknowledgements
            title("ack_title", 1) { stringResource(R.string.about_acknowledgements_title) }
            row("ack_sponsors", 1) { modifier, position ->
                val launcherName = remember { launcherName() }
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_sponsor),
                    title = stringResource(R.string.about_sponsors_title),
                    text = stringResource(R.string.about_sponsors_summary, launcherName),
                    button = {
                        Button(onClick = onViewSponsors) {
                            Text(text = stringResource(R.string.generic_view))
                        }
                    }
                )
            }
            row("ack_bangbang93", 1) { modifier, position ->
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_avatar_bangbang93),
                    title = "bangbang93",
                    text = stringResource(R.string.about_acknowledgements_bangbang93_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    button = {
                        ScalingActionButton(
                            onClick = { openLink("https://ifdian.net/a/bangbang93") }
                        ) {
                            Text(text = stringResource(R.string.about_sponsor))
                        }
                    }
                )
            }
            row("ack_fcl", 1) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_launcher_fcl),
                    title = "Fold Craft Launcher",
                    text = stringResource(R.string.about_acknowledgements_fcl_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    openLicense = { openLicense(R.raw.fcl_license) },
                    openLink = { openLink("https://github.com/FCL-Team/FoldCraftLauncher") }
                )
            }
            row("ack_hmcl", 1) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_launcher_hmcl),
                    title = "Hello Minecraft! Launcher",
                    text = stringResource(R.string.about_acknowledgements_hmcl_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    openLicense = { openLicense(R.raw.hmcl_license) },
                    openLink = { openLink("https://github.com/HMCL-dev/HMCL") }
                )
            }
            row("ack_mcmod", 1) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_platform_mcmod),
                    title = stringResource(R.string.about_acknowledgements_mcmod),
                    text = stringResource(R.string.about_acknowledgements_mcmod_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    openLink = { openLink(URL_MCMOD) }
                )
            }
            row("ack_mcim", 1) { modifier, position ->
                ButtonIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_avatar_mcim),
                    title = "mcmod-info-mirror",
                    text = stringResource(R.string.about_acknowledgements_mcim_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    button = {
                        ScalingActionButton(
                            onClick = { openLink("https://www.mcimirror.top/sponsor") }
                        ) {
                            Text(text = stringResource(R.string.about_sponsor))
                        }
                    }
                )
            }
            row("ack_pcl2", 1) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_launcher_pcl2),
                    title = "Plain Craft Launcher 2",
                    text = stringResource(R.string.about_acknowledgements_pcl_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    openLink = { openLink("https://github.com/Meloong-Git/PCL") }
                )
            }
            row("ack_pojav", 1) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_launcher_pojav),
                    title = "PojavLauncher",
                    text = stringResource(R.string.about_acknowledgements_pojav_text, BuildKeys.LAUNCHER_SHORT_NAME),
                    openLicense = { openLicense(R.raw.lgpl_3_license) },
                    openLink = { openLink("https://github.com/PojavLauncherTeam/PojavLauncher") }
                )
            }
            row("ack_github", 1) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.ic_github),
                    title = stringResource(R.string.about_acknowledgements_github_community),
                    text = stringResource(R.string.about_acknowledgements_github_community_text),
                    openLink = { openLink(URL_COMMUNITY) },
                    useImage = false
                )
            }
            row("ack_weblate", 1, last = true) { modifier, position ->
                LinkIconItem(
                    modifier = modifier,
                    position = position,
                    icon = painterResource(R.drawable.img_platform_weblate),
                    title = stringResource(R.string.about_acknowledgements_weblate_community),
                    text = stringResource(R.string.about_acknowledgements_weblate_community_text),
                    openLink = { openLink(URL_WEBLATE) }
                )
            }

            gap("gap_1")

            //Section 2: third-party libraries (rows are plain lazy items: nothing animates on entrance)
            title("library_title", 2) { stringResource(R.string.about_library_title) }
            items(
                count = libraryData.size,
                key = { "library_$it" },
                contentType = { "library" }
            ) { index ->
                LibraryInfoItem(
                    info = libraryData[index],
                    position = if (index == libraryData.lastIndex) CardPosition.Bottom else CardPosition.Middle,
                    openLicense = openLicense,
                    openLink = openLink
                )
            }

            //Section 3: plugins
            val allPlugins = PluginLoader.allPlugins
            if (allPlugins.isNotEmpty()) {
                gap("gap_2")
                title("plugin_title", 3) { stringResource(R.string.about_plugin_title) }
                allPlugins.forEachIndexed { index, apkPlugin ->
                    row("plugin_${index}_${apkPlugin.packageName}", 3, last = index == allPlugins.lastIndex) { modifier, position ->
                        PluginInfoItem(modifier = modifier, position = position, apkPlugin = apkPlugin)
                    }
                }
            }
        }
    }
}

/** Title-only glass card used as the header of a section whose items are separate lazy list items. */
@Composable
private fun SectionTitleCard(
    modifier: Modifier = Modifier,
    title: String
) {
    SettingsCard(
        modifier = modifier,
        position = CardPosition.Top
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AboutItemSurface(
    modifier: Modifier = Modifier,
    color: Color,
    contentColor: Color,
    shape: Shape,
    content: @Composable () -> Unit
) {
    val backdrop = liquidGlassBackdrop()
    if (backdrop != null) {
        LiquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            modifier = modifier,
            containerColor = color,
            contentColor = contentColor,
            blurRadius = 8.dp
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            color = color,
            contentColor = contentColor,
            shape = shape,
            onClick = {},
            content = content
        )
    }
}

@Composable
private fun LinkIconItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    text: String,
    openLicense: (() -> Unit)? = null,
    openLink: (() -> Unit)? = null,
    position: CardPosition = CardPosition.Middle,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
    useImage: Boolean = true
) {
    val shape = rememberSettingsCardShape(position)
    AboutItemSurface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = Modifier
                .size(34.dp)
                .clip(shape = RoundedCornerShape(6.dp))
            if (useImage) {
                Image(
                    modifier = iconModifier,
                    painter = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    modifier = iconModifier,
                    painter = icon,
                    contentDescription = null
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row {
                openLicense?.let {
                    IconButton(
                        onClick = it
                    ) {
                        Icon(
                            modifier = Modifier.size(22.dp),
                            painter = painterResource(R.drawable.ic_copyright_outlined),
                            contentDescription = "License"
                        )
                    }
                }
                openLink?.let {
                    IconButton(
                        onClick = it
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_link),
                            contentDescription = stringResource(R.string.generic_open_link)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonIconItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    text: String,
    button: @Composable RowScope.() -> Unit,
    position: CardPosition = CardPosition.Middle,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
) {
    val shape = rememberSettingsCardShape(position)
    AboutItemSurface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .size(34.dp)
                    .clip(shape = RoundedCornerShape(6.dp)),
                painter = icon,
                contentDescription = null,
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            button()
        }
    }
}

@Composable
private fun PluginInfoItem(
    apkPlugin: ApkPlugin,
    modifier: Modifier = Modifier,
    position: CardPosition = CardPosition.Middle,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
) {
    val shape = rememberSettingsCardShape(position)
    AboutItemSurface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape
    ) {
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconFile = appCacheIcon(apkPlugin.packageName)
            if (iconFile.exists()) {
                val model = remember(context, iconFile) {
                    ImageRequest.Builder(context)
                        .data(iconFile)
                        .build()
                }
                AsyncImage(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(shape = RoundedCornerShape(8.dp)),
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    modifier = Modifier.size(34.dp),
                    painter = painterResource(R.drawable.ic_unknown_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text(
                    text = apkPlugin.appName,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    modifier = Modifier.alpha(0.7f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = apkPlugin.packageName,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (apkPlugin.appVersion.isNotEmpty()) {
                        Text(
                            text = apkPlugin.appVersion,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryInfoItem(
    info: LibraryInfo,
    modifier: Modifier = Modifier,
    position: CardPosition = CardPosition.Middle,
    color: Color = itemColor(),
    contentColor: Color = onItemColor(),
    openLicense: (Int) -> Unit,
    openLink: (url: String) -> Unit
) {
    val shape = rememberSettingsCardShape(position)
    AboutItemSurface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Column(
                    modifier = Modifier.alpha(0.7f)
                ) {
                    info.copyrightInfo?.let { copyrightInfo ->
                        Text(
                            text = copyrightInfo,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        modifier = Modifier.clickable(
                            onClick = {
                                openLicense(info.license.raw)
                            }
                        ),
                        text = "Licensed under the ${info.license.name}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
            }
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = {
                    openLink(info.webUrl)
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_link),
                    contentDescription = null
                )
            }
        }
    }
}