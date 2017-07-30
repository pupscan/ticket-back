package com.pupscan.ticket.clients

import com.pupscan.ticket.ZTicket
import com.pupscan.ticket.toddMM_hhmm
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class ActivityService(val repository: ActivityCommandRepository, val clientRepository: ClientRepository) {
    val logger = LoggerFactory.getLogger(ActivityService::class.java)!!

    fun recreateVue(zTickets: List<ZTicket>) {
        logger.info("Recreate vue")
        repository.deleteAll()
        val activities = zTickets
                .filter { (it.via.source.from.address ?: "").contains("@") }
                .filter { clientRepository.findByEmail(it.via.source.from.address!!).isPresent }
                .map {
                    createActivity(
                            clientRepository.findByEmail(it.via.source.from.address!!).get().id,
                            it.id,
                            it.subject,
                            it.created_at)
                }
        repository.save(activities)
    }

    private fun createActivity(clientId: String,
                               zenDeskId: Long,
                               subject: String,
                               created: LocalDateTime): Activity {
        return when {
            subject.contains("[Indiegogo] New contribution") -> Activity(
                    UUID.randomUUID().toString(),
                    clientId,
                    "purchase",
                    "Indiegogo",
                    created.toddMM_hhmm(),
                    zenDeskId)
            subject.contains("[Indiegogo] Order Refunded") -> Activity(
                    UUID.randomUUID().toString(),
                    clientId,
                    "refund",
                    "Indiegogo",
                    created.toddMM_hhmm(),
                    zenDeskId)
            subject.contains("[Indiegogo] New Message") -> Activity(
                    UUID.randomUUID().toString(),
                    clientId,
                    "message",
                    "Indiegogo",
                    created.toddMM_hhmm(),
                    zenDeskId)
            subject.contains("[Indiegogo]") -> Activity(
                    UUID.randomUUID().toString(),
                    clientId,
                    "other",
                    "Indiegogo",
                    created.toddMM_hhmm(),
                    zenDeskId)
            else -> Activity(
                    UUID.randomUUID().toString(),
                    clientId,
                    "other",
                    "",
                    created.toddMM_hhmm(),
                    zenDeskId)
        }
    }
}


@Document
data class Activity(val id: String,
                    val clientId: String,
                    val type: String,
                    val description: String,
                    val date: String,
                    val zenDeskId: Long)

interface ActivityCommandRepository : CrudRepository<Activity, String>

