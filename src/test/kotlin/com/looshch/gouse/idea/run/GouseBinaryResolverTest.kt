package com.looshch.gouse.idea.run

import com.looshch.gouse.idea.services.GouseSettingsService
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GouseBinaryResolverTest {
    @Test
    fun `returns configured path when set`() {
        val settings = GouseSettingsService()
        settings.setConfiguredExecutablePath("/tmp/custom-gouse")

        val resolver = GouseBinaryResolver(settings)

        assertEquals("/tmp/custom-gouse", resolver.resolveExecutablePath())
    }

    @Test
    fun `falls back to installed path before PATH`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)
        resolver.rememberInstalledExecutablePath("/tmp/bin/gouse")

        assertEquals("/tmp/bin/gouse", resolver.resolveExecutablePath())
    }

    @Test
    fun `parses GOBIN before GOPATH`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)

        val path = resolver.parseInstalledExecutablePath(
            "/custom/bin\n/first/go${File.pathSeparator}/second/go\n",
        )

        assertEquals("/custom/bin/${resolver.binaryName("gouse")}", path)
    }

    @Test
    fun `parses first GOPATH entry when GOBIN is empty`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)

        val path = resolver.parseInstalledExecutablePath("\n/first/go${File.pathSeparator}/second/go\n")

        assertEquals("/first/go/bin/${resolver.binaryName("gouse")}", path)
    }

    @Test
    fun `returns null for empty go env output`() {
        val settings = GouseSettingsService()
        val resolver = GouseBinaryResolver(settings)

        assertNull(resolver.parseInstalledExecutablePath(""))
    }
}
