package cn.aitplus.wcs.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "wcs.workflow")
public class WcsWorkflowProperties {

    private List<Long> allowedWarehouseIds = new ArrayList<>();

    public List<Long> getAllowedWarehouseIds() {
        return allowedWarehouseIds;
    }

    public void setAllowedWarehouseIds(List<Long> allowedWarehouseIds) {
        this.allowedWarehouseIds = allowedWarehouseIds == null ? new ArrayList<>() : allowedWarehouseIds;
    }
}
