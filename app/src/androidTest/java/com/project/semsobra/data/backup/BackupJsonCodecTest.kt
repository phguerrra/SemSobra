package com.project.semsobra.data.backup

import com.project.semsobra.data.local.room.ItemProducaoEntity
import com.project.semsobra.data.local.room.PreparoEntity
import com.project.semsobra.data.local.room.ProducaoEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class BackupJsonCodecTest {
    private val exportedAt = Instant.parse("2026-10-30T12:30:00Z")

    @Test
    fun roundTripPreservaPreparosInativosProducaoEFechamento() {
        val backup = SemSobraBackup(
            preparos = listOf(
                PreparoEntity(10, "Arroz", "Arroz branco", "kg", 0, 127, true),
                PreparoEntity(11, "Feijão", "", "kg", 1, 1, false)
            ),
            producoes = listOf(
                ProducaoEntity(20, "2026-10-30", 5, 123, "ALMOCO", true, true, "2026-10-30T12:00:00Z")
            ),
            itens = listOf(
                ItemProducaoEntity(30, 20, 10, 12.345, 2.125, true, "12:10"),
                ItemProducaoEntity(31, 20, 11, 4.0, 0.0, false, null)
            )
        )

        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(backup, exportedAt))

        assertEquals(backup, decoded.backup)
        assertEquals(exportedAt, decoded.preview.exportedAt)
        assertEquals(2, decoded.preview.preparationCount)
        assertEquals(1, decoded.preview.productionCount)
        assertEquals(2, decoded.preview.productionItemCount)
    }

    @Test
    fun rejectsUnsupportedFormatVersion() {
        val json = JSONObject(BackupJsonCodec.encode(emptyBackup()))
            .put("formatVersion", BackupJsonCodec.FORMAT_VERSION + 1)

        assertThrows(BackupFormatException::class.java) { BackupJsonCodec.decode(json.toString()) }
    }

    @Test
    fun rejectsItemThatReferencesMissingPreparation() {
        val backup = emptyBackup().copy(
            preparos = listOf(PreparoEntity(10, "Arroz", "", "kg", 0, 127, true)),
            producoes = listOf(ProducaoEntity(20, "2026-10-30", 5, 0, "ALMOCO", true, false)),
            itens = listOf(ItemProducaoEntity(30, 20, 999, 1.0))
        )

        assertThrows(BackupFormatException::class.java) {
            BackupJsonCodec.decode(BackupJsonCodec.encode(backup, exportedAt))
        }
    }

    @Test
    fun aceitaBackupValidoComBancoVazio() {
        val decoded = BackupJsonCodec.decode(BackupJsonCodec.encode(emptyBackup(), exportedAt))

        assertTrue(decoded.backup.preparos.isEmpty())
        assertTrue(decoded.backup.producoes.isEmpty())
        assertTrue(decoded.backup.itens.isEmpty())
        assertFalse(decoded.preview.exportedAt.toString().isBlank())
    }

    private fun emptyBackup() = SemSobraBackup(emptyList(), emptyList(), emptyList())
}
