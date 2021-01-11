package no.nav.helse.flex.gateway.routes

import org.springframework.cloud.gateway.route.RouteLocator
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

            var serviceGatewayKey: String? = null
            service.apiGwKeyProperty?.let {
                env.getProperty(it)?.let { key ->
                    serviceGatewayKey = key
                }
            }

            fun addPath(paths: List<String>, metode: HttpMethod) {

                val pathsMedPrefix = paths.map { "/${service.basepath}$it" }.toTypedArray()

                routes = routes.route("${service.basepath}-${metode.name}") { p: PredicateSpec ->
                    p.path(*pathsMedPrefix)
                        .and()
                        .method(metode)
                        .filters { f ->
                            val filter = if (service.pathRewrite) {
                                f.rewritePath("/${service.basepath}(?<segment>/?.*)", "\$\\{segment}")
                            } else {
                                f
                            }
                            if (serviceGatewayKey != null) {
                                filter.addRequestHeader("x-nav-apiKey", serviceGatewayKey)
                            } else {
                                filter
                            }
                        }
                        .uri(uri)
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
