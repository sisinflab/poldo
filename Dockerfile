FROM glassfish:latest

COPY target/poldo-1.0-SNAPSHOT.war /
COPY start.sh /

RUN chmod +x /start.sh

EXPOSE 8080

ENTRYPOINT ["/start.sh"]