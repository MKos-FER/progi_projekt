JAR := target/lets-do-sports-backend-0.0.1-SNAPSHOT.jar

# Lokalna baza: podaci se čitaju iz db/.env
DB_ENV_FILE := ../../db/.env
dbval = $$(sed -n 's/^$(1)=//p' $(DB_ENV_FILE))
DB_ENV = SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/$(call dbval,POSTGRES_DB)" \
         SPRING_DATASOURCE_USERNAME="$(call dbval,APP_DB_USER)" \
         SPRING_DATASOURCE_PASSWORD="$(call dbval,APP_DB_PASSWORD)"

.PHONY: run test build run-prod db clean

# Lokalni razvoj
run:
	$(DB_ENV) ./mvnw spring-boot:run

test:
	$(DB_ENV) ./mvnw test

# Produkcijski jar (bez testova)
build:
	./mvnw clean package -DskipTests

run-prod: $(JAR)
	$(DB_ENV) java -jar $(JAR)

$(JAR):
	./mvnw package -DskipTests

# Pokreće bazu i migracije
db:
	cd ../../db && podman compose up -d

clean:
	./mvnw clean
