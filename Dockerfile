FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copie des fichiers de configuration
COPY pom.xml .

# Téléchargement des dépendances
RUN mvn dependency:go-offline

# Copie du code source
COPY src ./src

# Compilation (crée le fichier Light_Tron_Cycle_2D-1.0-SNAPSHOT.jar dans /app/target)
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# On le renomme "server.jar" uniquement DANS le conteneur pour simplifier la commande suivante
COPY --from=build /app/target/Light_Tron_Cycle_2D-1.0-SNAPSHOT.jar server.jar

# On expose le port
EXPOSE 2222

# On lance le serveur
ENTRYPOINT ["java", "-jar", "server.jar", "server"]

CMD ["-p", "2222"]