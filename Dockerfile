FROM navikt/java:14
COPY build/libs/app.jar /app/

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dhttps.proxyHost=webproxy-nais.nav.no \
               -Dhttps.proxyPort=8088 \
               -Dhttp.nonProxyHosts=*.adeo.no|*.preprod.local"
