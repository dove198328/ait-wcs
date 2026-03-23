package cn.aitplus.wcs.workflow.support;

import cn.aitplus.wcs.workflow.config.WcsWorkflowProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WarehouseTenantSupport {

    private final WcsWorkflowProperties workflowProperties;

    public WarehouseTenantSupport(WcsWorkflowProperties workflowProperties) {
        this.workflowProperties = workflowProperties;
    }

    public String tenantIdOf(Long warehouseId) {
        assertAllowedWarehouse(warehouseId);
        return String.valueOf(warehouseId);
    }

    public Long warehouseIdOf(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId cannot be blank");
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("tenantId must be a numeric warehouseId: " + tenantId, ex);
        }
    }

    public boolean isAllowedWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            return false;
        }
        Set<Long> allowedWarehouseIds = getAllowedWarehouseIds();
        return allowedWarehouseIds.isEmpty() || allowedWarehouseIds.contains(warehouseId);
    }

    public boolean isAllowedTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        return isAllowedWarehouse(warehouseIdOf(tenantId));
    }

    public void assertAllowedWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("warehouseId cannot be null");
        }
        if (!isAllowedWarehouse(warehouseId)) {
            throw new IllegalArgumentException("warehouseId is not allowed for current instance: " + warehouseId);
        }
    }

    public void assertAllowedTenant(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalArgumentException("tenantId cannot be blank");
        }
        if (!isAllowedTenant(tenantId)) {
            throw new IllegalArgumentException("tenantId is not allowed for current instance: " + tenantId);
        }
    }

    public Set<Long> getAllowedWarehouseIds() {
        return new LinkedHashSet<>(workflowProperties.getAllowedWarehouseIds());
    }

    public Set<String> getAllowedTenantIds() {
        return getAllowedWarehouseIds().stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
