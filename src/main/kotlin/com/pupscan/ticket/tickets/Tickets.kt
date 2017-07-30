package com.pupscan.ticket.tickets

import com.pupscan.ticket.ZTicket
import com.pupscan.ticket.escapeln
import com.pupscan.ticket.toddMM_hhmm
import com.pupscan.ticket.truncat
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.TextScore
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.*

@RestController
@RequestMapping("/ticket")
class TicketsController(val repository: TicketsRepository) {

    @RequestMapping("/main")
    fun main(): All {
        val labelsAndTotal = totalByDayOnTheLast30Days()
        val labels = labelsAndTotal.keys.map { it.dayOfMonth }.toList()
        val total = labelsAndTotal.values.toList()
        val unhappy = totalByDayOnTheLast30Days("unhappy").values.toList()
        val happy = totalByDayOnTheLast30Days("happy").values.toList()
        return All(labels, total, unhappy, happy, total.sum(), unhappy.sum(), happy.sum())
    }


    fun totalByDayOnTheLast30Days(tagName: String = "all"): Map<LocalDate, Int> {
        val from = LocalDate.now().minusDays(30L)
        return (0..30L).map {
            val current = from.plusDays(it)
            val totalDay = repository.findByCreatedDateBetween(current, current.plusDays(1L))
                    .filter { if (tagName != "all") it.tags.contains(tagName) else true }
                    .toList().size
            current to totalDay
        }.toMap()
    }

    @RequestMapping("/trend/value/{tagName}")
    fun trend(@PathVariable tagName: String = "all"): Map<String, Double> {
        val from = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val to = LocalDate.now()
        val amountCurrentWeek = repository.findByCreatedDateBetween(from, to.plusDays(1))
                .filter { if (tagName != "all") it.tags.contains(tagName) else true }
                .toList().size
        return mapOf("total" to amountCurrentWeek.toDouble(), "trend" to computeTrend(tagName))
    }

    fun computeTrend(tagName: String): Double {
        val trend = trendByTags(tagName)
        val max = trend.size
        val totalweek1 = trend.subList(max - 7, max).sum().toDouble()
        val totalweek2 = trend.subList(max - 14, max - 7).sum().toDouble()
        val totalweek3 = trend.subList(max - 21, max - 14).sum().toDouble()
        val totalweek4 = trend.subList(max - 28, max - 21).sum().toDouble()
        val average = listOf(totalweek2, totalweek3, totalweek4).average()
        return (totalweek1 - average) / average * 100
    }


    @RequestMapping("/trend/{tagName}")
    fun trendByTags(@PathVariable tagName: String = "all"): List<Int> {
        val from = LocalDate.now().minusDays(30)
        return (0..30).map {
            val current = from.plusDays(it.toLong())
            repository.findByCreatedDateBetween(current, current.plusDays(1))
                    .filter { if (tagName != "all") it.tags.contains(tagName) else true }
                    .toList().size
        }.toList()
    }

    @RequestMapping("/all")
    fun all() = repository.findByCreatedDateBetween(
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            LocalDate.now().plusDays(1),
            Sort(Sort.Order(Sort.Direction.DESC, "CreatedDate")))
            .map {
                SimpleTicket(
                        it.zenDeskId,
                        it.status,
                        it.createdDate.toddMM_hhmm(),
                        it.updatedDate.toddMM_hhmm(),
                        it.tags.joinToString(),
                        it.name,
                        it.mail,
                        it.messageSubject,
                        it.message.escapeln().truncat(200))
            }

    @RequestMapping("/status/{statusName}")
    fun countByStatus(@PathVariable statusName: String = "new") = repository.countByStatus(statusName)

    @PostMapping("/search")
    fun search(@RequestBody(required = false) search: String?): List<SimpleTicket> {
        if (search == null || search.isBlank()) return all()
        return repository.findAllByOrderByScoreDescCreatedDateDesc(TextCriteria().matchingAny(*search.split(' ')
                .toTypedArray()))
                .map {
                    SimpleTicket(
                            it.zenDeskId,
                            it.status,
                            it.createdDate.toddMM_hhmm(),
                            it.updatedDate.toddMM_hhmm(),
                            it.tags.joinToString(),
                            it.name,
                            it.mail,
                            it.messageSubject,
                            it.message.escapeln().truncat(200))
                }
    }
}


@Service
class TicketService(val repository: TicketsCommandRepository) {

    fun recreateVue(zTickets: List<ZTicket>) {
        repository.deleteAll()
        val tickets = zTickets.map {
            Ticket(UUID.randomUUID().toString(),
                    it.id,
                    it.created_at,
                    it.updated_at,
                    it.status,
                    it.via.channel,
                    it.via.source.from.name,
                    it.via.source.from.address ?: "",
                    it.subject,
                    it.description,
                    it.tags)
        }
        repository.save(tickets)
    }

}

data class SimpleTicket(val zenDeskId: Long,
                        val status: String,
                        val created: String,
                        val updated: String,
                        val tags: String,
                        val name: String,
                        val email: String,
                        val subject: String,
                        val message: String)

data class All(val labels: List<Int>,
               val all: List<Int>,
               val unhappy: List<Int>,
               val happy: List<Int>,
               val totalAll: Int,
               val totalUnhappy: Int,
               val totalHappy: Int)

@Document
data class Ticket(@Id val id: String,
                  val zenDeskId: Long,
                  val createdDate: LocalDateTime,
                  val updatedDate: LocalDateTime,
                  @TextIndexed(weight = 3F) val status: String,
                  @TextIndexed(weight = 3F) val channel: String,
                  @TextIndexed(weight = 6F) val name: String,
                  @TextIndexed(weight = 5F) val mail: String,
                  @TextIndexed(weight = 2F) val messageSubject: String,
                  @TextIndexed(weight = 1F) val message: String,
                  @TextIndexed(weight = 5F) val tags: List<String>,
                  @TextScore val score: Float = 0F)

interface TicketsRepository : Repository<Ticket, String> {
    fun countByStatus(status: String): Int
    fun findByCreatedDateBetween(from: LocalDate, to: LocalDate, order: Sort = Sort(Sort.Order(Sort.Direction.ASC,
            "CreatedDate")))
            : List<Ticket>

    fun findAllByOrderByScoreDescCreatedDateDesc(textCriteria: TextCriteria): List<Ticket>
}

interface TicketsCommandRepository : CrudRepository<Ticket, String>