package no.nav.helse.flex.gateway.readiness

import no.nav.helse.flex.gateway.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.exchange
import java.net.URI
import java.time.Duration

const val APPLICATION_READY = "Application is ready!"
const val APPLICATION_NOT_READY = "Application is NOT ready!"

@RestController
class SelfTestController(
    @Value("\${spinnsyn.backend.url}") private val spinnsynBackendUrl: String,
    @Value("\${syfosoknad.url}") private val syfosoknadUrl: String,
    @Value("\${service.gateway.key}") private val syfosoknadApiGwKey: String,
) {

    var ready = false
    val log = logger()

    private val restTemplate = RestTemplateBuilder()
        .setReadTimeout(Duration.ofSeconds(2))
        .setConnectTimeout(Duration.ofSeconds(1))
        .build()

    fun spinnsynBackendErOk(): Boolean {
        val request = RequestEntity<Any>(HttpMethod.GET, URI("$spinnsynBackendUrl/is_alive"))
        val res: ResponseEntity<String> = restTemplate.exchange(request)
        return res.statusCode.is2xxSuccessful
    }

    fun syfosoknadErOk(): Boolean {
        val headers = HttpHeaders()
        headers.set("x-nav-apiKey", syfosoknadApiGwKey)
        val request = RequestEntity<Any>(headers, HttpMethod.GET, URI("$syfosoknadUrl/api/internal/isAlive"))
        val res: ResponseEntity<String> = restTemplate.exchange(request)
        return res.statusCode.is2xxSuccessful
    }

    @GetMapping("/internal/isReady", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): ResponseEntity<String> {
        if (ready) {
            return ResponseEntity.ok(APPLICATION_READY)
        }
        try {

            if (spinnsynBackendErOk() && syfosoknadErOk()) {
                log.info("I am ready")
                ready = true
                return ResponseEntity.ok(APPLICATION_READY)
            }
            throw RuntimeException("Ikke klar")
        } catch (e: Exception) {
            log.info("flex-gateway er ikke klar", e)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(APPLICATION_NOT_READY)
        }
    }
}
