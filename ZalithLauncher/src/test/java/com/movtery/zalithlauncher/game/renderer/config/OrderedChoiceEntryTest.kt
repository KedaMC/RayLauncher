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

package com.movtery.zalithlauncher.game.renderer.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderedChoiceEntryTest {
    private val globalEntry = orderedEntry(
        "native", "multiindirect", "multibasevertex", "multiarrays",
        "indirect", "basevertex", "unroll", "compute"
    )

    @Test
    fun keepsDefaultOrder() {
        assertEquals(globalEntry.defaultValue, globalEntry.coerce(globalEntry.defaultValue))
    }

    @Test
    fun removesDuplicatesAndUnknownValuesAndAppendsMissingValues() {
        val value = globalEntry.coerce(
            ConfigScalar.StringValue("compute,native,unknown,compute,multiarrays")
        )

        assertEquals(
            "compute,native,multiarrays,multiindirect,multibasevertex,indirect,basevertex,unroll",
            value.asString()
        )
    }

    @Test
    fun functionEntryRejectsBackendsItDoesNotImplement() {
        val entry = orderedEntry("multiarrays", "multiindirect", "unroll")

        val value = entry.coerce(
            ConfigScalar.StringValue("compute,multiindirect,basevertex")
        )

        assertEquals("multiindirect,multiarrays,unroll", value.asString())
    }

    @Test
    fun enablingEntryWithoutEditingValueStillWritesJson() {
        val store = MemoryRendererConfigStore()
        val entry = globalEntry.copy(defaultEnabled = false)
        store.putEnabled("renderer", entry.id, true)

        val config = resolveRendererConfig(
            rendererId = "renderer",
            spec = RendererConfigSpec(listOf(entry)),
            globalStore = store
        )

        assertTrue(config.jsonFields.containsKey(entry.id))
        assertEquals(entry.defaultValue, config.jsonFields[entry.id])
    }

    @Test
    fun disabledEntryDoesNotWriteJson() {
        val store = MemoryRendererConfigStore()
        val entry = globalEntry.copy(defaultEnabled = false)
        store.putEnabled("renderer", entry.id, false)

        val config = resolveRendererConfig(
            rendererId = "renderer",
            spec = RendererConfigSpec(listOf(entry)),
            globalStore = store
        )

        assertFalse(config.jsonFields.containsKey(entry.id))
    }

    private fun orderedEntry(vararg values: String) = OrderedChoiceEntry(
        id = "multidrawOrder",
        title = null,
        description = null,
        options = values.map { ChoiceOption(ConfigScalar.StringValue(it)) },
        defaultValue = ConfigScalar.StringValue(values.joinToString(",")),
        target = ConfigTarget.JsonField("multidrawOrder")
    )

    private class MemoryRendererConfigStore : RendererConfigStore {
        private val values = mutableMapOf<Pair<String, String>, ConfigScalar>()
        private val enabled = mutableMapOf<Pair<String, String>, Boolean>()

        override fun getValue(rendererId: String, entryId: String): ConfigScalar? =
            values[rendererId to entryId]

        override fun putValue(rendererId: String, entryId: String, value: ConfigScalar) {
            values[rendererId to entryId] = value
        }

        override fun getEnabled(rendererId: String, entryId: String): Boolean? =
            enabled[rendererId to entryId]

        override fun putEnabled(rendererId: String, entryId: String, enabled: Boolean) {
            this.enabled[rendererId to entryId] = enabled
        }

        override fun removeEntry(rendererId: String, entryId: String) {
            values.remove(rendererId to entryId)
            enabled.remove(rendererId to entryId)
        }

        override fun removeRenderer(rendererId: String) {
            values.keys.removeAll { it.first == rendererId }
            enabled.keys.removeAll { it.first == rendererId }
        }

        override fun keys(rendererId: String): Set<String> =
            (values.keys + enabled.keys).filter { it.first == rendererId }.mapTo(mutableSetOf()) { it.second }
    }
}
