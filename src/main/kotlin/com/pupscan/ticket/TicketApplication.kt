package com.pupscan.ticket

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.TextScore
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.repository.CrudRepository
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@EnableScheduling
@SpringBootApplication
class TicketApplication {
    @Bean
    fun corsConfigurer() =
            object : WebMvcConfigurerAdapter() {
                override fun addCorsMappings(registry: CorsRegistry) {
                    registry.addMapping("/**").allowedOrigins("*")
                }
            }
}

fun main(args: Array<String>) {
    SpringApplication.run(TicketApplication::class.java, *args)
}

@RestController
@RequestMapping("/ticket")
class TicketController(val repository: TicketRepository) {

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
                        it.status,
                        it.createdDate.format(DateTimeFormatter.ofPattern("dd/MM hh:mm")),
                        it.updatedDate.format(DateTimeFormatter.ofPattern("dd/MM hh:mm")),
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
                            it.status,
                            it.createdDate.format(DateTimeFormatter.ofPattern("dd/MM hh:mm")),
                            it.updatedDate.format(DateTimeFormatter.ofPattern("dd/MM hh:mm")),
                            it.tags.joinToString(),
                            it.name,
                            it.mail,
                            it.messageSubject,
                            it.message.escapeln().truncat(200))
                }
    }
}


data class SimpleTicket(val status: String,
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

interface TicketRepository : CrudRepository<Ticket, String> {
    fun countByTags(tag: String): Int
    fun countByStatus(status: String): Int
    fun findByCreatedDateBetween(from: LocalDate, to: LocalDate, order: Sort = Sort(Sort.Order(Sort.Direction.ASC,
            "CreatedDate")))
            : List<Ticket>

    fun findAllByOrderByScoreDescCreatedDateDesc(textCriteria: TextCriteria): List<Ticket>
}


@Profile("prod")
@Service
class UpdateData(val repository: TicketRepository) {
    val logger = LoggerFactory.getLogger(UpdateData::class.java)!!

    @Scheduled(fixedDelay = (3_600_000 * 3), initialDelay = 0)
    fun run() {
        logger.info("Migration Start!")

        repository.deleteAll()
        var startTime = 0
        var endTime = 0

        do {
            startTime = endTime

            logger.info("Load page from startTime $startTime")
            val restTemplate = RestTemplate().exchange(
                    "https://pupscan.zendesk.com/api/v2/incremental/tickets.json?start_time=$startTime",
                    HttpMethod.GET,
                    HttpEntity<HttpHeaders>(HttpHeaders().apply { set("Authorization", "Basic YWxleGFuZHJlLmhlbWVyeUBwdXBzY2FuLmNvbTo/JVZ5Xk8jSjJDQnM=") }),
                    ZenDesk::class.java)
            val tickets = restTemplate.body.tickets
            tickets.map {
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
            }.forEach { repository.save(it) }
            endTime = restTemplate.body.end_time
            logger.info("${tickets.size} tickets loaded")
        } while (startTime != endTime)

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


fun String.escapeln() = this.replace("\n", "\\n").replace("\r", "\\n").replace("|", "")
fun String.truncat(length: Int) = this.substring(0, if (length > this.length) this.length else length) + "..."
