package no.nav.helse.flex.gateway.routes

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec
import org.springframework.cloud.gateway.route.builder.PredicateSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod

@Configuration
class RouteBuilder {

    @Bean
    fun myRoutes(builder: RouteLocatorBuilder, services: List<Service>, env: Environment): RouteLocator {

        var routes = builder.routes()

        services.forEach { service ->
            val uri = env.getProperty(service.serviceurlProperty)
                ?: throw RuntimeException("Fant ikke property ${service.serviceurlProperty}")

            fun GatewayFilterSpec.pathRewite(): GatewayFilterSpec {
                return this.rewritePath("/${service.basepath}(?<segment>/?.*)", "\$\\{segment}")
            }

            fun GatewayFilterSpec.pathPrefix(): GatewayFilterSpec {
                return if (service.pathPrefix != null) {
                    this.prefixPath(service.pathPrefix)
                } else {
                    this
                }
            }

            fun GatewayFilterSpec.cookieMonster(): GatewayFilterSpec {
                return if (service.extractAuthCookie) {

                    this.filter { exchange, chain ->
                        val httpCookie = exchange.request.cookies.getFirst("selvbetjening-idtoken")
                        if (httpCookie != null) {
                            val mutertRequest =
                                exchange.request.mutate().header("Authorization", "Bearer ${httpCookie.value}")
                                    .build()
                            chain.filter(exchange.mutate().request(mutertRequest).build())
                        } else {
                            chain.filter(exchange)
                        }
                    }
                } else {
                    this
                }
            }

            fun GatewayFilterSpec.fjernCorsFraRequest(): GatewayFilterSpec {
                return this
                    .removeRequestHeader("origin")
                    .removeRequestHeader("host")
                    .removeRequestHeader("sec-fetch-mode")
            }

            fun addPath(paths: List<String>, metode: HttpMethod) {

                paths.map { "/${service.basepath}$it" }
                    .forEach { path ->
                        routes = routes.route("${metode.name} $path") { p: PredicateSpec ->
                            p.path(path)
                                .and()
                                .method(metode)
                                .filters { f ->
                                    f.pathRewite().pathPrefix().cookieMonster().fjernCorsFraRequest()
                                }
                                .uri(uri)
                        }
                    }
            }
            addPath(service.paths.delete, HttpMethod.DELETE)
            addPath(service.paths.put, HttpMethod.PUT)
            addPath(service.paths.post, HttpMethod.POST)
            addPath(service.paths.get, HttpMethod.GET)
        }

        return routes.build()
    }
}
