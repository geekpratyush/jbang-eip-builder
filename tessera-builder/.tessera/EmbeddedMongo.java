package com.tessera.simulator;

import org.apache.camel.BindToRegistry;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedMongo {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedMongo.class);

    @BindToRegistry("mongoClient")
    public MongoClient mongoClient() {
        LOG.info("Starting Native Flapdoodle Embedded MongoDB 6.0...");
        
        // Force Flapdoodle platform override only on Linux (e.g. Linux Mint -> Ubuntu) to guarantee binary resolution.
        // On Windows, let the library auto-detect so it downloads the correct Windows binary.
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("linux")) {
            System.setProperty("de.flapdoodle.os.override", "Linux|X86_64|Ubuntu|Ubuntu_22_04");
        }

        TransitionWalker.ReachedState<RunningMongodProcess> running = Mongod.instance().start(Version.Main.V6_0);
        int port = running.current().getServerAddress().getPort();
        LOG.info("Embedded MongoDB successfully started on port " + port + "!");
        return MongoClients.create("mongodb://localhost:" + port);
    }
}
