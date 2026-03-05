package com.looshch.gouse.idea

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PluginMetadataParityTest {
    @Test
    fun `plugin xml declares official go plugin dependency`() {
        val pluginXml = loadResourceText("META-INF/plugin.xml")

        assertContains(pluginXml, "<depends>org.jetbrains.plugins.go</depends>")
    }

    @Test
    fun `plugin xml uses parity command title wording`() {
        val pluginXml = loadResourceText("META-INF/plugin.xml")

        assertContains(pluginXml, "gouse: Toggle ‘declared and not used’ errors")
    }

    @Test
    fun `messages keep gouse path wording parity`() {
        val properties = loadBundle()

        assertEquals(
            "The configured gouse.path does not point to an executable: {0}",
            properties.getProperty("error.configured.path"),
        )
        assertEquals(
            "gouse was installed, but the executable is still not reachable. Set gouse.path or add your Go bin directory to PATH.",
            properties.getProperty("error.gouse.installed.but.unreachable"),
        )
    }

    private fun loadBundle(): Properties {
        val properties = Properties()
        val stream = javaClass.classLoader.getResourceAsStream("messages/GouseBundle.properties")
        assertNotNull(stream, "Could not load messages/GouseBundle.properties")
        stream.use { properties.load(it) }
        return properties
    }

    private fun loadResourceText(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
        assertNotNull(stream, "Could not load resource: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
