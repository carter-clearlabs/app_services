package com.clearlabs.services.auth;

import com.clearlabs.services.auth.gen.*;

import com.clearlabs.services.common.gen.Error;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.stereotype.Component;


@Component
@GRpcService(interceptors = { LogInterceptor.class })
@Slf4j
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

  @Override
  public void login(LoginRequest request, StreamObserver<LoginResponse> responseObserver) {
    if(!request.getUsername().equalsIgnoreCase("test") || !request.getPassword().equalsIgnoreCase("test")){
      responseObserver.onNext(LoginResponse.newBuilder().setError(Error.newBuilder().setCode(403).setMessage("Invalid Credentials").build()).build());
    } else {
      responseObserver.onNext(LoginResponse.newBuilder().setToken("some_real_token").build());
    }
    responseObserver.onCompleted();
  }

}
