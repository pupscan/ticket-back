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

    @PostMapping("/search")
    fun search(@RequestBody(required = false) body: Body): Page<Client> {
        val (search, page, size) = body
        if (search.isBlank()) return clientRepository.findAll(PageRequest(page, size))
        return clientRepository.findAllByOrderByScore(
                TextCriteria().matchingAny(*search.split(' ').toTypedArray()),
                PageRequest(page, size))
    }

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = clientRepository.findById(clientId)

    @RequestMapping("/client/activities/{clientId}")
    fun activities(@PathVariable clientId: String = "") = activityRepository.findAllByClientIdOrderByDate(clientId)
}

data class Body(val search: String = "", val page: Int = 0, val size: Int = 20)

interface ClientRepository : Repository<Client, String> {
    fun findAll(pageable: Pageable): Page<Client>
    fun findById(id: String): Client
    fun findByEmail(email: String): Optional<Client>
    fun findAllByOrderByScore(matchingAny: TextCriteria, pageable: Pageable): Page<Client>
}

interface ActivityRepository : Repository<Activity, String> {
    fun findAllByClientIdOrderByDate(clientId: String): List<Activity>
}
