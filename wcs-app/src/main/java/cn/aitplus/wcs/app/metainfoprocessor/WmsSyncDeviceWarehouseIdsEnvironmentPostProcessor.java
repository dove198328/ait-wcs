package cn.aitplus.wcs.app.metainfoprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将 {@code wcs.workflow.allowedWarehouseIds} 拼成逗号串，供 WMS 等 URL 查询参数
 * {@code warehouseId=1,2,3} 使用；占位符属性名为 {@code wms.api.sync-device-warehouse-ids}。
 * <p>
 * 若已在配置中显式设置同名属性，则不再覆盖。
 */
public class WmsSyncDeviceWarehouseIdsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log =
        LoggerFactory.getLogger(WmsSyncDeviceWarehouseIdsEnvironmentPostProcessor.class);

    public static final String PROP_SYNC_WAREHOUSE_IDS = "wms.api.sync-device-warehouse-ids";
    private static final String PROP_ALLOWED = "wcs.workflow.allowed-warehouse-ids";
    private static final String SOURCE_NAME = "wmsSyncDeviceWarehouseIdsDerived";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.hasText(environment.getProperty(PROP_SYNC_WAREHOUSE_IDS))) {
            return;
        }
        try {
            Binder binder = Binder.get(environment);
            BindResult<List<Long>> bound = binder.bind(PROP_ALLOWED, Bindable.listOf(Long.class));
            if (!bound.isBound()) {
                addProperty(environment, "");
                return;
            }
            List<Long> ids = bound.get();
            if (isEmpty(ids)) {
                addProperty(environment, "");
                log.debug("wcs.workflow.allowedWarehouseIds 为空，{} 使用空字符串", PROP_SYNC_WAREHOUSE_IDS);
                return;
            }
            String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            addProperty(environment, joined);
        } catch (RuntimeException ex) {
            log.warn("从 {} 推导 {} 失败，将注入空字符串", PROP_ALLOWED, PROP_SYNC_WAREHOUSE_IDS, ex);
            addProperty(environment, "");
        }
    }

    private static boolean isEmpty(List<Long> ids) {
        return ids == null || ids.isEmpty();
    }

    private static void addProperty(ConfigurableEnvironment environment, String value) {
        MutablePropertySources sources = environment.getPropertySources();
        HashMap<String, Object> map = new HashMap<>(1);
        map.put(PROP_SYNC_WAREHOUSE_IDS, value);
        sources.addFirst(new MapPropertySource(SOURCE_NAME, map));
    }
}
