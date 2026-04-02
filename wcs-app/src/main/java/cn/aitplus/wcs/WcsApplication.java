package cn.aitplus.wcs;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "cn.aitplus.wcs")
@EnableCreateCacheAnnotation
@EnableMethodCache(basePackages = "cn.aitplus.wcs")
public class WcsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WcsApplication.class, args);
    }
}
