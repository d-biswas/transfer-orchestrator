package com.company.orchestrator.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"com.company.orchestrator.infrastructure.persistence.repository"})
public class DatabaseConfig{

    private static final String DEFAULT_DRIVER = "org.postgresql.Driver";

    private final Environment environment;

    public DatabaseConfig (Environment environment) {
        this.environment = environment;
    }


    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(environment.getProperty("spring.datasource.url"));
        config.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name", DEFAULT_DRIVER));
        config.setUsername(environment.getProperty("spring.datasource.username"));
        config.setPassword(environment.getProperty("spring.datasource.password"));

        // Hikari pool settings
        config.setMaximumPoolSize(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 10));
        config.setMinimumIdle(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 5));
        config.setConnectionTimeout(environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 10000L));
        config.setIdleTimeout(environment.getProperty("spring.datasource.hikari.idle-timeout", Long.class, 600000L));
        config.setMaxLifetime(environment.getProperty("spring.datasource.hikari.max-lifetime", Long.class, 1800000L));
        config.setLeakDetectionThreshold(environment.getProperty("spring.datasource.hikari.leak-detection-threshold", Long.class, 60000L));
        config.setValidationTimeout(environment.getProperty("spring.datasource.hikari.validation-timeout", Long.class, 2000L));
        config.setPoolName("PrimaryWriteHikariCP");

        return new HikariDataSource(config);
    }

    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
        bean.setDataSource(dataSource());
        bean.setPackagesToScan("com.company.orchestrator.infrastructure.persistence.entity");
        configureJpa(bean);
        return bean;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager manager = new JpaTransactionManager();
        manager.setEntityManagerFactory(entityManagerFactory().getObject());
        return manager;
    }


    private void configureJpa(LocalContainerEntityManagerFactoryBean bean) {
        JpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        bean.setJpaVendorAdapter(adapter);

        Map<String, String> props = new HashMap<>();
        props.put("hibernate.dialect", environment.getProperty("spring.jpa.properties.hibernate.dialect",
                environment.getProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect")));
        props.put("hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto", "validate"));
        props.put("hibernate.show_sql", environment.getProperty("spring.jpa.show-sql", "false"));
        props.put("hibernate.format_sql", environment.getProperty("spring.jpa.properties.hibernate.format_sql", "false"));
        props.put("hibernate.jdbc.fetch_size", environment.getProperty("spring.jpa.properties.hibernate.jdbc.fetch_size", "50"));
        props.put("hibernate.jdbc.batch_size", environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "25"));
        props.put("hibernate.default_batch_fetch_size", environment.getProperty("spring.jpa.properties.hibernate.default_batch_fetch_size", "10"));
        props.put("hibernate.order_inserts", environment.getProperty("spring.jpa.properties.hibernate.order_inserts", "true"));
        props.put("hibernate.order_updates", environment.getProperty("spring.jpa.properties.hibernate.order_updates", "true"));
        props.put("hibernate.globally_quoted_identifiers",
                environment.getProperty("spring.jpa.properties.hibernate.globally_quoted_identifiers", "false"));

        // Add default_schema if configured
        String defaultSchema = environment.getProperty("spring.jpa.properties.hibernate.default_schema");
        if (defaultSchema != null && !defaultSchema.isEmpty()) {
            props.put("hibernate.default_schema", defaultSchema);
        }

        bean.setJpaPropertyMap(props);
    }
}
