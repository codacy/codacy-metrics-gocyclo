FROM amazoncorretto:8-alpine3.14-jre

ENV GOPATH /go
ENV PATH /go/bin:$PATH

RUN apk add --no-cache bash musl-dev go git && \
    go get github.com/fzipp/gocyclo/cmd/gocyclo && \
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
