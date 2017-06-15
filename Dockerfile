# Notice! Don't use openjdk:latest docker image, it's too big and will build fail in docker:DinD

FROM openjdk:alpine

MAINTAINER cookeem@qq.com

RUN mkdir -p /root/cookim/

ADD target/scala-2.11/CookIM-assembly-0.2.0-SNAPSHOT.jar /root/cookim/cookim.jar
ADD conf /root/cookim/conf
ADD www /root/cookim/www

WORKDIR /root/cookim

RUN echo '#!/bin/ash' >> /root/cookim/run.sh
RUN echo 'java -classpath "/root/cookim/cookim.jar" com.cookeem.chat.CookIM -n -h $HOST_NAME -w $WEB_PORT -a $AKKA_PORT -s $SEED_NODES' >> /root/cookim/run.sh
RUN chmod a+x /root/cookim/run.sh

CMD [ "/root/cookim/run.sh" ]

# sbt clean assembly
# docker build -t cookeem/cookim .