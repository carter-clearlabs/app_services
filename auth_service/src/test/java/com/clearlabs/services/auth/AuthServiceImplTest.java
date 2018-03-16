package com.clearlabs.services.auth;

import com.clearlabs.services.auth.gen.LoginRequest;
import com.clearlabs.services.auth.gen.LoginResponse;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AuthServiceImplTest {
  @Autowired
  AuthServiceImpl authService;

  @Test
  public void testLogin(){
    StreamObserver<LoginResponse> resp = new StreamObserver<LoginResponse>() {
      @Override
      public void onNext(LoginResponse value) {
        assertEquals("Token does not match expectations.", "some_real_token", value.getToken());
      }
      @Override
      public void onError(Throwable t) {
        fail(t.getMessage());
      }
      @Override
      public void onCompleted() {}
    };
    authService.login(LoginRequest.newBuilder().setPassword("test").setUsername("test").build(), resp);
  }

}
