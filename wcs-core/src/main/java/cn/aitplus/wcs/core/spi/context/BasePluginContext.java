package cn.aitplus.wcs.core.spi.context;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class BasePluginContext {

    private String warehouseId;
    private String traceId;
    private String correlationId;
    private Map<String, Object> attributes = new HashMap<String, Object>();
}

