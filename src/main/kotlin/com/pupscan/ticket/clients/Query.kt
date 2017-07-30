package com.pupscan.ticket.clients

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.repository.Repository
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/clients")
class ClientController(val clientRepository: ClientRepository, val activityRepository: ActivityRepository) {

    @RequestMapping("")
    fun all() = clientRepository.findAll(PageRequest(1, 20))

    @PostMapping("/search")
    fun search(@RequestBody(required = false) search: String?): Page<Client> {
        if (search == null || search.isBlank()) return all()
        return clientRepository.findAllByOrderByScore(
                TextCriteria().matchingAny(*search.split(' ').toTypedArray()),
                PageRequest(1, 20))
    }

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = clientRepository.findById(clientId)

    @RequestMapping("/client/activities/{clientId}")
    fun activities(@PathVariable clientId: String = "") = activityRepository.findAllByClientIdOrderByDate(clientId)
}

interface ClientRepository : Repository<Client, String> {
    fun findAll(pageable: Pageable): Page<Client>
    fun findById(id: String): Client
    fun findByEmail(email: String): Optional<Client>
    fun findAllByOrderByScore(matchingAny: TextCriteria, pageable: Pageable): Page<Client>
}

interface ActivityRepository : Repository<Activity, String> {
    fun findAllByClientIdOrderByDate(clientId: String): List<Activity>
}