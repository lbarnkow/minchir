package com.github.lbarnkow.minchir.test.testutilities;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.Map;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
    MappingBuilder mb;
    switch (stub.method.toUpperCase()) {
      case "GET":
        mb = WireMock.get(urlPathEqualTo(stub.getUrl()));
        break;
      case "POST":
        mb = WireMock.post(urlPathEqualTo(stub.getUrl()));
        break;
      case "PUT":
        mb = WireMock.put(urlPathEqualTo(stub.getUrl()));
        break;
      default:
        throw new RuntimeException("Unsupported stub method!");
    }

    for (var queryParam : stub.getQueryParams().entrySet()) {
      mb.withQueryParam(queryParam.getKey(), equalTo(queryParam.getValue()));
    }

    for (var header : stub.request.headers.entrySet()) {
      mb.withHeader(header.getKey(), equalTo(header.getValue()));
    }

    if (stub.request.getBody() != null) {
      mb.withRequestBody(equalToJson(stub.request.getBody(), true, false));
    }

    mb.willReturn(buildResponse(stub));

    return mb;
  }

  private ResponseDefinitionBuilder buildResponse(FileBasedWireMockStub stub) {
    var response = aResponse();

    switch (stub.response.type.toLowerCase()) {
      case "json":
        response.withBody(stub.response.body);
        response.withHeader("Content-Type", "application/json");
        break;
      default:
        throw new RuntimeException("Unsupported response type!");
    }

    return response;
  }

  @Data
  public static class FileBasedWireMockStub {
    private String method;
    private String url;
    private Map<String, String> queryParams;
    private FileBasedWireMockStubRequest request;
    private FileBasedWireMockStubResponse response;
  }

  @Data
  public static class FileBasedWireMockStubRequest {
    private Map<String, String> headers;
    private String body;
  }

  @Data
  public static class FileBasedWireMockStubResponse {
    private String type;
    private String body;
  }
}
