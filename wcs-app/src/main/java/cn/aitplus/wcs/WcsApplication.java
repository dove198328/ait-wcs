package cn.aitplus.wcs;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCreateCacheAnnotation
@EnableMethodCache(basePackages = "cn.aitplus.wcs")
public class WcsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WcsApplication.class, args);
    }
}
