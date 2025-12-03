package org.konex.server.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static DatabaseManager instance;

    private MongoClient mongoClient;
    private MongoDatabase database;

    private DatabaseManager() {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

            String connectionString = dotenv.get("MONGO_URI");
            String dbName = dotenv.get("MONGO_DB_NAME");

            if (connectionString == null || dbName == null) {
                throw new RuntimeException("Gagal memuat konfigurasi database dari .env");
            }

            this.mongoClient = MongoClients.create(connectionString);
            this.database = mongoClient.getDatabase(dbName);

            this.database.runCommand(new Document("ping", 1));
            LOGGER.info("Database Connected: " + dbName);

        } catch (Exception e) {
            System.err.println("Database Connection Failed: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Database Connection Failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}