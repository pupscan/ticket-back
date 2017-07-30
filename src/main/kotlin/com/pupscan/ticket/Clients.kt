package com.pupscan.ticket

import org.springframework.data.repository.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/clients")
class ClientsController(val clientsService: ClientsService) {

    @RequestMapping("/all")
    fun all() = clientsService.clients()

    @RequestMapping("/client/{mail:.+}")
    fun client(@PathVariable mail: String = "") = clientsService.client(mail)

    @RequestMapping("/client/activities/{mail:.+}")
    fun activities(@PathVariable mail: String = "") = clientsService.activities(mail)
}

@Service
class ClientsService(val repository: ClientsRepository) {

    fun clients() = repository.findFirst40ByOrderByCreatedDateDesc()
            .filter { it.mail.isNotEmpty() }
            .map { SimpleClient(it.name.cleanName(), it.mail, it.status.capitalize()) }.toSet().take(20)

    fun client(mail: String) = repository.findAllByMail(mail).map { Client(it.name, it.mail, it.tags) }.first()

    fun activities(mail: String) = activities(repository.findAllByMail(mail))

    private fun activities(tickets: List<Ticket>) = tickets.map(transform = this::createActivity).toList()

    private fun createActivity(ticket: Ticket): Activity {
        if (ticket.messageSubject.contains("[Indiegogo] New contribution")) {
            return Activity("purchase", "Indiegogo", ticket.createdDate.toddMM_hhmm(), ticket.zenDeskId)
        } else if (ticket.messageSubject.contains("[Indiegogo] Order Refunded")) {
            return Activity("refund", "Indiegogo", ticket.createdDate.toddMM_hhmm(), ticket.zenDeskId)
        } else if (ticket.messageSubject.contains("[Indiegogo] New Message")) {
            return Activity("message", "Indiegogo", ticket.createdDate.toddMM_hhmm(), ticket.zenDeskId)
        } else if (ticket.messageSubject.contains("[Indiegogo]")) {
            return Activity("other", "Indiegogo", ticket.createdDate.toddMM_hhmm(), ticket.zenDeskId)
        }
        return Activity("message", "", ticket.createdDate.toddMM_hhmm(), ticket.zenDeskId)
    }
}


data class Activity(val type: String, val description: String, val date: String, val zenDeskId: Long)
data class SimpleClient(val name: String, val email: String, val status: String)
data class Client(val name: String, val email: String, val tags: List<String>)


interface ClientsRepository : Repository<Ticket, String> {
    fun findFirst40ByOrderByCreatedDateDesc(): List<Ticket>
    fun findAllByMail(email: String): List<Ticket>
}

fun String.cleanName() = this.replace("\"", "").replace("\\", "").capitalize()