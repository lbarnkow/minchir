###############################################################################
FROM ghcr.io/graalvm/native-image:22 AS builder

COPY . .

RUN \
    cd app/native-image && \
    native-image \
        --allow-incomplete-classpath \
        --no-fallback \
        --static \
        --libc=glibc \
        --class-path '../build/install/app/lib/*' \
        com.github.lbarnkow.minchir.App \
        minchir

RUN \
    cd app/native-image && \
    ./minchir --help

###############################################################################
FROM scratch AS release

COPY --from=builder /app/app/native-image/minchir /
