echo "Bygger flex-gateway latest"

./gradlew bootJar

docker build . -t flex-gateway:latest
