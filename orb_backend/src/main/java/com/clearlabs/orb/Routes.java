package com.clearlabs.orb;

import com.clearlabs.services.auth.gen.AuthServiceGrpc;
import com.clearlabs.services.common.gen.Error;
import com.clearlabs.services.auth.gen.LoginRequest;
import com.clearlabs.services.auth.gen.LoginResponse;
import io.vavr.Function2;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Objects;

import static com.clearlabs.orb.RoutesUtils.isAtLeast;
import static com.clearlabs.orb.RoutesUtils.isNotEmpty;
import static io.vavr.API.*;

@Slf4j
@Component
public class Routes {

  @Autowired
  AuthServiceGrpc.AuthServiceVertxStub authService;

  @Autowired
  Vertx vertx;

  @Autowired
  JWTAuth provider;

  CircuitBreakerOptions options = new CircuitBreakerOptions()
    .setMaxFailures(3) // How many times do we fail til we are in an OPEN state
    .setTimeout(600) // how long until we fail
    .setResetTimeout(5000); // How long to wait til we reset and try again?

  CircuitBreaker authServiceCircuitBreaker;

  @PostConstruct
  public void startServerAndRoutes(){

    authServiceCircuitBreaker = CircuitBreaker.create("login", vertx, options);

    Router router = Router.router(vertx);

    // Parse json posts
    router.route().handler(BodyHandler.create());

    // Add CORS support
    router.route().handler(RoutesUtils.corsHandler());

    // Handle the login route
    router.post("/login").handler(handleLogin);

    router.route().handler(JWTAuthHandler.create(provider));

    router.get("/protected").handler(h -> {
      log.info(h.user().principal().encodePrettily() + "");
      h.response().end(Match(h.user()).of(
        Case($(Objects::isNull), ""),
        Case($(), h.user().principal().encodePrettily() + "")
      ));
    });


    vertx.createHttpServer().requestHandler(router::accept).listen(9090, ar-> {
      log.info("Vertx Server Started : " + ar.result());
    });
  }

  private void clientLogin(Future<LoginResponse> future, LoginRequest loginRequest) {

    authService.login(loginRequest, loginResponseHandler -> {
      if(loginResponseHandler.failed()){
        loginResponseHandler.cause().printStackTrace();
        future.fail(loginResponseHandler.cause());
      } else {
        future.complete(loginResponseHandler.result());
      }
    });
  }

  private Handler<RoutingContext> handleLogin =
    (routingContext) -> {
      HttpServerResponse response = routingContext.response();
      JsonObject loginForm = routingContext.getBodyAsJson();

      String username = loginForm.getString("username");
      String password = loginForm.getString("password");

      Function2<String, String, Validation<String, String>> isNotEmptyAndAtLeast3 = (name, str) ->
        Validation.combine(isNotEmpty(str), isAtLeast(3, str))
          .ap((valid1, _valid2) -> valid1)
          .fold(error -> Validation.invalid(String.format("%s is invalid : %s", name, error)),
            Validation::valid);

      Validation.combine(isNotEmptyAndAtLeast3.apply("username", username),
        isNotEmptyAndAtLeast3.apply("password", password))
        .ap(Tuple2::new)
        .fold(
          error -> {
            JsonArray errors = new JsonArray();
            error.toStream().map(errors::add);
            response.setStatusCode(400).end(new JsonObject().put("error", errors).encodePrettily());
            return null;},
          success -> {
            authServiceCircuitBreaker.executeWithFallback(
              future -> clientLogin(future, LoginRequest.newBuilder().setUsername(success._1).setPassword(success._2).build()),
              fallback -> loginFallback()
              ).setHandler(loginResponseHandler(response, username));
            return null;
          }
        );
    };

  private Handler<AsyncResult<LoginResponse>> loginResponseHandler(HttpServerResponse response, String username) {
    return (result) -> {

      AsyncResult<LoginResponse> _res = result.otherwise((t)-> LoginResponse.newBuilder().setError(Error.newBuilder().setCode(500).setMessage(t.getMessage()).build()).build());

      JsonObject jsonResponse = Try.of(RoutesUtils.protobufToJson(_res.result())).getOrElse(new JsonObject().put("status", "error - can not translate protobuf to string"));

      // Create tuple of status code and json response
      Tuple2<Integer, JsonObject> resp = Match(jsonResponse.getJsonObject("error")).of(
          Case($(Objects::nonNull), (v) -> new Tuple2<>(v.getInteger("code"), jsonResponse)), // If error then return error
          Case($(Objects::isNull), () -> { // if success (no error), then produce jwt token
            JWTOptions options = new JWTOptions().setIssuer("clearlabs").setPermissions(Arrays.asList("cl_admin", "cl_view")).setSubject(username);
            jsonResponse.put("token", provider.generateToken(new JsonObject().put("custom_data", "anything we want here"), options));
            return new Tuple2<>(200, jsonResponse);
          })
      );

      response.setStatusCode(resp._1).end(resp._2.encodePrettily());
    };
  }

  private LoginResponse loginFallback() {
    return LoginResponse.newBuilder().setError(Error.newBuilder().setCode(503).setMessage("Auth Service is not available at this time (loginFallback).").build()).build();
  }
}
