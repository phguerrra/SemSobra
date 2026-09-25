package com.project.semsobra.domain.usecase

import com.project.semsobra.domain.model.ProductionDayUiModel
import com.project.semsobra.domain.model.ProductionItemUiModel
import com.project.semsobra.domain.model.ProductionSummary
import com.project.semsobra.domain.repository.ProducaoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProducaoUseCasesTest {
    @Test
    fun salvarProducaoPersisteDadosEDevolveHistoricoAtualizado() {
        val repository = RepositorioFalso()
        val useCase = SalvarProducaoUseCase(repository)
        val producao = ProductionDayUiModel(data = "2026-09-25", diaDaSemana = 5)
        val quantidades = mapOf(10L to 12.5)

        val resultado = useCase.executar(producao, quantidades)

        assertEquals(producao, repository.producaoSalva)
        assertEquals(quantidades, repository.quantidadesSalvas)
        assertEquals(repository.historico, resultado)
    }

    @Test(expected = IllegalArgumentException::class)
    fun salvarProducaoRejeitaQuantidadeInvalida() {
        SalvarProducaoUseCase(RepositorioFalso()).executar(
            ProductionDayUiModel(data = "2026-09-25", diaDaSemana = 5),
            mapOf(10L to Double.NaN)
        )
    }

    @Test
    fun fecharProducaoPersisteFechamentoEDevolveHistoricoAtualizado() {
        val repository = RepositorioFalso()
        val useCase = FecharProducaoUseCase(repository)
        val itens = listOf(
            ProductionItemUiModel(
                id = 20L,
                producaoDiaId = 30L,
                alimentoId = 10L,
                quantidadeProduzida = 12.5,
                quantidadeSobra = 2.0
            )
        )

        val resultado = useCase.executar(30L, 100, itens)

        assertTrue(repository.fechamentoFoiSalvo)
        assertEquals(30L, repository.producaoFechadaId)
        assertEquals(100, repository.clientesAtendidos)
        assertEquals(itens, repository.itensFechados)
        assertEquals(repository.historico, resultado)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fecharProducaoRejeitaSobraMaiorQueProduzido() {
        FecharProducaoUseCase(RepositorioFalso()).executar(
            producaoId = 30L,
            clientesAtendidos = 100,
            itens = listOf(
                ProductionItemUiModel(
                    id = 20L,
                    producaoDiaId = 30L,
                    alimentoId = 10L,
                    quantidadeProduzida = 5.0,
                    quantidadeSobra = 6.0
                )
            )
        )
    }

    private class RepositorioFalso : ProducaoRepository {
        val historico = emptyList<ProductionSummary>()
        var producaoSalva: ProductionDayUiModel? = null
        var quantidadesSalvas: Map<Long, Double>? = null
        var fechamentoFoiSalvo = false
        var producaoFechadaId: Long? = null
        var clientesAtendidos: Int? = null
        var itensFechados: List<ProductionItemUiModel>? = null

        override fun observarHistorico(): Flow<List<ProductionSummary>> = emptyFlow()

        override fun salvarProducao(
            producao: ProductionDayUiModel,
            quantidadesPorPreparo: Map<Long, Double>
        ): Long {
            producaoSalva = producao
            quantidadesSalvas = quantidadesPorPreparo
            return 30L
        }

        override fun fecharProducao(
            producaoId: Long,
            clientesAtendidos: Int,
            itens: List<ProductionItemUiModel>
        ) {
            fechamentoFoiSalvo = true
            producaoFechadaId = producaoId
            this.clientesAtendidos = clientesAtendidos
            itensFechados = itens
        }

        override fun listarHistorico(): List<ProductionSummary> = historico
    }
}
