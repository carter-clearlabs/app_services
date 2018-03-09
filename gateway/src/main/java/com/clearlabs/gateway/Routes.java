package com.clearlabs.gateway;

import com.clearlab.services.auth.gen.AuthServiceGrpc;
import com.clearlab.services.auth.gen.Error;
import com.clearlab.services.auth.gen.LoginRequest;
import com.clearlab.services.auth.gen.LoginResponse;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.vavr.API;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.core.Future;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static io.vavr.API.$;
import static io.vavr.API.Case;

@Slf4j
@Component
public class Routes {

  @Autowired
  AuthServiceGrpc.AuthServiceVertxStub authService;

  @Autowired
  Vertx vertx;

  CircuitBreakerOptions options = new CircuitBreakerOptions()
    .setMaxFailures(3) // How many times do we fail til we are in an OPEN state
    .setTimeout(600) // how long until we fail
    .setResetTimeout(5000); // How long to wait til we reset and try again?

  CircuitBreaker loginCB;

  @PostConstruct
  public void startServerAndRoutes(){

    loginCB = CircuitBreaker.create("login", vertx, options);

    Router router = Router.router(vertx);

    // Parse json posts
    router.route().handler(BodyHandler.create());

    // Add CORS support
    router.route().handler(corsHandler());

    // TODO : Add JWT Handler to inspect headers

    // JWTAuthOptions config = new JWTAuthOptions()
    //  .setKeyStore(new KeyStoreOptions()
    //    .setPath("keystore.jceks")
    //    .setPassword("secret"));
    //
    //AuthProvider provider = JWTAuth.create(vertx, config);

    // Handle the login route
    router.post("/login").handler(handleLogin);

    // TODO : Add Routes for addUser

    vertx.createHttpServer().requestHandler(router::accept).listen(9090, ar->{
      log.info("Vertx Server Started : " + ar.result());
    });
  }



  private CorsHandler corsHandler() {
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



  private void clientLogin(Future<LoginResponse> future, String username, String password) {

    authService.login(LoginRequest.newBuilder().setUsername(username).setPassword(password).build(), loginResponseHandler -> {
      if(loginResponseHandler.failed()){
        loginResponseHandler.cause().printStackTrace();
        future.fail(loginResponseHandler.cause());
      } else {
        future.complete(loginResponseHandler.result());
      }
    });
  }

  CheckedFunction0<JsonObject> protobufToJson(MessageOrBuilder msg) {
    return () -> new JsonObject(JsonFormat.printer().print(msg));
  }

  private Handler<AsyncResult<LoginResponse>> handleCircuitBreaker(HttpServerResponse response) {
    return (result) -> {
      if (result.failed()) {
        log.error("Failed Auth Service Call", result.cause());
        response.end(new JsonObject().put("status", "failed").put("error", result.cause().getMessage()).encodePrettily());
      } else {
        JsonObject jsonResponse = Try.of(protobufToJson(result.result())).getOrElse(new JsonObject().put("status", "error - can not translate protobuf to string"));
        Integer statusCode = API.Match(jsonResponse.getJsonObject("error")).of(
          Case($(Objects::nonNull), (v) -> v.getInteger("code")),
          Case($(Objects::isNull), 200),
          Case($(), 200)
        );
        response.setStatusCode(statusCode).end(jsonResponse.encodePrettily());
      }
    };
  }

  private Handler<RoutingContext> handleLogin = (routingContext) -> {

    HttpServerResponse response = routingContext.response();
    JsonObject loginForm = routingContext.getBodyAsJson();

    String username = loginForm.getString("username");
    String password = loginForm.getString("password");

    // TODO : Add some validation to the parameters above

    // Call the CircuitBreaker
    loginCB.executeWithFallback(
      future -> clientLogin(future, username, password),
      fallback -> fallback()
    ).setHandler(handleCircuitBreaker(response));
  };


  private LoginResponse fallback() {
    return LoginResponse.newBuilder().setError(Error.newBuilder().setCode(503).setMessage("Auth Service is not available at this time (fallback).").build()).build();
  }
}
