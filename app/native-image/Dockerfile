# Dockerfile used for github actions ci multi platform build
FROM scratch

ARG TARGETOS
ARG TARGETARCH

WORKDIR /
USER 1000

COPY minchir-$TARGETOS-$TARGETARCH /minchir
COPY assets /assets/

ENTRYPOINT [ "/minchir" ]
CMD [ \
    "--config", "/assets/config/config.yaml", \
    "--scopes", "/assets/scopes/scopes.yaml", \
    "--translation", "/assets/i18n/translations.yaml" \
]
