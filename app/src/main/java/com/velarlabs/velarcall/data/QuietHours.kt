package com.velarlabs.velarcall.data

import java.util.Calendar

/** Lógica do horário silencioso, separada para poder ser testada sem Android. */
object QuietHours {

    fun nowMinutes(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /**
     * true se [now] (minutos desde a meia-noite) está dentro do intervalo [start]..[end).
     * Suporta intervalos que viram a noite (ex.: 22:00 às 07:00).
     */
    fun isActive(now: Int, start: Int, end: Int): Boolean {
        if (start == end) return false
        return if (start < end) now in start until end
        else now >= start || now < end
    }

    fun format(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return "%02d:%02d".format(h, m)
    }
}
