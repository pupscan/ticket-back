package com.pupscan.ticket.clients

import com.pupscan.ticket.ZTicket
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.TextScore
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.util.*

@Profile("prod")
@Service
class ClientService(val repository: ClientCommandRepository) {
    val logger = LoggerFactory.getLogger(ClientService::class.java)!!

    fun recreateVue(zTickets: List<ZTicket>) {
        logger.info("Recreate vue")
        repository.deleteAll()
        val clients = zTickets
                .filter { clientFilter(it) }
                .map {
                    Client(UUID.randomUUID().toString(),
                            it.via.source.from.name,
                            it.via.source.from.address!!,
                            it.tags)
                }
                .toSet()
        repository.save(clients)
    }

    private fun clientFilter(zTicket: ZTicket): Boolean {
        val mail = zTicket.via.source.from.address ?: ""
        return when {
            mail.isBlank() -> false
            mail.contains("@").not() -> false
            mail.contains("pupscan.com") -> false
            mail.contains("indiegogo.com") -> false
            mail.contains("kisskissbankbank.com") -> false
            else -> true
        }
    }
}

@Document
data class Client(val id: String,
                  @TextIndexed(weight = 5F) val name: String,
                  @TextIndexed(weight = 5F) val email: String,
                  @TextIndexed(weight = 3F) val tags: List<String>,
                  @TextScore val score: Float? = 0F)

interface ClientCommandRepository : CrudRepository<Client, String>