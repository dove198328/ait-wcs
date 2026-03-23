package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.core.domain.model.SubTask;
import cn.aitplus.wcs.core.domain.model.Task;
import cn.aitplus.wcs.infra.persistence.task.InstructionsMapper;
import cn.aitplus.wcs.infra.persistence.task.SubTasksMapper;
import cn.aitplus.wcs.infra.persistence.task.TasksMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private TasksMapper tasksMapper;

    @Autowired
    private SubTasksMapper subTasksMapper;

    @Autowired
    private InstructionsMapper instructionsMapper;

    @Test
    @Transactional
    @Rollback
    @DisplayName("查询指令列表接口")
    void queryList() throws Exception {
        long subtaskId = prepareInstructionData("it-" + UUID.randomUUID(), 1);

        String body = """
                {
                  "subtaskId": %d
                }
                """.formatted(subtaskId);

        MvcResult result = mockMvc.perform(post("/api/1/instructions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].subtaskId").value(subtaskId))
                .andExpect(jsonPath("$.data.rows[0].sequence").value(1))
                .andExpect(jsonPath("$.data.rows[0].protocol").value("S7"))
                .andExpect(jsonPath("$.data.rows[0].status").value("NEW"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("instructions queryList request body = " + body);
        System.out.println("instructions queryList data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @Transactional
    @Rollback(false)
    @DisplayName("批量新增指令接口")
    void insertBatch() throws Exception {
        String unique = "it-" + UUID.randomUUID();
        Task task = insertTask(unique);
        SubTask subTask = insertSubTask(task, unique);

        String body = """
                [
                  {
                    "subtaskId": %d,
                    "sequence": 1,
                    "protocol": "S7",
                    "status": "NEW",
                    "taskId": %d,
                    "deviceId": "D1",
                    "params": "{\\"pointId\\":\\"A1\\"}"
                  }
                ]
                """.formatted(subTask.getId(), task.getId());

        MvcResult result = mockMvc.perform(post("/api/1/instructions/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        Instruction query = new Instruction();
        query.setSubtaskId(subTask.getId());
        List<Instruction> instructions = instructionsMapper.queryList(1L, query);

        assertFalse(instructions.isEmpty());
        Instruction instruction = instructions.get(0);
        assertNotNull(instruction.getId());
        assertEquals(task.getId(), instruction.getTaskId());
        assertEquals(subTask.getId(), instruction.getSubtaskId());
        assertEquals(1, instruction.getSequence());
        assertEquals("S7", instruction.getProtocol());
        assertEquals("NEW", instruction.getStatus());
        assertEquals("D1", instruction.getDeviceId());

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("instructions insertBatch request body = " + body);
        System.out.println("instructions insertBatch data = " + responseJson.path("data").toPrettyString());
        System.out.println("instructions insertBatch firstInstruction in db = " + objectMapper.writeValueAsString(instruction));
    }

    private long prepareInstructionData(String unique, int sequence) {
        Task task = insertTask(unique);
        SubTask subTask = insertSubTask(task, unique);
        insertInstruction(task, subTask, sequence);
        return subTask.getId();
    }

    private Task insertTask(String unique) {
        Task task = new Task();
        task.setWorkflowDefId("wf-" + unique);
        task.setTaskName("task-" + unique);
        task.setStatus("pending");
        task.setWarehouseId(1L);
        tasksMapper.insert(task);
        return task;
    }

    private SubTask insertSubTask(Task task, String unique) {
        SubTask subTask = new SubTask();
        subTask.setTaskId(task.getId());
        subTask.setSubtaskDefId("subdef-" + unique);
        subTask.setName("subtask-" + unique);
        subTask.setPriority(1);
        subTask.setStatus("pending");
        subTask.setWarehouseId(1L);
        subTask.setWorkflowDefId(task.getWorkflowDefId());
        subTasksMapper.insert(subTask);
        return subTask;
    }

    private void insertInstruction(Task task, SubTask subTask, int sequence) {
        Instruction instruction = new Instruction();
        instruction.setTaskId(task.getId());
        instruction.setSubtaskId(subTask.getId());
        instruction.setSequence(sequence);
        instruction.setProtocol("S7");
        instruction.setStatus("NEW");
        instruction.setDeviceId("D1");
        instruction.setParams("{\"pointId\":\"A1\"}");
        instructionsMapper.insert(instruction);
    }
}
