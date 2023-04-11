FROM amazoncorretto:8-alpine3.17-jre

ENV GOPATH /go
ENV PATH /go/bin:$PATH

RUN apk add --no-cache bash musl-dev go git && \
    go install github.com/fzipp/gocyclo/cmd/gocyclo@latest && \
    apk del musl-dev git && \
    rm -rf /tmp/* && \
    rm -rf /var/cache/apk/* && \
    adduser -u 2004 -D docker

COPY docs /docs

WORKDIR /workdir

COPY target/universal/stage .
RUN chmod +x bin/codacy-metrics-gocyclo

USER docker

ENTRYPOINT [ "bin/codacy-metrics-gocyclo" ]
