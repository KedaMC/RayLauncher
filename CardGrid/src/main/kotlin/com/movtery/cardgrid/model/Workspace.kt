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

package com.movtery.cardgrid.model

/**
 * A grid of a fixed size, independent of the size of its container: a canvas that is looked at through
 * a movable, zoomable viewport.
 * @param columns number of columns
 * @param rows number of rows
 * @param cellSize edge length of one cell (dp)
 * @param contentUnitsPerCell factor between a cell span and the span reported to the card content
 * @param contentColumns grid width, in content units, that card contents size themselves against
 */
data class Workspace(
    val columns: Int,
    val rows: Int,
    val cellSize: Float,
    val contentUnitsPerCell: Int = 1,
    val contentColumns: Int = columns * contentUnitsPerCell
)
