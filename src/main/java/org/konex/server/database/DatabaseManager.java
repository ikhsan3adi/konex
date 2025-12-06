package org.konex.server.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("java:S6548")
public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private MongoClient mongoClient;
    private MongoDatabase database;

    private DatabaseManager() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            String connectionString = dotenv.get("MONGO_URI");
            String dbName = dotenv.get("MONGO_DB_NAME");

            if (connectionString == null || dbName == null) {
                throw new IllegalStateException("Gagal memuat konfigurasi database dari .env");
            }

            this.mongoClient = MongoClients.create(connectionString);
            this.database = mongoClient.getDatabase(dbName);

            this.database.runCommand(new Document("ping", 1));
            LOGGER.info(() -> "Database Connected: " + dbName);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Database Connection Failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static final class InstanceHolder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    public static DatabaseManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}