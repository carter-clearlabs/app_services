package com.clearlabs.clearview.user_management.service;

import com.clearlabs.clearview.user_management.model.User;
import com.clearlabs.clearview.user_management.repository.UserRepository;
import com.clearlabs.services.common.gen.Response;
import com.clearlabs.services.user.gen.*;
import io.grpc.stub.StreamObserver;
import io.vavr.control.Try;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.clearlabs.clearview.user_management.utils.BCryptPasswordDeserializer.generatePasswordHash;
import static com.clearlabs.clearview.user_management.utils.BCryptPasswordDeserializer.validatePassword;

@GRpcService
public class UserManagementServiceImpl extends UserManagementServiceGrpc.UserManagementServiceImplBase {
  @Autowired
  UserRepository userRepo;

  @Override
  public void addUser(AddUserRequest request, StreamObserver<Response> responseObserver) {

    // Add any business logic here
    Try.of(() -> generatePasswordHash(request.getPassword()))
        .onSuccess(hashedpw -> {
          User user = new User();
          user.setLastName(request.getLastname());
          user.setFirstName(request.getFirstname());
          user.setEmail(request.getEmail());
          user.setPasswordHash(hashedpw);
          userRepo.save(user);

          responseObserver.onNext(Response.newBuilder().setStatus("ok").build());
          responseObserver.onCompleted();
        })
        .onFailure( f -> {
          responseObserver.onError(f);
          responseObserver.onCompleted();
        });
  }

  @Override
  public void findAllUsers(AllUserRequest request, StreamObserver<UserListResponse> responseObserver) {

    List<com.clearlabs.services.user.gen.User> users = new ArrayList<>();

    userRepo.findAll().forEach(u -> users.add(
        com.clearlabs.services.user.gen.User
            .newBuilder()
            .setEmail(u.getEmail())
            .setLastname(u.getLastName())
            .setFirstname(u.getFirstName())
            .build()));

    responseObserver.onNext(UserListResponse.newBuilder().addAllUsers(users).build());
    responseObserver.onCompleted();
  }

  @Override
  public void verifyPassword(VerifyPasswordRequest request, StreamObserver<Response> responseObserver) {
    List<User> users = userRepo.findByEmail(request.getUsername());
    if(users.size() == 0){
      responseObserver.onError(new IllegalArgumentException("More than one user with this email."));
      responseObserver.onCompleted();
    }
    else if(users.size() > 1){
      responseObserver.onError(new IllegalArgumentException("More than one user with this email."));
      responseObserver.onCompleted();
    }
    else if(!validatePassword(request.getPassword(), users.get(0).getPasswordHash())){
      // Log fail attempts etc..
      responseObserver.onError(new IllegalArgumentException("Incorrect Password"));
      responseObserver.onCompleted();
    } else {
      // All good
      responseObserver.onNext(Response.newBuilder().setStatus("ok").build());
      responseObserver.onCompleted();
    }
  }
}
