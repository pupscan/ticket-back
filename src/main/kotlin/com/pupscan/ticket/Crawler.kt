package com.pupscan.ticket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.pupscan.ticket.clients.ActivityService
import com.pupscan.ticket.clients.ClientService
import com.pupscan.ticket.tickets.TicketService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class UpdateData(@Value("\${zendek.authorization}") val zendeskAuthorization: String,
                 val ticketService: TicketService,
                 val clientService: ClientService,
                 val ActivityService: ActivityService) {
    val logger = LoggerFactory.getLogger(UpdateData::class.java)!!

    init {
        logger.info("Connect to Zendesk with authorizationCode=${zendeskAuthorization.safeDisplaySecret()}")
    }

    @Scheduled(fixedDelay = (3_600_000 * 3), initialDelay = 0)
    fun run() {
        logger.info("Migration Start!")
        var startTime = 0
        var endTime = 0
        val zTickets = ArrayList<ZTicket>(10000)

        do {
            startTime = endTime

            logger.info("Load page from startTime $startTime")
            val restTemplate = RestTemplate().exchange(
                    "https://pupscan.zendesk.com/api/v2/incremental/tickets.json?start_time=$startTime",
                    HttpMethod.GET,
                    HttpEntity<HttpHeaders>(HttpHeaders().apply { set("Authorization", "Basic $zendeskAuthorization") }),
                    ZenDesk::class.java)
            val tickets = restTemplate.body.tickets
            zTickets.addAll(tickets)

            endTime = restTemplate.body.end_time
            logger.info("${tickets.size} tickets loaded")
        } while (startTime != endTime)


        ticketService.recreateVue(zTickets)
        clientService.recreateVue(zTickets)
        ActivityService.recreateVue(zTickets)

        logger.info("Migration done!")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ZenDesk(val tickets: List<ZTicket>, val end_time: Int)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ZTicket(val id: Long,
                   @JsonDeserialize(using = LocalDateTimeDeserializer::class) val created_at: LocalDateTime,
                   @JsonDeserialize(using = LocalDateTimeDeserializer::class) val updated_at: LocalDateTime,
                   val status: String,
                   val subject: String,
                   val description: String,
                   val tags: List<String>,
                   val via: Via)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Via(val channel: String, val source: Source)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Source(val from: From = From())

@JsonIgnoreProperties(ignoreUnknown = true)
data class From(val name: String = "", val address: String? = "")


class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        return LocalDateTime.parse(p.getText(), DateTimeFormatter.ISO_DATE_TIME)
    }
}



