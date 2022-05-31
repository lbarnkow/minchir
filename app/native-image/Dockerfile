# Dockerfile used for github actions ci multi platform build
FROM scratch

ARG TARGETOS
ARG TARGETARCH

WORKDIR /
USER 1000

COPY minchir-$TARGETOS-$TARGETARCH /minchir
COPY assets /assets/

ENTRYPOINT [ "/minchir" ]
CMD [ "--help" ]
