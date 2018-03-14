package com.clearlabs.orb;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.vavr.CheckedFunction0;
import io.vavr.control.Validation;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class RoutesUtils {

  public static CorsHandler corsHandler() {
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");

    return CorsHandler.create("*")
      .allowedHeaders(allowedHeaders)
      .allowedMethod(HttpMethod.GET)
      .allowedMethod(HttpMethod.POST)
      .allowedMethod(HttpMethod.OPTIONS)
      .allowedMethod(HttpMethod.DELETE)
      .allowedMethod(HttpMethod.PATCH)
      .allowedMethod(HttpMethod.PUT);
  }

  public static CheckedFunction0<JsonObject> protobufToJson(MessageOrBuilder msg) {
    return () -> new JsonObject(JsonFormat.printer().print(msg));
  }

  static Validation<String, String> isNotEmpty(String str) { return !StringUtils.hasLength(str) ? Validation.invalid("Field must not be empty") : Validation.valid(str); }
  static Validation<String, String> isAtLeast(Integer len, String str) { return str.length() <= len ? Validation.invalid(String.format("Field must be at least %d characters.", len)) : Validation.valid(str); }

}
