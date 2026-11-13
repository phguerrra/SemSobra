package com.project.semsobra.domain.model

data class Cidade(
    val nome: String,
    val uf: String,
    val latitude: Double,
    val longitude: Double
) {
    val nomeFormatado: String
        get() = "$nome, $uf"

    companion object {
        val SANTIAGO_RS = Cidade("Santiago", "RS", -29.1917, -54.8672)
        val PORTO_ALEGRE_RS = Cidade("Porto Alegre", "RS", -30.0346, -51.2177)
        val SANTA_MARIA_RS = Cidade("Santa Maria", "RS", -29.6842, -53.8069)
        val SAO_PAULO_SP = Cidade("São Paulo", "SP", -23.5505, -46.6333)
        val CURITIBA_PR = Cidade("Curitiba", "PR", -25.4284, -49.2733)
        val RIO_DE_JANEIRO_RJ = Cidade("Rio de Janeiro", "RJ", -22.9068, -43.1729)

        val CIDADES_PADRAO = listOf(
            SANTIAGO_RS,
            PORTO_ALEGRE_RS,
            SANTA_MARIA_RS,
            SAO_PAULO_SP,
            CURITIBA_PR,
            RIO_DE_JANEIRO_RJ
        )
    }
}
