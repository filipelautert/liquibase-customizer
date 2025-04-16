package org.liquibase.springboot.demo.customizer;

import com.datical.liquibase.ext.command.FlowCommandStep;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.command.CommandScope;
import liquibase.command.core.helpers.DbUrlConnectionCommandStep;
import liquibase.integration.spring.Customizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

//@Configuration
public class LiquibaseFlowCustomizer<T extends Liquibase> implements Customizer<T> {

    @Value("${liquibase.flowfile}")
    private String flowFile;

    @Value("${liquibase.license_key}")
    private String licenseKey;

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseFlowCustomizer.class);

    //@Override
    public void customize(T liquibase) {
        System.setProperty("liquibaseProLicenseKey", licenseKey);
        try {
            liquibase.listUnrunChangeSets(new Contexts(), new LabelExpression())
                            .forEach(changeSet -> LOG.info("Unrun changeset: {}", changeSet));

            new CommandScope(FlowCommandStep.COMMAND_NAME)
                .addArgumentValue(FlowCommandStep.FLOW_FILE, flowFile)
                .addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, liquibase.getDatabase())
                .execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}