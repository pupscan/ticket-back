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

    @RequestMapping("")
    fun all() = repository.findAll(PageRequest(1, 20))

    @PostMapping("/search")
    fun search(@RequestBody(required = false) search: String?): Page<Company> {
        if (search == null || search.isBlank()) return all()
        return repository.findAllByOrderByScore(
                TextCriteria().matchingAny(*search.split(' ').toTypedArray()),
                PageRequest(1, 20))
    }

    @RequestMapping("/client/{clientId}")
    fun client(@PathVariable clientId: String = "") = repository.findById(clientId)
}

interface CompanyRepository : Repository<Company, String> {
    fun findAll(pageable: Pageable): Page<Company>
    fun findById(id: String): Company
    fun findAllByOrderByScore(matchingAny: TextCriteria, pageable: Pageable): Page<Company>
}
