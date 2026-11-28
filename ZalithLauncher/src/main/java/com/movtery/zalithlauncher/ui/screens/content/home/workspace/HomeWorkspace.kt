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

package com.movtery.zalithlauncher.ui.screens.content.home.workspace

import com.movtery.cardgrid.model.Workspace

/** Cell size of the home workspace (dp) */
private const val CELL_SIZE = 40f

/**
 * The home screen canvas: 32 x 32 cells. Card contents keep measuring themselves in 20 dp units
 * against a reference width of 40 units.
 */
val HomeWorkspace = Workspace(
    columns = 32,
    rows = 32,
    cellSize = CELL_SIZE,
    contentUnitsPerCell = 2,
    contentColumns = 40
)
