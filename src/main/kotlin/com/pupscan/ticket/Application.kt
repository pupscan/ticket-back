package com.pupscan.ticket

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

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


/**********/
/** Util **/
/**********/
