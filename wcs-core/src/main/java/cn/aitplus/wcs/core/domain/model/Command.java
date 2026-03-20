package cn.aitplus.wcs.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 命令实体类
 * 用于表示指令中的单个命令
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Command {
    /**
     * 命令字符串
     */
    private String command;
    
    /**
     * 写入值
     */
    private Object value;

    /**
     * 是否写入
     */
    private Boolean isWrite;
}
