package com.project.semsobra.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.semsobra.data.mapper.HistoricoProducaoMapper
import com.project.semsobra.data.repository.PreparoLocalRepository
import com.project.semsobra.data.repository.ProducaoLocalRepository
import com.project.semsobra.domain.analytics.CalculadoraAnalytics
import com.project.semsobra.domain.previsao.PrevisaoPorMediaPonderada
import com.project.semsobra.domain.repository.PreparoRepository
import com.project.semsobra.domain.repository.ProducaoRepository
import com.project.semsobra.domain.usecase.CadastrarPreparoUseCase
import com.project.semsobra.domain.usecase.ExcluirPreparoUseCase
import com.project.semsobra.domain.usecase.FecharProducaoUseCase
import com.project.semsobra.domain.usecase.SalvarProducaoUseCase
import com.project.semsobra.domain.usecase.ValidarNomePreparoUseCase

class SemSobraViewModelFactory(application: Application) : ViewModelProvider.Factory {
    private val preparoRepository: PreparoRepository = PreparoLocalRepository(application)
    private val producaoRepository: ProducaoRepository = ProducaoLocalRepository(application)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SemSobraViewModel::class.java)) {
            "ViewModel não suportado: ${modelClass.name}"
        }

        val validarNomePreparo = ValidarNomePreparoUseCase(preparoRepository)
        @Suppress("UNCHECKED_CAST")
        return SemSobraViewModel(
            preparoRepository = preparoRepository,
            producaoRepository = producaoRepository,
            cadastrarPreparo = CadastrarPreparoUseCase(preparoRepository, validarNomePreparo),
            excluirPreparo = ExcluirPreparoUseCase(preparoRepository),
            validarNomePreparo = validarNomePreparo,
            salvarProducao = SalvarProducaoUseCase(producaoRepository),
            fecharProducao = FecharProducaoUseCase(producaoRepository),
            motorPrevisao = PrevisaoPorMediaPonderada(),
            calculadoraAnalytics = CalculadoraAnalytics(),
            historicoMapper = HistoricoProducaoMapper()
        ) as T
    }
}
