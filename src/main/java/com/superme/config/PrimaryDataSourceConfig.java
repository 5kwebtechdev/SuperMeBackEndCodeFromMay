package com.superme.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
//
//@Configuration
//@EnableJpaRepositories(basePackages = "com.superme.repository", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
//public class PrimaryDataSourceConfig {
//
//    @Value("${spring.datasource.url}")
//    private String primaryUrl;
//
//    @Value("${spring.datasource.username}")
//    private String primaryUsername;
//
//    @Value("${spring.datasource.password}")
//    private String primaryPassword;
//
//    @Value("${spring.datasource.driver-class-name}")
//    private String primaryDriverClassName;
//
//    @Primary
//    @Bean(name = "dataSource")
//    public DataSource dataSource() {
//        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl(primaryUrl);
//        config.setUsername(primaryUsername);
//        config.setPassword(primaryPassword);
//        config.setDriverClassName(primaryDriverClassName);
//        config.setMaximumPoolSize(20);
//        config.setMinimumIdle(5);
//        config.setConnectionTimeout(20000);
//        config.setIdleTimeout(300000);
//        config.setMaxLifetime(1200000);
//        config.setPoolName("PrimaryHikariPool");
//        return new HikariDataSource(config);
//    }
//
//    @Primary
//    @Bean(name = "entityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
//            @Qualifier("dataSource") DataSource dataSource) {
//
//        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
//        em.setDataSource(dataSource);
//        em.setPackagesToScan("com.superme.model");
//        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
//
//        Properties properties = new Properties();
//        properties.setProperty("hibernate.hbm2ddl.auto", "update");
//        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
//        properties.setProperty("hibernate.show_sql", "true");
//        properties.setProperty("hibernate.format_sql", "true");
//        properties.setProperty("hibernate.jdbc.batch_size", "20");
//        properties.setProperty("hibernate.order_inserts", "true");
//        properties.setProperty("hibernate.order_updates", "true");
//        em.setJpaProperties(properties);
//
//        em.setPersistenceUnitName("primary");
//        return em;
//    }
//
//    @Primary
//    @Bean(name = "transactionManager")
//    public PlatformTransactionManager transactionManager(
//            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
//        return new JpaTransactionManager(entityManagerFactory.getObject());
//    }
//}
@Configuration
@EnableJpaRepositories(
        basePackages = "com.superme.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
public class PrimaryDataSourceConfig {

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.superme.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");

        em.setPersistenceUnitName("primary");
        em.setJpaProperties(props);
        return em;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {

        return new JpaTransactionManager(emf.getObject());
    }
}