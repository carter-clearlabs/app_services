package com.clearlabs.clearview.user_management.utils;

import java.io.IOException;

import io.vavr.CheckedFunction1;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class BCryptPasswordDeserializer extends JsonDeserializer<String> {

  public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    ObjectCodec oc = jsonParser.getCodec();
    JsonNode node = oc.readTree(jsonParser);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    return encoder.encode(node.asText());
  }

  public static String generatePasswordHash(String pw) {
    return BCrypt.hashpw(pw, BCrypt.gensalt(12));
  }

  public static Boolean validatePassword(String pw, String hashedPW) {
    return BCrypt.checkpw(pw, hashedPW);
  }

}
