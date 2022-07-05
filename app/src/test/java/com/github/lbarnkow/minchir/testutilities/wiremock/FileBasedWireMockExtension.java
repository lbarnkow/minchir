package com.github.lbarnkow.minchir.testutilities.wiremock;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_HTML;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.lbarnkow.minchir.testutilities.Resource;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

import lombok.Data;

public class FileBasedWireMockExtension extends WireMockExtension implements Extension, TestInstancePostProcessor {

  private ExtensionContext context;

  public FileBasedWireMockExtension() {
    super();
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
    this.context = context;
  }

  @Override
  protected void onBeforeEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
    try {
      var mapper = new ObjectMapper(new YAMLFactory());
      mapper.configure(FAIL_ON_IGNORED_PROPERTIES, false);
      mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

      var annotation = context.getRequiredTestClass().getAnnotation(FileBasedWireMock.class);
      for (var stubFile : annotation.stubs()) {
        var stubs = mapper.readValue(Resource.load(stubFile), FileBasedWireMockStub[].class);

        for (var stub : stubs) {
          stubFor(buildStub(stub));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private MappingBuilder buildStub(FileBasedWireMockStub stub) {
    MappingBuilder nmb;
    switch (stub.method.toUpperCase()) {
      case "GET":
        nmb = WireMock.get(urlPathEqualTo(stub.getUrl()));
        break;
      case "POST":
        nmb = WireMock.post(urlPathEqualTo(stub.getUrl()));
        break;
      case "PUT":
        nmb = WireMock.put(urlPathEqualTo(stub.getUrl()));
        break;
      case "DELETE":
        nmb = WireMock.delete(urlPathEqualTo(stub.getUrl()));
        break;
      default:
        throw new RuntimeException("Unsupported stub method!");
    }

    var smb = nmb.inScenario("minchir");

    for (var queryParam : stub.getQueryParams().entrySet()) {
      smb.withQueryParam(queryParam.getKey(), equalTo(queryParam.getValue()));
    }

    for (var header : stub.request.headers.entrySet()) {
      smb.withHeader(header.getKey(), equalTo(header.getValue()));
    }

    if (stub.request.getBody() != null) {
      smb.withRequestBody(equalToJson(stub.request.getBody(), true, false));
    }

    if (stub.state.getFrom() != null) {
      smb.whenScenarioStateIs(stub.state.getFrom());
    }

    if (stub.state.getTo() != null) {
      smb.willSetStateTo(stub.state.getTo());
    }

    smb.willReturn(buildResponse(stub));

    return smb;
  }

  private ResponseDefinitionBuilder buildResponse(FileBasedWireMockStub stub) {
    var response = aResponse();

    response.withStatus(stub.response.getStatus());

    response.withBody(stub.response.body);

    for (var header : stub.response.headers.entrySet()) {
      response.withHeader(header.getKey(), header.getValue());
    }

    switch (stub.response.type.toLowerCase()) {
      case "json":
        response.withHeader("Content-Type", APPLICATION_JSON.asString());
        break;
      default:
        response.withHeader("Content-Type", TEXT_HTML.asString());
        break;
    }

    return response;
  }

  @Data
  public static class FileBasedWireMockStub {
    private String method;
    private String url;
    private Map<String, String> queryParams = new HashMap<>();
    private FileBasedWireMockStubRequest request = new FileBasedWireMockStubRequest();
    private FileBasedWireMockStubResponse response = new FileBasedWireMockStubResponse();
    private FileBasedWireMockStubState state = new FileBasedWireMockStubState();
  }

  @Data
  public static class FileBasedWireMockStubRequest {
    private Map<String, String> headers = new HashMap<>();
    private String body;
  }

  @Data
  public static class FileBasedWireMockStubResponse {
    private int status = 200;
    private String type = "html";
    private String body;
    private Map<String, String> headers = new HashMap<>();
  }

  @Data
  public static class FileBasedWireMockStubState {
    private String from;
    private String to;
  }
}
