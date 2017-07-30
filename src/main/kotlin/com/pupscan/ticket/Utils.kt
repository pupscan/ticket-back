package com.pupscan.ticket

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun String.cleanName() = this.replace("\"", "").replace("\\", "").capitalize()
fun String.escapeln() = this.replace("\n", "\\n").replace("\r", "\\n").replace("|", "")
fun String.truncat(length: Int) = this.substring(0, if (length > this.length) this.length else length) + "..."
fun String.safeDisplaySecret(): String {
    if (this.isBlank()) return ""
    return "X".repeat(this.length - 3) + this.substring(this.length - 3)
}

val DDMM_HHMM = DateTimeFormatter.ofPattern("dd/MM hh:mm")
fun LocalDateTime.toddMM_hhmm() = this.format(DDMM_HHMM)
