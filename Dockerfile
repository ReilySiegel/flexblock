FROM clojure:openjdk-8-tools-deps-alpine

RUN apk add --no-cache make npm

COPY . /app
RUN npm config set unsafe-perm true
RUN make init build -C /app

EXPOSE 5000

CMD ["java", "-cp", "/app/flexblock.jar", "clojure.main", "-m", "flexblock.core"]