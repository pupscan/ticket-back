package com.pupscan.ticket.companies

import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.repository.Repository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/companies")
class CompaniesController(val repository: CompanyRepository) {

    @RequestMapping("")
    fun all() = Response(repository.findAll().takeLast(20), repository.count())

    @PostMapping("/search")
    fun search(@RequestBody(required = false) search: String?): Response {
        if (search == null || search.isBlank()) return all()
        val companies = repository.findAllByOrderByScore(TextCriteria()
                .matchingAny(*search.split(' ').toTypedArray()))
        return Response(companies, companies.size.toLong())
    }

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = repository.findById(clientId)
}

data class Response(val clients: List<Company>, val total: Long)

interface CompanyRepository : Repository<Company, String> {
    fun count(): Long
    fun findAll(): List<Company>
    fun findById(id: String): List<Company>
    fun findAllByOrderByScore(matchingAny: TextCriteria): List<Company>
}
