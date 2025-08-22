#Pi configuration

# Use a Java base image
FROM adoptopenjdk:11-jdk-hotspot

# Copy the .opus files
COPY target/audio/*.opus /app/audio/
COPY target/audio/welcomeAudio/*.opus /app/audio/welcomeAudio/
COPY target/audio/goodbyeAudio/*.opus /app/audio/goodbyeAudio/
COPY target/audio/customAudio/*.opus /app/audio/customAudio/

# Copy the project JAR file LepoDiscordBot into the /app directory inside the container
COPY target/LepoDiscordBot-1.0.0.jar /app/LepoDiscordBot-1.0.0.jar

# Command to start the application
CMD ["java","-Djasypt.encryptor.password=<<PASSWORD_PLACEHOLDER>>","-Dspring.profiles.active=<<PROFILE_PLACEHOLDER>>","-jar","/app/LepoDiscordBot-1.0.0.jar"]


#Local Configuration
# Use a Java base image
#FROM adoptopenjdk:11-jdk-hotspot

# Copy the .opus files
#COPY src/main/resources/audio/*.opus /app/audio/
#COPY src/main/resources/audio/welcomeAudio/*.opus /app/audio/welcomeAudio/
#COPY src/main/resources/audio/welcomeAudio/*.opus /app/audio/goodbyeAudio/
#COPY src/main/resources/audio/customAudio/*.opus /app/audio/customAudio/

# Copy the project JAR file LepoDiscordBot into the /app directory inside the container
#COPY target/LepoDiscordBot-1.0.0.jar /app/LepoDiscordBot-1.0.0.jar

# Copy libconnector.so file in the container
#COPY target/libconnector.so /usr/lib/

# Command to start the application
#CMD ["java","-Djasypt.encryptor.password=<<PASSWORD_PLACEHOLDER>>","-Dspring.profiles.active=docker","-jar","/app/LepoDiscordBot-1.0.0.jar"]