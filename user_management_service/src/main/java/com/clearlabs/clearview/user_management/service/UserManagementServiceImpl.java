package com.clearlabs.clearview.user_management.service;

import com.clearlabs.clearview.user_management.model.User;
import com.clearlabs.clearview.user_management.repository.UserRepository;
import com.clearlabs.services.common.gen.Response;
import com.clearlabs.services.user.gen.*;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@GRpcService
public class UserManagementServiceImpl extends UserManagementServiceGrpc.UserManagementServiceImplBase {
  @Autowired
  UserRepository userRepo;

  @Override
  public void addUser(AddUserRequest request, StreamObserver<Response> responseObserver) {

    // Add any business logic here
    User user = new User();
    user.setLastName(request.getUser().getLastname());
    user.setFirstName(request.getUser().getFirstname());
    user.setEmail(request.getUser().getEmail());

    userRepo.save(user);

    responseObserver.onNext(Response.newBuilder().setStatus("ok").build());
    responseObserver.onCompleted();

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
}
