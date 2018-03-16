package com.clearlabs.orb;

import com.clearlabs.services.auth.gen.AuthServiceGrpc;
import com.clearlabs.services.user.gen.UserManagementServiceGrpc;
import io.grpc.ManagedChannel;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
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
  public Integer authServiceClientPort;

  @Value("${auth_service.grpc.host:localhost}")
  public String authServiceClientHost;

  @Bean
  public AuthServiceGrpc.AuthServiceVertxStub authServiceClient(){
    // Extract host/port to config
    final ManagedChannel channel =
      VertxChannelBuilder
        .forAddress(vertx().getDelegate(), authServiceClientHost, authServiceClientPort)
        .usePlaintext(true)
        .build();

    return AuthServiceGrpc.newVertxStub(channel);
  }

  @Value("${user_service.grpc.port:6565}")
  public Integer userServiceClientPort;

  @Value("${user_service.grpc.host:localhost}")
  public String userServiceClientHost;

  @Bean
  public UserManagementServiceGrpc.UserManagementServiceVertxStub userManagementClient(){
    final ManagedChannel channel =
      VertxChannelBuilder
        .forAddress(vertx().getDelegate(), userServiceClientHost, userServiceClientPort)
        .usePlaintext(true)
        .build();

    return UserManagementServiceGrpc.newVertxStub(channel);
  }

  @Bean
  public JWTAuth getJWTProvider(){
    JWTAuthOptions config = new JWTAuthOptions()
        .setKeyStore(new KeyStoreOptions()
            .setPath("keystore.jceks") // manually created and stored with the project - not ideal and should be extracted to docker injected config.
            .setPassword("secret"));

    return JWTAuth.create(vertx(), config);
  }
}
