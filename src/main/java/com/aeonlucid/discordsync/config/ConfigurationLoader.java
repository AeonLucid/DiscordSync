package com.aeonlucid.discordsync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ConfigurationLoader {

    private static final Logger LOGGER = LogManager.getLogger();

    private final File configFile;
    private final Gson gson;

    public ConfigurationLoader() {
        this.configFile = new File("./config/discordsync.json");
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    }

    public Configuration load() {
        Configuration config = new Configuration();

        if (this.configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile);
                 JsonReader reader = new JsonReader(new InputStreamReader(fis))) {
                // Load config from file.
                config = gson.fromJson(reader, Configuration.class);

                // Save file in case new variables were added.
                save(config);
            } catch (Exception e) {
                LOGGER.error("Failed to load configuration file", e);
            }
        } else {
            // Instantiate new config.
            config = new Configuration();

            // Save config file.
            save(config);
        }

        return config;
    }

    public void save(Configuration config) {
        try (FileOutputStream fos = new FileOutputStream(configFile);
             JsonWriter writer = new JsonWriter(new OutputStreamWriter(fos))) {
            writer.setSerializeNulls(true);
            writer.setIndent("  ");
            gson.toJson(config, Configuration.class, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save configuration file", e);
        }
    }

}
