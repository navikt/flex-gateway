package no.nav.helse.flex.fss.gateway.pdl

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import no.nav.helse.flex.gateway.Application
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    classes = [Application::class],
    webEnvironment = RANDOM_PORT,
    properties = [
        "flex.bucket.uploader.url=http://localhost:\${wiremock.server.port}",
        "syfosoknad.url=http://localhost:\${wiremock.server.port}/syfosoknad",
        "service.gateway.key=husnokkel",
    ]
)
@AutoConfigureWireMock(port = 0)
class GatewayTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun testIsAlive() {
        webClient
            .get().uri("/internal/health")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `ok kall videresendes`() {
        stubFor(
            post(urlEqualTo("/opplasting"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .post().uri("/flex-bucket-uploader/opplasting")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `ok kall videresendes med path parameter`() {
        stubFor(
            get(urlEqualTo("/kvittering/1234"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/flex-bucket-uploader/kvittering/1234")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `500 kall videresendes`() {
        stubFor(
            post(urlEqualTo("/opplasting"))

                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .post().uri("/flex-bucket-uploader/opplasting")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `ukjent api returnerer 404`() {
        webClient
            .post().uri("/dfgasdyfghuadsfgliuafdg")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `selvbetjening cookie flyttes til auth header`() {
        stubFor(
            post(urlEqualTo("/opplasting"))
                .withHeader("Authorization", EqualToPattern("Bearer napoleonskake"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")

                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .post().uri("/flex-bucket-uploader/opplasting")
            .cookie("selvbetjening-idtoken", "napoleonskake")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `cors request`() {
        stubFor(
            post(urlEqualTo("/opplasting"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .post().uri("/flex-bucket-uploader/opplasting")
            .header("Origin", "http://domain.nav.no")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://domain.nav.no")
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `cors preflight request`() {
        webClient
            .options().uri("/flex-bucket-uploader/opplasting")
            .header("Origin", "http://domain.nav.no")
            .header("Access-Control-Request-Method", "GET")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true")
            .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://domain.nav.no")
            .expectHeader().valueEquals("Access-Control-Allow-Methods", "GET")
            .expectBody().isEmpty
    }

    @Test
    fun `cors request med feil origin returnerer 403`() {
        webClient
            .post().uri("/flex-bucket-uploader/opplasting")
            .header("Origin", "http://kompromittertside.com")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `api gw key legges på`() {
        stubFor(
            get(urlEqualTo("/syfosoknad/api/soknader"))
                .withHeader("x-nav-apiKey", EqualToPattern("husnokkel"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/syfosoknad/api/soknader")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }
}
