package com.clearlabs.orb;

import com.clearlab.services.auth.gen.AuthServiceGrpc;
import com.clearlab.services.auth.gen.Error;
import com.clearlab.services.auth.gen.LoginRequest;
import com.clearlab.services.auth.gen.LoginResponse;
import io.vavr.control.Try;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

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

  CircuitBreaker authServiceCircuitBreaker;

  @PostConstruct
  public void startServerAndRoutes(){

    authServiceCircuitBreaker = CircuitBreaker.create("login", vertx, options);

    Router router = Router.router(vertx);

    // Parse json posts
    router.route().handler(BodyHandler.create());

    // Add CORS support
    router.route().handler(RoutesUtils.corsHandler());

    JWTAuthOptions config = new JWTAuthOptions()
        .setKeyStore(new KeyStoreOptions()
            .setPath("keystore.jceks")
            .setPassword("secret"));

    JWTAuth provider = JWTAuth.create(vertx, config);
//    JWTAuth

    // Handle the login route
    router.post("/login").handler(handleLogin(provider));

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

  private Handler<RoutingContext> handleLogin(JWTAuth provider) {
    return (routingContext) -> {
      HttpServerResponse response = routingContext.response();
      JsonObject loginForm = routingContext.getBodyAsJson();

      String username = loginForm.getString("username");
      String password = loginForm.getString("password");

      // TODO : Add some validation to the parameters above
      // Call the CircuitBreaker
      authServiceCircuitBreaker.executeWithFallback(
        future -> clientLogin(future, username, password),
        fallback -> fallback()
      ).setHandler(handleCircuitBreaker(response, username, provider));
    };
  };

  private Handler<AsyncResult<LoginResponse>> handleCircuitBreaker(HttpServerResponse response, String username, JWTAuth provider) {
    return (result) -> {
      if (result.failed()) {
        log.error("Failed Service Call", result.cause());
        // TODO : Consider mimicking the same Error format as the protobuf message
        response.setStatusCode(500).end(new JsonObject().put("status", "failed").put("error", result.cause().getMessage()).encodePrettily());
      } else {
        JsonObject jsonResponse = Try.of(RoutesUtils.protobufToJson(result.result())).getOrElse(new JsonObject().put("status", "error - can not translate protobuf to string"));
        // Extract the error code from the LoginResponse if there is one, otherwise its a 200
        Integer statusCode = Match(jsonResponse.getJsonObject("error")).of(
            Case($(Objects::nonNull), (v) -> v.getInteger("code")),
            Case($(Objects::isNull), 200)
        );
        if(statusCode == 200){
          JWTOptions options = new JWTOptions().setIssuer("clearlabs").setPermissions(Arrays.asList("cl_admin", "cl_view")).setSubject(username);
          jsonResponse.put("token", provider.generateToken(new JsonObject().put("custom_data", "anything we want here"), options));
          response.setStatusCode(statusCode).end(jsonResponse.encodePrettily());
        } else {
          response.setStatusCode(statusCode).end(jsonResponse.encodePrettily());
        }


      }
    };
  }

  private LoginResponse fallback() {
    return LoginResponse.newBuilder().setError(Error.newBuilder().setCode(503).setMessage("Auth Service is not available at this time (fallback).").build()).build();
  }
}