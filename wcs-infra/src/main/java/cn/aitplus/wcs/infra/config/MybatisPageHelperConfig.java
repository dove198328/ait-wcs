package cn.aitplus.wcs.infra.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 注册 infra 层 MyBatis Mapper，让 XML mapper 能被 Spring 正常加载。
 */
@Configuration
@MapperScan("cn.aitplus.wcs.infra.persistence")
public class MybatisPageHelperConfig {
}
