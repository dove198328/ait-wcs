package cn.aitplus.wcs.infra.service.task.impl;

import cn.aitplus.wcs.core.domain.model.WareHouseMode;
import cn.aitplus.wcs.infra.persistence.execution.CustomException;
import cn.aitplus.wcs.infra.persistence.task.WareHouseModeMapper;
import cn.aitplus.wcs.infra.service.task.WareHouseModeService;
import cn.aitplus.wcs.manager.Plc4xConnectionManager;
import cn.aitplus.wcs.plc4x.PlcDirectOperateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WareHouseModeServiceImpl implements WareHouseModeService {
    @Resource
    private WareHouseModeMapper wareHouseModeMapper;

    @Resource
    private Plc4xConnectionManager connectionManager;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String getCurrentMode(Integer warehouseId) {
        // 1. 获取仓库配置
        WareHouseMode wareHouseMode = getConfigByWarehouseId(warehouseId);
        if (wareHouseMode == null) {
            log.debug("未找到仓库{}的模式配置", warehouseId);
            return null;
        }
        String protocol = wareHouseMode.getProtocol();
        String connectionString = wareHouseMode.getAddress();
        String pointsJson = wareHouseMode.getCammonds(); // 保存了整段 pointsConfig JSON

        // 需要读取的点位
        List<String> pointIds = Arrays.asList("DQMS");

        Map<String, Object> values = PlcDirectOperateUtil.batchRead(protocol, connectionString, pointsJson, pointIds, connectionManager, objectMapper);

        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return values.toString();
        }
    }

    @Override
    public String changeMode(Integer warehouseId, int mode) {
        // 1. 获取仓库配置
        WareHouseMode wareHouseMode = getConfigByWarehouseId(warehouseId);
        if (wareHouseMode == null) {
            throw new CustomException("未找到WCS仓库的模式配置");
        }
        String protocol = wareHouseMode.getProtocol();
        String connectionString = wareHouseMode.getAddress();
        String pointsJson = wareHouseMode.getCammonds(); // 保存了整段 pointsConfig JSON

        // 需要读取的点位
        List<String> pointIds = Arrays.asList("DQMS", "SBZT");

        Map<String, Object> values = PlcDirectOperateUtil.batchRead(protocol,connectionString,pointsJson,pointIds,connectionManager,objectMapper);
        if (values != null){
            if((mode+"").equals(values.get("DQMS").toString())){
                return "已处在设置模式，无需切换";
            }
            if(values.get("SBZT").toString().equals("0")){
                return "当前线体非空闲，请稍后再试";
            }
            boolean b = PlcDirectOperateUtil.batchWrite(protocol, connectionString, pointsJson, Map.of("XFMS", mode), connectionManager, objectMapper);
            if (!b){
                return "模式切换失败";
            }
            return "模式切换成功";
        }
        return "模式切换失败";
    }

    private WareHouseMode getConfigByWarehouseId(Integer warehouseId) {
        return wareHouseModeMapper.selectOne(new QueryWrapper<WareHouseMode>().eq("warehouse_id", warehouseId));
    }
}
