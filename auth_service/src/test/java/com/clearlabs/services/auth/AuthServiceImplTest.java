package com.clearlabs.services.auth;

import com.clearlabs.services.auth.gen.LoginRequest;
import com.clearlabs.services.common.gen.Response;
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
    StreamObserver<Response> resp = new StreamObserver<Response>() {
      @Override
      public void onNext(Response value) {
        assertEquals("Response should not have an error.", false, value.hasError());
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
