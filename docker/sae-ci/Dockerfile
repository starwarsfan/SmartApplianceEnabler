FROM adoptopenjdk/openjdk11
MAINTAINER Axel Mueller <axel.mueller@avanux.de>

ARG SAE_VERSION=2.3.0

ENV PATH="/usr/bin:${PATH}"
ENV JAVA_OPTS=""
ENV SAE_INSTALL_DIR=/opt/sae
ENV SAE_HOME=/opt/sae/data
ENV SAE_VERSION=${SAE_VERSION}

EXPOSE 8080

RUN echo "Europe/Berlin" > /etc/timezone
RUN cp /usr/share/zoneinfo/Europe/Berlin /etc/localtime

RUN mkdir -p $SAE_INSTALL_DIR

CMD java $JAVA_OPTS -Djava.awt.headless=true -Xmx256m -Dsae.home=$SAE_HOME -Dlogging.config=$SAE_INSTALL_DIR/logback-spring.xml -jar $SAE_INSTALL_DIR/SmartApplianceEnabler.war

COPY run/logback-spring.xml $SAE_INSTALL_DIR/logback-spring.xml
COPY target/SmartApplianceEnabler*.war $SAE_INSTALL_DIR/SmartApplianceEnabler.war
