package com.project.semsobra.data.backup

import com.project.semsobra.data.local.room.ItemProducaoEntity
import com.project.semsobra.data.local.room.PreparoEntity
import com.project.semsobra.data.local.room.ProducaoEntity
import com.project.semsobra.domain.previsao.model.Turno
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

object BackupJsonCodec {
    const val FORMAT = "com.project.semsobra.backup"
    const val FORMAT_VERSION = 1
    const val MAX_BACKUP_BYTES = 50L * 1024 * 1024

    fun encode(backup: SemSobraBackup, exportedAt: Instant = Instant.now()): String {
        val json = JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("exportedAt", exportedAt.toString())
            .put("preparos", JSONArray().apply { backup.preparos.forEach { put(it.toJson()) } })
            .put("producoes", JSONArray().apply { backup.producoes.forEach { put(it.toJson()) } })
            .put("itens", JSONArray().apply { backup.itens.forEach { put(it.toJson()) } })
            .toString(2)
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
            throw BackupFormatException("O backup gerado é grande demais para este formato")
        }
        return json
    }

    fun decode(json: String): DecodedBackup {
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
            throw BackupFormatException("O arquivo de backup é grande demais")
        }

        try {
            val root = JSONObject(json)
            if (root.requiredString("format") != FORMAT) {
                throw BackupFormatException("Este arquivo não é um backup do SemSobra")
            }
            if (root.requiredInt("formatVersion") != FORMAT_VERSION) {
                throw BackupFormatException("A versão deste backup não é compatível com o aplicativo")
            }
            val exportedAt = try {
                Instant.parse(root.requiredString("exportedAt"))
            } catch (error: RuntimeException) {
                throw BackupFormatException("A data do backup é inválida", error)
            }
            val backup = SemSobraBackup(
                preparos = root.requiredArray("preparos").toPreparoList(),
                producoes = root.requiredArray("producoes").toProducaoList(),
                itens = root.requiredArray("itens").toItemList()
            )
            validar(backup)
            return DecodedBackup(
                backup = backup,
                preview = BackupPreview(
                    exportedAt = exportedAt,
                    preparationCount = backup.preparos.size,
                    productionCount = backup.producoes.size,
                    productionItemCount = backup.itens.size
                )
            )
        } catch (error: BackupFormatException) {
            throw error
        } catch (error: JSONException) {
            throw BackupFormatException("O arquivo de backup está incompleto ou malformado", error)
        } catch (error: RuntimeException) {
            throw BackupFormatException("O conteúdo do backup não é válido", error)
        }
    }

    private fun validar(backup: SemSobraBackup) {
        val preparosPorId = backup.preparos.associateBy(PreparoEntity::id)
        val producoesPorId = backup.producoes.associateBy(ProducaoEntity::id)
        require(backup.preparos.all { it.id > 0 } && preparosPorId.size == backup.preparos.size) {
            "Os IDs dos preparos são inválidos ou repetidos"
        }
        require(backup.producoes.all { it.id > 0 } && producoesPorId.size == backup.producoes.size) {
            "Os IDs das produções são inválidos ou repetidos"
        }
        require(backup.itens.all { it.id > 0 } && backup.itens.map(ItemProducaoEntity::id).toSet().size == backup.itens.size) {
            "Os IDs dos itens são inválidos ou repetidos"
        }

        val nomes = mutableSetOf<Pair<String, Int>>()
        backup.preparos.forEach { preparo ->
            require(preparo.nome.isNotBlank() && preparo.unidadeMedida.isNotBlank()) {
                "Há um preparo sem nome ou unidade de medida"
            }
            require(preparo.diaDaSemana in 0..7 && preparo.diasSemanaMask in 0..127) {
                "Há um preparo com dias da semana inválidos"
            }
            require(nomes.add(sqliteNoCase(preparo.nome) to preparo.diaDaSemana)) {
                "Há preparos duplicados no backup"
            }
        }

        val producoesUnicas = mutableSetOf<Pair<String, String>>()
        backup.producoes.forEach { producao ->
            val data = LocalDate.parse(producao.data)
            require(producao.diaDaSemana == data.dayOfWeek.value) {
                "Há uma produção com dia da semana inconsistente"
            }
            require(Turno.entries.any { it.name == producao.turno }) { "Há uma produção com turno inválido" }
            require(producao.diaDaSemana in 1..7) { "Há uma produção com dia da semana inválido" }
            require(producao.clientesAtendidos in 0..10_000) { "Há uma quantidade de clientes inválida" }
            require(!producao.fechada || producao.clientesAtendidos > 0) {
                "Há um fechamento sem clientes registrados"
            }
            require(producoesUnicas.add(producao.data to producao.turno)) {
                "Há produções repetidas para a mesma data e turno"
            }
        }

        val paresUnicos = mutableSetOf<Pair<Long, Long>>()
        backup.itens.forEach { item ->
            require(item.producaoId in producoesPorId && item.preparoId in preparosPorId) {
                "Há um item ligado a um preparo ou produção inexistente"
            }
            require(paresUnicos.add(item.producaoId to item.preparoId)) {
                "Há itens repetidos na mesma produção"
            }
            require(item.quantidadeProduzida.isFinite() && item.quantidadeProduzida in 0.0005..10_000.0) {
                "Há uma quantidade produzida inválida"
            }
            require(item.quantidadeSobra.isFinite() && item.quantidadeSobra >= 0.0 &&
                item.quantidadeSobra <= item.quantidadeProduzida && item.quantidadeSobra <= 10_000.0
            ) { "Há uma quantidade de sobra inválida" }
            if (item.acabouAntesDoFim) {
                require(item.horarioAcabou?.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")) == true) {
                    "Há um horário inválido para um preparo que acabou antes do fim"
                }
            }
        }
        require(backup.itens.map(ItemProducaoEntity::producaoId).toSet().containsAll(producoesPorId.keys)) {
            "Há uma produção sem itens no backup"
        }
    }

    private fun PreparoEntity.toJson() = JSONObject()
        .put("id", id)
        .put("nome", nome)
        .put("descricao", descricao)
        .put("unidadeMedida", unidadeMedida)
        .put("diaDaSemana", diaDaSemana)
        .put("diasSemanaMask", diasSemanaMask)
        .put("ativo", ativo)

    private fun ProducaoEntity.toJson() = JSONObject()
        .put("id", id)
        .put("data", data)
        .put("diaDaSemana", diaDaSemana)
        .put("clientesAtendidos", clientesAtendidos)
        .put("turno", turno)
        .put("restauranteAberto", restauranteAberto)
        .put("fechada", fechada)
        .put("alteradoEm", alteradoEm ?: JSONObject.NULL)

    private fun ItemProducaoEntity.toJson() = JSONObject()
        .put("id", id)
        .put("producaoId", producaoId)
        .put("preparoId", preparoId)
        .put("quantidadeProduzida", quantidadeProduzida)
        .put("quantidadeSobra", quantidadeSobra)
        .put("acabouAntesDoFim", acabouAntesDoFim)
        .put("horarioAcabou", horarioAcabou ?: JSONObject.NULL)

    private fun preparoFromJson(value: JSONObject) = PreparoEntity(
        id = value.requiredLong("id"),
        nome = value.requiredString("nome"),
        descricao = value.requiredString("descricao"),
        unidadeMedida = value.requiredString("unidadeMedida"),
        diaDaSemana = value.requiredInt("diaDaSemana"),
        diasSemanaMask = value.requiredInt("diasSemanaMask"),
        ativo = value.requiredBoolean("ativo")
    )

    private fun producaoFromJson(value: JSONObject) = ProducaoEntity(
        id = value.requiredLong("id"),
        data = value.requiredString("data"),
        diaDaSemana = value.requiredInt("diaDaSemana"),
        clientesAtendidos = value.requiredInt("clientesAtendidos"),
        turno = value.requiredString("turno"),
        restauranteAberto = value.requiredBoolean("restauranteAberto"),
        fechada = value.requiredBoolean("fechada"),
        alteradoEm = value.optionalString("alteradoEm")
    )

    private fun itemFromJson(value: JSONObject) = ItemProducaoEntity(
        id = value.requiredLong("id"),
        producaoId = value.requiredLong("producaoId"),
        preparoId = value.requiredLong("preparoId"),
        quantidadeProduzida = value.requiredDouble("quantidadeProduzida"),
        quantidadeSobra = value.requiredDouble("quantidadeSobra"),
        acabouAntesDoFim = value.requiredBoolean("acabouAntesDoFim"),
        horarioAcabou = value.optionalString("horarioAcabou")
    )

    private fun JSONObject.requiredString(key: String): String =
        (opt(key) as? String) ?: throw BackupFormatException("Campo obrigatório inválido: $key")

    private fun JSONObject.optionalString(key: String): String? {
        if (!has(key)) throw BackupFormatException("Campo obrigatório ausente: $key")
        val value = opt(key)
        return when {
            value === JSONObject.NULL -> null
            value is String -> value
            else -> throw BackupFormatException("Campo inválido: $key")
        }
    }

    private fun JSONObject.requiredBoolean(key: String): Boolean =
        (opt(key) as? Boolean) ?: throw BackupFormatException("Campo obrigatório inválido: $key")

    private fun JSONObject.requiredLong(key: String): Long {
        val number = requiredNumber(key)
        val value = number.toLong()
        if (value.toDouble() != number.toDouble()) {
            throw BackupFormatException("Campo numérico inválido: $key")
        }
        return value
    }

    private fun JSONObject.requiredInt(key: String): Int =
        requiredLong(key).takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
            ?: throw BackupFormatException("Campo numérico inválido: $key")

    private fun JSONObject.requiredDouble(key: String): Double = requiredNumber(key).toDouble()
        .takeIf(Double::isFinite) ?: throw BackupFormatException("Campo numérico inválido: $key")

    private fun JSONObject.requiredNumber(key: String): Number =
        (opt(key) as? Number) ?: throw BackupFormatException("Campo numérico inválido: $key")

    private fun JSONArray.toPreparoList(): List<PreparoEntity> =
        List(length()) { index -> getJSONObject(index).let(::preparoFromJson) }

    private fun JSONArray.toProducaoList(): List<ProducaoEntity> =
        List(length()) { index -> getJSONObject(index).let(::producaoFromJson) }

    private fun JSONArray.toItemList(): List<ItemProducaoEntity> =
        List(length()) { index -> getJSONObject(index).let(::itemFromJson) }

    private fun JSONObject.requiredArray(key: String): JSONArray =
        optJSONArray(key) ?: throw BackupFormatException("Lista obrigatória inválida: $key")

    private fun sqliteNoCase(value: String): String = buildString(value.length) {
        value.forEach { char -> append(if (char in 'A'..'Z') char + ('a' - 'A') else char) }
    }
}
