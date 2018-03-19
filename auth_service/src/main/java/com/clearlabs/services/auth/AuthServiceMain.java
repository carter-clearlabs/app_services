package com.clearlabs.services.auth;

import com.clearlabs.services.user.gen.UserManagementServiceGrpc;
import io.grpc.ManagedChannel;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class AuthServiceMain {

  public static void main(String[] args) {
    SpringApplication.run(AuthServiceMain.class, args);
  }

  @Bean
  public Vertx vertx(){
    return Vertx.vertx();
  }

  @Value("${user_service.grpc.port:6566}")
  public Integer userServiceClientPort;

  @Value("${user_service.grpc.host:localhost}")
  public String userServiceClientHost;

  @Bean
  public UserManagementServiceGrpc.UserManagementServiceVertxStub userClient(){
    final ManagedChannel channel =
      VertxChannelBuilder
        .forAddress(vertx(), userServiceClientHost, userServiceClientPort)
        .usePlaintext(true)
        .build();

    return UserManagementServiceGrpc.newVertxStub(channel);
  }

}
