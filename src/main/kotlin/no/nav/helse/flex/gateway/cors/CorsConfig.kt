package no.nav.helse.flex.gateway.cors

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig(@Value("\${allowed.origins}") private val allowedOrigins: String) {

    private val allowedOriginsList: List<String> = allowedOrigins.split(",")

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration()
        corsConfig.allowedOrigins = allowedOriginsList
        corsConfig.allowCredentials = true
        corsConfig.allowedMethods = listOf("*")
        corsConfig.addAllowedHeader("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        return CorsWebFilter(source)
    }
}
