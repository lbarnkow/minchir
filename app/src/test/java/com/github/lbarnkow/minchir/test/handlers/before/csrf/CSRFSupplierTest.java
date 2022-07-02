package com.github.lbarnkow.minchir.test.handlers.before.csrf;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.lbarnkow.minchir.handlers.before.csrf.CSRFSupplier;
import com.github.lbarnkow.minchir.test.testutilities.baseclasses.BaseTest;

@BaseTest
public class CSRFSupplierTest {

  private static final long TEST_DATA_TOTP_STEP_SIZE = 300L;

  @ParameterizedTest
  @MethodSource
  public void testEnforceKeyLength(byte[] input, int expectedLength) {
    var output = CSRFSupplier.enforceKeyLength(input);

    assertThat(output).hasSize(expectedLength);
  }

  private static Stream<Arguments> testEnforceKeyLength() {
    var min = CSRFSupplier.MIN_KEY_LENGTH_BITS / 8;
    var max = CSRFSupplier.MAX_KEY_LENGTH_BITS / 8;

    return Stream.of( //
        Arguments.of(random(1), min), //
        Arguments.of(random(min - 1), 2 * (min - 1)), //
        Arguments.of(random(min), min), //
        Arguments.of(random(min + 1), min + 1), //
        Arguments.of(random(max - 1), max - 1), //
        Arguments.of(random(max), max), //
        Arguments.of(random(max + 1), max), //
        Arguments.of(random(1024), max) //
    );
  }

  private static byte[] random(int bytes) {
    byte[] result = new byte[bytes];
    new Random().nextBytes(result);
    return result;
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testGenerate(String base64TotpKey, String base64HmacKey, long instant, String tokenCurrent,
      String cookieCurrent, String tokenPrevious, String cookiePrevious)
      throws InvalidKeyException, NoSuchAlgorithmException {

    var supplier = new CSRFSupplier(TEST_DATA_TOTP_STEP_SIZE, base64TotpKey, base64HmacKey);

    var csrfCurrent = supplier.generate(Instant.ofEpochSecond(instant));
    var csrfPrevious = supplier.generate(Instant.ofEpochSecond(instant).minus(TEST_DATA_TOTP_STEP_SIZE, SECONDS));

    assertThat(csrfCurrent.getToken()).isEqualTo(tokenCurrent);
    assertThat(csrfCurrent.getCookie()).isEqualTo(cookieCurrent);
    assertThat(csrfPrevious.getToken()).isEqualTo(tokenPrevious);
    assertThat(csrfPrevious.getCookie()).isEqualTo(cookiePrevious);
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testVerify(String base64TotpKey, String base64HmacKey, long instant, String tokenCurrent,
      String cookieCurrent, String tokenPrevious, String cookiePrevious)
      throws InvalidKeyException, NoSuchAlgorithmException {

    var supplier = new CSRFSupplier(TEST_DATA_TOTP_STEP_SIZE, base64TotpKey, base64HmacKey);

    supplier.verify(tokenCurrent, cookieCurrent, Instant.ofEpochSecond(instant));
    supplier.verify(tokenPrevious, cookiePrevious, Instant.ofEpochSecond(instant));
  }

  private static Stream<Arguments> data() {
    return Stream.of( //
        Arguments.of( //
            "Ho13ZcxELGDUeFSmccpIo3j04JBJ7Wz+0Y/5mqucuAogqushZy4CH52/JxgSBKn32AKgEpjEw+/eAISY1RB6VA==", //
            "/30bGfwuART3TtZkOoI3mwzyVHeveUFrR6ZK7Xo0DOdRgl35IdtAK+SXdrRLFfqmE+7HOI1Q1kjhCUExwcyJUw==", //
            801231316L, //
            "48030610", //
            "IQsKy13Nv6CdDZZliSx1L+DSziPelNDON19xvdBdfe3g9MPzvpC+lUz5dgJEGdYp3B8WdrIHG3RhaTLsq3DAHw==", //
            "61179155", //
            "DTigOIYIohoGtRmA9Nd97qDn3D6h6upOEIIHlTD2YcplqG0Oi8hLFTq7YjhvprsdrWpDkMZcd0jPEuLQZ4K2Dw==" //
        ), //
        Arguments.of( //
            "2ro8HVsvmbTV9mNz/uT8FqwpQ/++0ZEgNgSFNY+hC2HZQQR4HPK0/PfWnSiUx1w+DLudap6NK73MwqFlXYK/ZA==", //
            "2b4flSC3tGMyt/bitQpLjnSX4MgefMP6NqCqfSPTEbZmzx0asSgi5eGOexFCqGVxPvESJE4qM+64RYS5qKgDvg==", //
            1116591316L, //
            "57228566", //
            "VE0kAQY7Xw0KeoLmAEbTU1k5gqzcVQhyEtfCf17ukRpUH/m9gZpjnEgfzJjk7ZYYHZpxFZ3ZlKPvhk/TR7QWQg==", //
            "28797404", //
            "m6YDd4/7lJKUd8aBU1Z91wiy8wfqkBAj+t+OT/AjLSkajVbCHjFX97trhED5BuizWfO2QuomkU6QKeKdcQ1lNw==" //
        ), //
        Arguments.of( //
            "k1r9yw2J1HZ4O2MMaUzCrQKuPR+dUDmb7gPksEpHeYpo5ndKCYqrBn9l/PvBtZ7mWVhJxxbpeG3LIJfvIy4+PQ==", //
            "Djuhl1U/e+vB3E84Vibbg38B/NTlkpteb4DhCKunkifKIOWKPsLuxbSer/bAAmCLJ2uJVFxEVuNIL4wvUY2ujg==", //
            1431951316L, //
            "44586690", //
            "e0tvSZrMBlTS/owR3Ba4rk7qg2mpBdhY/ht3tXWXMdvu79O471M6cQr7y4LFv6EXUwVSfBH3BIYRVEhdYoj7Hg==", //
            "45287569", //
            "fRNNtJIdtmr1yCizdH63jFU/SqFpmePS4vGzk2x3fLTLGaUvJnYHr/tNDdzDvuKjmBQaTy9wcB1LBuEygcPhdA==" //
        ) //
    );
  }
}
