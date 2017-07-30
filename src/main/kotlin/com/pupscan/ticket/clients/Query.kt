package com.pupscan.ticket.clients

import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.repository.Repository
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/clients")
class ClientController(val clientRepository: ClientRepository, val activityRepository: ActivityRepository) {

    @RequestMapping("")
    fun all() = Response(clientRepository.findAll().takeLast(20), clientRepository.count())

    @PostMapping("/search")
    fun search(@RequestBody(required = false) search: String?): Response {
        if (search == null || search.isBlank()) return all()
        val clients = clientRepository.findAllByOrderByScore(TextCriteria()
                .matchingAny(*search.split(' ').toTypedArray()))
        return Response(clients, clients.size.toLong())
    }

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = clientRepository.findById(clientId)

    @RequestMapping("/client/activities/{clientId}")
    fun activities(@PathVariable clientId: String = "") = activityRepository.findAllByClientIdOrderByDate(clientId)
}

data class Response(val clients: List<Client>, val total: Long)

interface ClientRepository : Repository<Client, String> {
    fun count(): Long
    fun findAll(): List<Client>
    fun findById(id: String): List<Client>
    fun findByEmail(email: String): Optional<Client>
    fun findAllByOrderByScore(matchingAny: TextCriteria): List<Client>
}

interface ActivityRepository : Repository<Activity, String> {
    fun findAllByClientIdOrderByDate(clientId: String): List<Activity>
}