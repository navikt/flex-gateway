package no.nav.helse.flex.gateway.cookie

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthCookieTilHeaderFlytter : GlobalFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val httpCookie = exchange.request.cookies.getFirst("selvbetjening-idtoken")
        return if (httpCookie != null) {
            val mutertRequest = exchange.request.mutate().header("Authorization", "Bearer ${httpCookie.value}").build()
            chain.filter(exchange.mutate().request(mutertRequest).build())
        } else {
            chain.filter(exchange)
        }
    }
}
