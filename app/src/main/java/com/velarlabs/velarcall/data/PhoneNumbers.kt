package com.velarlabs.velarcall.data

/**
 * Normalização de números brasileiros para comparação.
 *
 * Exemplos que viram a mesma chave "11987654321":
 *   +55 11 98765-4321, 5511987654321, 011987654321, 0 15 11 98765-4321 (com código de operadora)
 */
object PhoneNumbers {

    fun digits(raw: String?): String = raw.orEmpty().filter { it.isDigit() }

    /** Chave canônica: DDD + número (10 ou 11 dígitos) quando possível; senão, só os dígitos. */
    fun key(raw: String?): String {
        var d = digits(raw)
        if (d.isEmpty()) return d
        // Números especiais (0300, 0303, 0800, 4004...) ficam como estão.
        if (d.startsWith("0300") || d.startsWith("0303") || d.startsWith("0500") ||
            d.startsWith("0800") || d.startsWith("0900")
        ) return d
        if (d.startsWith("55") && d.length in 12..13) d = d.substring(2)
        if (d.startsWith("0")) {
            d = d.substring(1)
            // 0 + operadora (2 dígitos) + DDD + número
            if (d.length in 12..13) d = d.substring(2)
        }
        return d
    }

    /** Formatação amigável para exibir. */
    fun pretty(raw: String?): String {
        val k = key(raw)
        return when {
            k.length == 11 -> "(${k.substring(0, 2)}) ${k.substring(2, 7)}-${k.substring(7)}"
            k.length == 10 -> "(${k.substring(0, 2)}) ${k.substring(2, 6)}-${k.substring(6)}"
            k.isEmpty() -> "Número oculto"
            else -> k
        }
    }
}
