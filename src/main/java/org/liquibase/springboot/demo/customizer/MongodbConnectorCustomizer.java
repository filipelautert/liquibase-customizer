package org.liquibase.springboot.demo.customizer;

import jakarta.annotation.PostConstruct;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.ext.mongodb.database.MongoClientDriver;
import liquibase.ext.mongodb.database.MongoConnection;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.nosql.executor.NoSqlExecutor;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MongodbConnectorCustomizer {

    private static final Logger log = LoggerFactory.getLogger(MongodbConnectorCustomizer.class);

    @Value("${liquibase.license_key}")
    private String licenseKey;

    @PostConstruct
    public void applyLiquibaseChanges() {
        System.setProperty("liquibaseProLicenseKey", licenseKey);
        var dbName = "devDb";
        try {

            MongoLiquibaseDatabase mongoLiquibaseDatabase = new MongoLiquibaseDatabase();

            final MongoConnection mongoConnection = new MongoConnection();
            mongoConnection.open("mongodb://localhost:27017/" + dbName, new MongoClientDriver(), null);
            mongoLiquibaseDatabase.setConnection(mongoConnection);
            mongoLiquibaseDatabase.checkDatabaseConnection();

            var noSqlExecutor = new NoSqlExecutor();
            noSqlExecutor.setDatabase(mongoLiquibaseDatabase);
            noSqlExecutor.setResourceAccessor(new ClassLoaderResourceAccessor());
            //Scope.getCurrentScope().getSingleton(ExecutorService.class).setExecutor(mongoLiquibaseDatabase, noSqlExecutor);

            Liquibase liquibase = new Liquibase("db/changelog/db.changelog-mongo.xml",
                    new ClassLoaderResourceAccessor(), mongoLiquibaseDatabase);
            log.info("Liqiubase change log file {}", liquibase.getChangeLogFile());
            liquibase.listUnrunChangeSets(null, null);

            List<ChangeSet> unrunChangeStes = liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression());

            if (!unrunChangeStes.isEmpty()) {
                log.info("LiquibaseMongoDBService: number of unrun changes {}", unrunChangeStes.size());

                unrunChangeStes.forEach(changeSet -> log.info("Unrun changeset: {}", changeSet));
                log.info("Schema: {}", mongoLiquibaseDatabase.getSystemSchema());
                liquibase.update(new Contexts(), new LabelExpression());

                log.info("LiquibaseMongoDBService: Applied Liquibase changes for {} ", dbName);
            } else {
                log.info("LiquibaseMongoDBService: No new Liquibase changes  detected for {} ", dbName);

            }

        } catch (Exception e) {
            log.error("LiquibaseMongoDBService: Exception occurred while applying changes for {}", dbName);
            throw new RuntimeException(e);
        }
    }
}
