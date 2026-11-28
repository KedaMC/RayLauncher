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

package com.movtery.zalithlauncher.utils.festival

import androidx.annotation.StringRes
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.checkDate
import com.movtery.zalithlauncher.utils.checkDateRange
import com.movtery.zalithlauncher.utils.festival.Festival.entries
import java.time.LocalDate

enum class Festival(
    @field:StringRes
    val textRes: Int
) {
    /**
     * 新年
     */
    NEW_YEAR(
        textRes = R.string.festivals_new_year
    ) {
        override fun isToday(date: LocalDate): Boolean {
            return date.checkDate(1, 1)
        }
    },

    /**
     * 愚人节
     */
    APRIL_FOOLS(
        textRes = R.string.festivals_april_fools
    ) {
        override fun isToday(date: LocalDate): Boolean {
            return date.checkDate(4, 1)
        }
    },

    /**
     * 圣诞节
     */
    CHRISTMAS(
        textRes = R.string.festivals_christmas
    ) {
        override fun isToday(date: LocalDate): Boolean {
            return date.checkDateRange(12, 24..25)
        }
    },

    /**
     * 万圣节
     */
    HALLOWEEN(
        textRes = R.string.festivals_halloween
    ) {
        override fun isToday(date: LocalDate): Boolean {
            return date.checkDate(10, 31)
        }
    },

    /**
     * 情人节
     */
    VALENTINES(
        textRes = R.string.festivals_valentines
    ) {
        override fun isToday(date: LocalDate): Boolean {
            return date.checkDate(2, 14)
        }
    };

    abstract fun isToday(date: LocalDate): Boolean
}


/**
 * 获取指定日期的节日
 */
fun getTodayFestivals(date: LocalDate = LocalDate.now()): List<Festival> {
    return Festival.entries.filter { festival -> festival.isToday(date) }
}
