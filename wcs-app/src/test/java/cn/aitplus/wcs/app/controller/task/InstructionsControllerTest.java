package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.infra.service.task.InstructionsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InstructionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstructionsService instructionsService;

    // 测试查询指令列表接口
    @Test
    @Transactional
    @Rollback
    @DisplayName("查询指令列表接口")
    void queryList() throws Exception {
        // 创建一个 Instruction 示例
        Instruction instruction = new Instruction();
        instruction.setTaskId(1L);
        instruction.setSubtaskId(1L);
        instruction.setSequence(1);
        instruction.setProtocol("S7");
        instruction.setStatus("NEW");
        instruction.setDeviceId("D1");
        instruction.setParams("{\"pointId\":\"A1\"}");

        // 假设 instructionsService 有方法插入指令，这里模拟插入操作
        instructionsService.insertBatch(Arrays.asList(instruction));

        String body = "{ \"subtaskId\": 1 }";  // 请求体，查询条件，假设按 subtaskId 查询

        mockMvc.perform(post("/api/1/instructions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))  // 预期返回1条数据
                .andExpect(jsonPath("$.data.rows[0].taskId").value(1))  // 校验taskId
                .andExpect(jsonPath("$.data.rows[0].subtaskId").value(1))  // 校验subtaskId
                .andExpect(jsonPath("$.data.rows[0].sequence").value(1))  // 校验sequence
                .andExpect(jsonPath("$.data.rows[0].protocol").value("S7"))  // 校验protocol
                .andExpect(jsonPath("$.data.rows[0].status").value("NEW"))  // 校验status
                .andExpect(jsonPath("$.data.rows[0].deviceId").value("D1"));  // 校验deviceId
    }

    // 测试批量新增指令接口
    @Test
    @Transactional
    @Rollback(false)
    @DisplayName("批量新增指令接口")
    void insertBatch() throws Exception {
        // 创建两个指令对象
        Instruction instruction1 = new Instruction();
        instruction1.setTaskId(1L);
        instruction1.setSubtaskId(1L);
        instruction1.setSequence(1);
        instruction1.setProtocol("S7");
        instruction1.setStatus("NEW");
        instruction1.setDeviceId("D1");
        instruction1.setParams("{\"pointId\":\"A1\"}");
        instruction1.setCreatedAt(LocalDateTime.now());  // 设置默认的创建时间
        instruction1.setUpdatedAt(LocalDateTime.now());  // 设置默认的更新时间

        Instruction instruction2 = new Instruction();
        instruction2.setTaskId(1L);
        instruction2.setSubtaskId(1L);
        instruction2.setSequence(2);
        instruction2.setProtocol("S7");
        instruction2.setStatus("NEW");
        instruction2.setDeviceId("D2");
        instruction2.setParams("{\"pointId\":\"B1\"}");
        instruction2.setCreatedAt(LocalDateTime.now());  // 设置默认的创建时间
        instruction2.setUpdatedAt(LocalDateTime.now());  // 设置默认的更新时间

        List<Instruction> instructions = Arrays.asList(instruction1, instruction2);

        String body = objectMapper.writeValueAsString(instructions);  // 将指令列表转换为JSON字符串

        mockMvc.perform(post("/api/1/instructions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").isNumber())
                .andExpect(jsonPath("$.data[1]").isNumber());

        // 校验数据是否插入成功
        List<Instruction> insertedInstructions = instructionsService.queryList(1L, new Instruction());
        assertEquals(2, insertedInstructions.size());  // 检查插入的指令数量
        assertEquals("S7", insertedInstructions.get(0).getProtocol());  // 校验第一个指令的协议
        assertEquals("NEW", insertedInstructions.get(1).getStatus());  // 校验第二个指令的状态
    }
}