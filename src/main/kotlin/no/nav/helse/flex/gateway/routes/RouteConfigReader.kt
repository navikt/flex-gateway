package no.nav.helse.flex.gateway.routes

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.gateway.log
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RouteConfigReader {
    val logger = log()

    @Bean
    fun endpoints(): List<Service> {

        val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        val serviceList: List<Service> = mapper.readValue(this.javaClass.getResourceAsStream("/routes.yaml"))

        serviceList.forEach { s ->
            val allePaths = ArrayList<String>().also {
                it.addAll(s.paths.delete)
                it.addAll(s.paths.get)
                it.addAll(s.paths.put)
                it.addAll(s.paths.post)
            }
            allePaths.forEach {
                if (!it.startsWith("/")) {
                    throw RuntimeException("$it skal starte med /")
                }
            }
        }

        logger.info("Setter opp med servicelist $serviceList")
        return serviceList
    }
}
