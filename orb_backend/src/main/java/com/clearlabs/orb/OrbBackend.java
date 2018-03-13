package com.clearlabs.orb;

import com.clearlab.services.auth.gen.AuthServiceGrpc;
import io.grpc.ManagedChannel;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.reactivex.core.Vertx;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class OrbBackend {

  public static void main(String[] args) {
    SpringApplication.run(OrbBackend.class, args);
  }

  @Bean
  public Vertx vertx(){
    return Vertx.vertx();
  }

  @Value("${auth_service.grpc.port:6565}")
  public Integer clientPort;

  @Value("${auth_service.grpc.host:localhost}")
  public String clientHost;

  @Bean
  public AuthServiceGrpc.AuthServiceVertxStub authService(){
    // Extract host/port to config
    final ManagedChannel channel =
      VertxChannelBuilder
        .forAddress(vertx().getDelegate(), clientHost, clientPort)
        .usePlaintext(true)
        .build();

    return AuthServiceGrpc.newVertxStub(channel);
  }

}
