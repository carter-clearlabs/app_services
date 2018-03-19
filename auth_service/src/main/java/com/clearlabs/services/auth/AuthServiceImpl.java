package com.clearlabs.services.auth;

import com.clearlabs.services.auth.gen.*;

import com.clearlabs.services.common.gen.Error;
import com.clearlabs.services.common.gen.Response;
import com.clearlabs.services.user.gen.UserManagementServiceGrpc;
import com.clearlabs.services.user.gen.VerifyPasswordRequest;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@GRpcService(interceptors = { LogInterceptor.class })
@Slf4j
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {

  @Autowired
  UserManagementServiceGrpc.UserManagementServiceVertxStub userClient;

  @Override
  public void login(LoginRequest request, StreamObserver<Response> responseObserver) {

    userClient.verifyPassword(VerifyPasswordRequest.newBuilder().setPassword(request.getPassword()).setUsername(request.getUsername()).build(), serviceHandler ->{

      if(serviceHandler.failed()) {
        responseObserver.onError(serviceHandler.cause());
        responseObserver.onCompleted();
      } else {
        responseObserver.onNext(serviceHandler.result());
        responseObserver.onCompleted();
      }

    });

  }

}
