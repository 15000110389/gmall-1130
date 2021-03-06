package com.atguigu.gmall.sms.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {


    /**
     * 需要将 DataSourceProxy 设置为主数据源，否则事务无法回滚
     *

     * @return The default datasource
     */
    @Primary
    @Bean("dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource(
            @Value("${spring.datasource.url}")String url,
            @Value("${spring.datasource.driver-class-name}")String driverclassname,
            @Value("${spring.datasource.username}")String username,
            @Value("${spring.datasource.password}")String password
    ) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(password);
        hikariDataSource.setDriverClassName(driverclassname);
        return new DataSourceProxy(hikariDataSource);
    }
}