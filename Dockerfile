FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY backend/spring-boot/pom.xml .
RUN mvn -B dependency:go-offline

COPY backend/spring-boot/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY data-worker/requirements.txt /app/data-worker/requirements.txt
RUN pip3 install --no-cache-dir -r /app/data-worker/requirements.txt

COPY data-worker /app/data-worker
COPY --from=builder /build/target/market-lens-backend-0.1.0-SNAPSHOT.jar /app/app.jar
COPY deploy/render-start.sh /app/render-start.sh
RUN chmod +x /app/render-start.sh

EXPOSE 10000
ENTRYPOINT ["/app/render-start.sh"]
