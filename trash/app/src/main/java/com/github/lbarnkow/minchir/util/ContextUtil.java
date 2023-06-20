package com.github.lbarnkow.minchir.util;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lbarnkow.minchir.handlers.request.AbstractHandler;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public class ContextUtil {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractHandler.class);

  public static String getQueryParam(Context ctx, String name) {
    return getParamOrFail(ctx::queryParam, name);
  }

  public static String getFormParam(Context ctx, String name) {
    return getParamOrFail(ctx::formParam, name);
  }

  public static String getFormParam(Context ctx, String name, String defaultValue) {
    return getParamOrFail(ctx::formParam, name, Optional.of(defaultValue));
  }

  private static String getParamOrFail(Function<String, String> fun, String name) {
    return getParamOrFail(fun, name, Optional.empty());
  }

  private static String getParamOrFail(Function<String, String> fun, String name, Optional<String> defaultValue) {
    var value = fun.apply(name);

    if (value != null) {
      return value;
    }

    return defaultValue.orElseThrow(() -> {
      LOG.error("Mandatory param '{}' not found in requests form/query data!", name);
      throw new BadRequestResponse();
    });
  }
}
