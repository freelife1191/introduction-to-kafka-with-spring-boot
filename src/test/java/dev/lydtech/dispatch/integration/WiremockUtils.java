package dev.lydtech.dispatch.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WiremockUtils {

    public static void reset() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.resetAllScenarios();
        WireMock.resetToDefault();
    }

    public static void stubWiremock(String url, HttpStatus httpStatus, String body) {
        stubWiremock(url, httpStatus, body, null, null, null);
    }

    public static void stubWiremock(String url, HttpStatus httpStatus, String body, String scenario, String initialState, String nextState) {
        if (scenario != null) {
            stubFor(get(urlEqualTo(url))
                    .inScenario(scenario)
                    .whenScenarioStateIs(initialState)
                    .willReturn(aResponse().withStatus(httpStatus.value()).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE).withBody(body))
                    .willSetStateTo(nextState));
        } else {
            stubFor(get(urlEqualTo(url))
                    .willReturn(aResponse().withStatus(httpStatus.value()).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE).withBody(body)));
        }
    }
}
