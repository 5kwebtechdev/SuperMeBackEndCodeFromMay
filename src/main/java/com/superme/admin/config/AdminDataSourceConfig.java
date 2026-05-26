package com.superme.admin.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import java.util.Properties;
//
//@Configuration
//@EnableJpaRepositories(basePackages = "com.superme.admin.repository", entityManagerFactoryRef = "adminEntityManagerFactory", transactionManagerRef = "adminTransactionManager")
//public class AdminDataSourceConfig {
//
//    @Value("${spring.admin.datasource.url}")
//    private String adminUrl;
//
//    @Value("${spring.admin.datasource.username}")
//    private String adminUsername;
//
//    @Value("${spring.admin.datasource.password}")
//    private String adminPassword;
//
//    @Value("${spring.admin.datasource.driver-class-name}")
//    private String adminDriverClassName;
//
//    @Bean(name = "adminDataSource")
//    public DataSource adminDataSource() {
//        HikariConfig config = new HikariConfig();
//        config.setJdbcUrl(adminUrl);
//        config.setUsername(adminUsername);
//        config.setPassword(adminPassword);
//        config.setDriverClassName(adminDriverClassName);
//        config.setMaximumPoolSize(10);
//        config.setMinimumIdle(2);
//        config.setConnectionTimeout(20000);
//        config.setIdleTimeout(300000);
//        config.setMaxLifetime(1200000);
//        config.setPoolName("AdminHikariPool");
//        return new HikariDataSource(config);
//    }
//
//    @Bean(name = "adminEntityManagerFactory")
//    public LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(
//            @Qualifier("adminDataSource") DataSource dataSource) {
//
//        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
//        em.setDataSource(dataSource);
//        em.setPackagesToScan("com.superme.admin.model");
//        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
//
//        Properties properties = new Properties();
//        properties.setProperty("hibernate.hbm2ddl.auto", "update");
//        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
//        properties.setProperty("hibernate.show_sql", "true");
//        properties.setProperty("hibernate.format_sql", "true");
//        em.setJpaProperties(properties);
//
//        em.setPersistenceUnitName("admin");
//        return em;
//    }
//
//    @Bean(name = "adminTransactionManager")
//    public PlatformTransactionManager adminTransactionManager(
//            @Qualifier("adminEntityManagerFactory") LocalContainerEntityManagerFactoryBean adminEntityManagerFactory) {
//        JpaTransactionManager transactionManager = new JpaTransactionManager();
//        transactionManager.setEntityManagerFactory(adminEntityManagerFactory.getObject());
//        return transactionManager;
//    }
//}
@Configuration
@EnableJpaRepositories(
        basePackages = "com.superme.admin.repository",
        entityManagerFactoryRef = "adminEntityManagerFactory",
        transactionManagerRef = "adminTransactionManager"
)
public class AdminDataSourceConfig {

    @Bean(name = "adminDataSource")
    @ConfigurationProperties(prefix = "spring.admin.datasource")
    public DataSource adminDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "adminEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean adminEntityManagerFactory(
            @Qualifier("adminDataSource") DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.superme.admin.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");

        em.setPersistenceUnitName("admin");
        em.setJpaProperties(props);
        return em;
    }

    @Bean(name = "adminTransactionManager")
    public PlatformTransactionManager adminTransactionManager(
            @Qualifier("adminEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {

        return new JpaTransactionManager(emf.getObject());
    }
}