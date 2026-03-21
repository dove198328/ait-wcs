package cn.aitplus.wcs.core.spi.context;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class BasePluginContext {

    private String warehouseId;
    private String traceId;
    private String correlationId;
    private Map<String, Object> attributes = new HashMap<String, Object>();
}

