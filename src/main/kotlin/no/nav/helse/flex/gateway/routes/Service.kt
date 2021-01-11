package no.nav.helse.flex.gateway.routes

data class Service(
    val paths: Paths,
    val basepath: String,
    val serviceurlProperty: String,
    val apiGwKeyProperty: String?,
    val pathRewrite: Boolean = true
)

data class Paths(
    val get: List<String> = emptyList(),
    val put: List<String> = emptyList(),
    val post: List<String> = emptyList(),
    val delete: List<String> = emptyList(),
)
