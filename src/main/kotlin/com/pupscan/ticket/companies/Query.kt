package com.pupscan.ticket.companies

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.repository.Repository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/companies")
class CompaniesController(val repository: CompanyRepository) {

    @PostMapping("/search")
    fun search(@RequestBody(required = false) body: Body): Page<Company> {
        val (search, page, size) = body
        if (search.isBlank()) return repository.findAll(PageRequest(page, size))
        return repository.findAllByOrderByScore(
                TextCriteria().matchingAny(*search.split(' ').toTypedArray()),
                PageRequest(page, size))
    }

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = repository.findById(clientId)
}

data class Body(val search: String = "", val page: Int = 0, val size: Int = 20)

interface CompanyRepository : Repository<Company, String> {
    fun findAll(pageable: Pageable): Page<Company>
    fun findById(id: String): Company
    fun findAllByOrderByScore(matchingAny: TextCriteria, pageable: Pageable): Page<Company>
}
