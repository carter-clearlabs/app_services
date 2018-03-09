package com.clearlabs.services.auth;

import com.clearlab.services.auth.gen.*;
import com.clearlab.services.auth.gen.Error;
import io.grpc.stub.StreamObserver;
import io.vavr.API;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;

@Component
@GRpcService
@Slf4j
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

  @Override
  public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {

    log.info("In Server");

    Callable<LoginResponse> resp = () -> {
      Double randomTime = Math.random() * 750;

      Thread.sleep(randomTime.intValue());

      if(!request.getUsername().equalsIgnoreCase("test") || !request.getPassword().equalsIgnoreCase("test")){
        return LoginResponse.newBuilder().setError(Error.newBuilder().setCode(403).setMessage("Invalid Credentials").build()).build();
      }
      return LoginResponse.newBuilder().setToken("some_real_token").build();
    };

    // Do some work here... have some retry/circuitbreaker/Try......etc
    // Wrap the callable in a Try - with a fallback
    LoginResponse lr = Try.ofCallable(resp).get();

    responseObserver.onNext(lr);
    responseObserver.onCompleted();
  }

  @Override
  public void validateToken(ValidateTokenRequest request, StreamObserver<Response> responseObserver) {
    responseObserver.onNext(Response.newBuilder().setStatus("ok").build());
    responseObserver.onCompleted();
  }
}
