package no.nav.helse.flex.gateway

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.matching.EqualToPattern
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
        "isdialogmote.url=http://localhost:\${wiremock.server.port}",
        "pto.proxy.url=http://localhost:\${wiremock.server.port}",
    ]
)
@AutoConfigureWireMock(port = 0)
class GatewayTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun testHealth() {
        webClient
            .get().uri("/internal/health")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun testIsReadyErKlar() {

        webClient
            .get().uri("/internal/isReady")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `ok kall videresendes`() {
        stubFor(
            get(urlEqualTo("/api/v1/arbeidstaker/brev"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/isdialogmote/api/v1/arbeidstaker/brev")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `500 kall videresendes`() {
        stubFor(
            get(urlEqualTo("/api/v1/arbeidstaker/brev"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/isdialogmote/api/v1/arbeidstaker/brev")
            .exchange()
            .expectStatus().is5xxServerError
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `ukjent api returnerer 404`() {
        webClient
            .get().uri("/dfgasdyfghuadsfgliuafdg")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `selvbetjening cookie flyttes til auth header`() {
        stubFor(
            get(urlEqualTo("/api/v1/arbeidstaker/brev"))
                .withHeader("Authorization", EqualToPattern("Bearer napoleonskake"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/isdialogmote/api/v1/arbeidstaker/brev")
            .cookie("selvbetjening-idtoken", "napoleonskake")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }

    @Test
    fun `cors request`() {
        stubFor(
            get(urlEqualTo("/api/v1/arbeidstaker/brev"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/isdialogmote/api/v1/arbeidstaker/brev")
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
            .options().uri("/isdialogmote/api/v1/arbeidstaker/brev")
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
            .get().uri("/isdialogmote/api/v1/arbeidstaker/brev")
            .header("Origin", "http://kompromittertside.com")
            .header("Host", "www.path.org")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `ok veilarboppfolging kall videresendes`() {
        stubFor(
            get(urlEqualTo("/proxy/veilarboppfolging/api/oppfolging"))
                .willReturn(
                    aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}")
                        .withHeader("Content-Type", "application/json")
                )
        )

        webClient
            .get().uri("/veilarboppfolging/api/oppfolging")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.headers.Hello").isEqualTo("World")
    }
}
