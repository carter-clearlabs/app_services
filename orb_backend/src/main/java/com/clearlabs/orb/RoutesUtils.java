package com.clearlabs.orb;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.vavr.CheckedFunction0;
import io.vavr.Function1;
import io.vavr.Function3;
import io.vavr.collection.Seq;
import io.vavr.control.Either;
import io.vavr.control.Validation;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

//  static Function1<String, Validation<String, String>> isNotEmptyFn = (str) -> !StringUtils.hasLength(str) ? Validation.invalid("Field must not be empty") : Validation.valid(str);

//  static Function3<String, JsonObject, List<Function1<String, Validation<String, String>>>, Validation<List<String>, String>> validate =
//    (fieldName, json, validations) -> {
//
////      Seq<Either> s = io.vavr.collection.List.empty();
//
//      String field = json.getString(fieldName);
//
//      validations.stream().map(
//        validator -> validator.apply(field));
//
//
//      return Validation.invalid(s);
//    };

}
