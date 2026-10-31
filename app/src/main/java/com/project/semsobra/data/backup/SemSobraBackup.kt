package com.project.semsobra.data.backup

import com.project.semsobra.data.local.room.ItemProducaoEntity
import com.project.semsobra.data.local.room.PreparoEntity
import com.project.semsobra.data.local.room.ProducaoEntity
import java.time.Instant

data class SemSobraBackup(
    val preparos: List<PreparoEntity>,
    val producoes: List<ProducaoEntity>,
    val itens: List<ItemProducaoEntity>
)

data class BackupPreview(
    val exportedAt: Instant,
    val preparationCount: Int,
    val productionCount: Int,
    val productionItemCount: Int
)

data class DecodedBackup(
    val backup: SemSobraBackup,
    val preview: BackupPreview
)

class BackupFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)
