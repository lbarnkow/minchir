package com.github.lbarnkow.minchir.handlers.before.csrf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;

import lombok.Data;

public class CSRFSupplier {

  public static final Encoder BASE64_E = Base64.getEncoder();
  public static final Decoder BASE64_D = Base64.getDecoder();

  public static final String ALGORITHM = "HmacSHA512";
  public static final int TOTP_LENGTH = 8;

  // see https://security.stackexchange.com/a/96176
  public static final int MIN_KEY_LENGTH_BITS = 512;
  public static final int MAX_KEY_LENGTH_BITS = 1024;

  private final long totpStepSize;
  private final SecretKeySpec totpKey;
  private final TimeBasedOneTimePasswordGenerator totp;

  private final SecretKeySpec hmacKey;
  private final Mac hmac;

  public CSRFSupplier(long totpStepSize, String totpKeyBase64, String hmacKeyBase64)
      throws InvalidKeyException, NoSuchAlgorithmException {
    this( //
        totpStepSize, //
        Base64.getDecoder().decode(totpKeyBase64), //
        Base64.getDecoder().decode(hmacKeyBase64) //
    );
  }

  public CSRFSupplier(long totpStepSize, byte[] totpKeyBytes, byte[] hmacKeyBytes)
      throws NoSuchAlgorithmException, InvalidKeyException {
    byte[] sanitizedTotpKeyBytes = enforceKeyLength(totpKeyBytes);
    byte[] sanitizedHmacKeyBytes = enforceKeyLength(hmacKeyBytes);

    this.totpStepSize = totpStepSize;
    totpKey = new SecretKeySpec(sanitizedTotpKeyBytes, ALGORITHM);
    totp = new TimeBasedOneTimePasswordGenerator(ofSeconds(totpStepSize), TOTP_LENGTH, ALGORITHM);
    totp.generateOneTimePassword(totpKey, Instant.now());

    hmacKey = new SecretKeySpec(sanitizedHmacKeyBytes, ALGORITHM);
    hmac = Mac.getInstance(ALGORITHM);
    hmac.init(hmacKey);
  }

  public CSRFData generate() throws InvalidKeyException {
    return generate(Instant.now());
  }

  public CSRFData generate(Instant instant) throws InvalidKeyException {
    var token = String.valueOf(totp.generateOneTimePassword(totpKey, instant));
    var cookie = new String(BASE64_E.encode(hmac.doFinal(token.getBytes(UTF_8))), UTF_8);

    return new CSRFData(cookie, token);
  }

  public void verify(String token, String cookie) throws InvalidKeyException {
    verify(token, cookie, Instant.now());
  }

  public void verify(String token, String cookie, Instant instant) throws InvalidKeyException {
    var csrf = new CSRFData(cookie, token);
    var expected1 = generate(instant);
    var expected2 = generate(instant.minus(totpStepSize, SECONDS));

    if (!expected1.equals(csrf) && !expected2.equals(csrf)) {
      throw new RuntimeException("Bad csrf token or signature!");
    }
  }

  public static byte[] enforceKeyLength(byte[] keyBytes) {
    var minSize = MIN_KEY_LENGTH_BITS / 8;
    var maxSize = MAX_KEY_LENGTH_BITS / 8;

    if (keyBytes.length >= minSize && keyBytes.length <= maxSize) {
      return keyBytes.clone();
    }

    ByteBuffer buffer = ByteBuffer.allocate(maxSize);
    while (buffer.position() < minSize) {
      buffer.put(keyBytes, 0, Math.min(keyBytes.length, buffer.limit() - buffer.position()));
    }

    return Arrays.copyOf(buffer.array(), buffer.position());
  }

  public static String generateKey() {
    var random = new SecureRandom();

    var key = new byte[MIN_KEY_LENGTH_BITS / 8];
    random.nextBytes(key);

    return new String(BASE64_E.encode(key), UTF_8);
  }

  @Data
  public static class CSRFData {
    private final String cookie;
    private final String token;
  }
}
