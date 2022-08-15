package no.nav.helse.flex.gateway.readiness

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

const val APPLICATION_READY = "Application is ready!"

@RestController
class SelfTestController {

    @GetMapping("/internal/isReady", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun isReady(): ResponseEntity<String> {
        return ResponseEntity.ok(APPLICATION_READY)
    }
}
