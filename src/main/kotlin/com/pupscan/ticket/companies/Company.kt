package com.pupscan.ticket.companies

import com.pupscan.ticket.ZTicket
import com.pupscan.ticket.cleanName
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.TextScore
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.util.*

@Service
class CompanyService(val repository: CompanyCommandRepository) {
    val logger = LoggerFactory.getLogger(CompanyService::class.java)!!

    fun recreateVue(zTickets: List<ZTicket>) {
        logger.info("Recreate vue")
        repository.deleteAll()
        val clients = zTickets
                .filter { companyFilter(it) }
                .map {
                    Company(UUID.randomUUID().toString(),
                            it.via.source.from.name.cleanName(),
                            it.via.source.from.address!!)
                }
                .distinctBy { it.email }
        repository.save(clients)
    }

    private fun companyFilter(zTicket: ZTicket): Boolean {
        val status = zTicket.status
        val mail = zTicket.via.source.from.address ?: ""
        return when {
            mail.isBlank() -> false
            mail.contains("@").not() -> false
            status.contains("distributor", true) -> true
            status.contains("resseler", true) -> true
            status.contains("reseller", true) -> true
            status.contains("Distributeur", true) -> true
            status.contains("Revendeur", true) -> true
            else -> false
        }
    }
}


@Document
data class Company(val id: String,
                   @TextIndexed(weight = 5F) val name: String,
                   @TextIndexed(weight = 5F) val email: String,
                   @TextScore val score: Float? = 0F)

interface CompanyCommandRepository : CrudRepository<Company, String>
