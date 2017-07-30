package com.pupscan.ticket.clients

import org.springframework.data.repository.Repository
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/clients")
class ClientController(val clientRepository: ClientRepository, val activityRepository: ActivityRepository) {

    @RequestMapping("")
    fun all() = Response(clientRepository.findFirst20ByOrderByName(), clientRepository.count())

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = clientRepository.findById(clientId)

    @RequestMapping("/client/activities/{clientId}")
    fun activities(@PathVariable clientId: String = "") = activityRepository.findAllByClientIdOrderByDate(clientId)
}

data class Response(val clients: List<Client>, val total: Long)

interface ClientRepository : Repository<Client, String> {
    fun count(): Long
    fun findFirst20ByOrderByName(): List<Client>
    fun findById(id: String): List<Client>
    fun findByEmail(email: String): Optional<Client>
}

interface ActivityRepository : Repository<Activity, String> {
    fun findAllByClientIdOrderByDate(clientId: String): List<Activity>
}