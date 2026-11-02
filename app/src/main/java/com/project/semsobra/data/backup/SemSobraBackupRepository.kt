package com.project.semsobra.data.backup

import androidx.room.withTransaction
import com.project.semsobra.data.local.room.SemSobraDatabase

class SemSobraBackupRepository(private val database: SemSobraDatabase) {
    suspend fun exportar(): String = database.withTransaction {
        BackupJsonCodec.encode(
            SemSobraBackup(
                preparos = database.preparoDao().listarTodosParaBackup(),
                producoes = database.producaoDao().listarTodasParaBackup(),
                itens = database.producaoDao().listarTodosItensParaBackup()
            )
        )
    }

    fun inspecionar(json: String): DecodedBackup = BackupJsonCodec.decode(json)

    suspend fun restaurar(json: String) {
        val backup = BackupJsonCodec.decode(json).backup
        database.withTransaction {
            database.producaoDao().excluirTodosItensParaBackup()
            database.producaoDao().excluirTodasParaBackup()
            database.preparoDao().excluirTodosParaBackup()

            database.preparoDao().inserirTodosParaBackup(backup.preparos)
            database.producaoDao().inserirProducoesParaBackup(backup.producoes)
            database.producaoDao().inserirItensParaBackup(backup.itens)
        }
    }
}
