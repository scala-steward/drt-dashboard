FROM openjdk:24-ea-17-slim-bookworm as stage0
LABEL snp-multi-stage="intermediate"
LABEL snp-multi-stage-id="9af2938f-6807-405c-bf91-02e7c8cbd4b5"
WORKDIR /opt/docker
COPY target/docker/stage/2/opt /2/opt
COPY target/docker/stage/4/opt /4/opt
USER root
RUN ["chmod", "-R", "u=rX,g=rX", "/2/opt/docker"]
RUN ["chmod", "-R", "u=rX,g=rX", "/4/opt/docker"]
RUN ["chmod", "u+x,g+x", "/4/opt/docker/bin/drt-dashboard"]

FROM openjdk:24-ea-17-slim-bookworm as mainstage
USER root
RUN id -u drt 1>/dev/null 2>&1 || (( getent group 0 1>/dev/null 2>&1 || ( type groupadd 1>/dev/null 2>&1 && groupadd -g 0 root || addgroup -g 0 -S root )) && ( type useradd 1>/dev/null 2>&1 && useradd --system --create-home --uid 1001 --gid 0 drt || adduser -S -u 1001 -G root drt ))
WORKDIR /opt/docker
COPY --from=stage0 --chown=drt:root /2/opt/docker /opt/docker
COPY --from=stage0 --chown=drt:root /4/opt/docker /opt/docker

RUN mkdir -p /var/data
RUN chown 1001:1001 -R /var/data

RUN apt-get update
RUN apt-get install -y curl

RUN mkdir /home/drt/.postgresql
RUN curl https://truststore.pki.rds.amazonaws.com/eu-west-2/eu-west-2-bundle.pem > /home/drt/.postgresql/root.crt

EXPOSE 8081
USER 1001:0
ENTRYPOINT ["/opt/docker/bin/drt-dashboard"]
CMD []
