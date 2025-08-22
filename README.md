# LepoDiscordBot

---

## _Italiano_

LepoDiscordBot è un bot Discord sviluppato in Java, progettato per funzionare su Raspberry Pi tramite Docker.

### Requisiti

- Raspberry Pi con Docker installato
- Java 11+
- Maven

### Compilazione del jar

Per generare il file `.jar` necessario all'esecuzione, usa il comando:

```bash
mvn clean install -Dspring.profiles.active=<<TUO_PROFILO>>
```

### Build dell'immagine Docker

Costruisci l'immagine Docker con:

```bash
docker build -t lepodiscordbot .
```

> ⚠️ **Attenzione:** Nel `Dockerfile` è necessario inserire la password al posto di `PASSWORD_PLACEHOLDER`.

### Variabili d'ambiente

Assicurati di impostare le seguenti variabili d'ambiente prima di avviare il bot:

- `spring.profiles.active`
- `jasypt.encryptor.password`

Queste variabili sono necessarie per la configurazione e la cifratura dei dati sensibili. Puoi impostarle nell'ambiente
di esecuzione, nel Dockerfile o tramite il compose.

### Creazione del volume dati

Per salvare i dati del bot in modo persistente, crea un volume Docker:

```bash
docker volume create data
```

### Avvio del bot

Avvia il bot in background, collegando il volume appena creato:

```bash
docker run -d -v data:/app/data lepodiscordbot
```

---

## _English_

LepoDiscordBot is a Discord bot written in Java, designed to run on a Raspberry Pi using Docker.

### Requirements

- Raspberry Pi with Docker installed
- Java 11+
- Maven

### Jar compilation

To generate the executable `.jar` file, use:

```bash
mvn clean install -Dspring.profiles.active=<<YOUR_PROFILE>>
```

### Docker image build

Build the Docker image with:

```bash
docker build -t lepodiscordbot .
```

> ⚠️ **Note:** In the `Dockerfile`, you must set your password in place of `PASSWORD_PLACEHOLDER`.

### Environment variables

Make sure to set the following environment variables before starting the bot:

- `spring.profiles.active`
- `jasypt.encryptor.password`

These variables are needed for configuration and sensitive data encryption. You can set them in your runtime
environment, Dockerfile, or via compose.

### Data volume creation

To persist bot data, create a Docker volume:

```bash
docker volume create data
```

### Starting the bot

Run the bot in background, mounting the created volume:

```bash
docker run -d -v data:/app/data lepodiscordbot
```