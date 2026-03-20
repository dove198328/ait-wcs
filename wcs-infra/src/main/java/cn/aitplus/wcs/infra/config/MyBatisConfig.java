package cn.aitplus.wcs.infra.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis 基础配置，负责根据 JDBC 产品名称映射 databaseId，
 * 供 XML 中的 _databaseId 分支使用。
 */
@Configuration
public class MyBatisConfig {

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties p = new Properties();
        p.setProperty("PostgreSQL", "postgresql");
        p.setProperty("openGauss", "postgresql");
        p.setProperty("GaussDB", "postgresql");
        p.setProperty("SQL Server", "sqlserver");
        provider.setProperties(p);
        return provider;
    }
} 