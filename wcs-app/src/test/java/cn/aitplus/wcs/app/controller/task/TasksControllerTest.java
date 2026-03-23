package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.Task;
import cn.aitplus.wcs.infra.persistence.task.TasksMapper;
import cn.hutool.json.JSONObject;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TasksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TasksMapper tasksMapper;

    @Test
    @Transactional
    @Rollback
    @DisplayName("查询任务列表接口")
    void queryList() throws Exception {
        String unique = "it-" + UUID.randomUUID();
        insertTasksAndReturnFirstId(unique);

        String queryBody = """
                {
                  "taskName": "%s-a"
                }
                """.formatted(unique);

        MvcResult queryResult = mockMvc.perform(post("/api/1/tasks/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows[0].taskName").value(unique + "-a"))
                .andExpect(jsonPath("$.data.rows[0].warehouseId").value(1))
                .andReturn();

        JsonNode queryJson = objectMapper.readTree(queryResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("queryList request body = " + queryBody);
        System.out.println("queryList data = " + queryJson.path("data").toPrettyString());
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("根据ID查询任务接口")
    void queryById() throws Exception {
        String unique = "it-" + UUID.randomUUID();
        long firstId = insertTasksAndReturnFirstId(unique);

        MvcResult queryByIdResult = mockMvc.perform(get("/api/1/tasks/" + firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(firstId))
                .andExpect(jsonPath("$.data.taskName").value(unique + "-a"))
                .andExpect(jsonPath("$.data.warehouseId").value(1))
                .andReturn();

        JsonNode queryByIdJson = objectMapper.readTree(queryByIdResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("queryById taskId = " + firstId);
        System.out.println("queryById data = " + queryByIdJson.path("data").toPrettyString());
    }

    @Test
    @Transactional
    @Rollback(false)
    @DisplayName("批量新增任务接口")
    void insertBatch() throws Exception {
        String unique = "it-" + UUID.randomUUID();
        String firstTaskName = unique + "-a";
        String secondTaskName = unique + "-b";
        String body = """
                [
                  {
                    "taskName": "%s",
                    "workflowDefId": "wf-a",
                    "status": "pending"
                  },
                  {
                    "taskName": "%s",
                    "workflowDefId": "wf-b",
                    "status": "pending"
                  }
                ]
                """.formatted(firstTaskName, secondTaskName);

        MvcResult result = mockMvc.perform(post("/api/1/tasks/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").isNumber())
                .andExpect(jsonPath("$.data[1]").isNumber())
                .andReturn();

        JsonNode insertJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long firstId = insertJson.path("data").get(0).asLong();
        long secondId = insertJson.path("data").get(1).asLong();

        Task firstTask = tasksMapper.queryById(1L, firstId);
        Task secondTask = tasksMapper.queryById(1L, secondId);

        assertNotNull(firstTask);
        assertNotNull(secondTask);
        assertEquals(firstTaskName, firstTask.getTaskName());
        assertEquals(secondTaskName, secondTask.getTaskName());
        assertEquals(1L, firstTask.getWarehouseId());
        assertEquals(1L, secondTask.getWarehouseId());
        System.out.println("insertBatch request body = " + body);
        System.out.println("insertBatch ids = " + insertJson.path("data").toPrettyString());
        System.out.println("insertBatch firstTask in db = " + objectMapper.writeValueAsString(firstTask));
        System.out.println("insertBatch secondTask in db = " + objectMapper.writeValueAsString(secondTask));
    }

    @Test
    @Transactional
    @Rollback
    @DisplayName("查询任务统计接口")
    void queryTaskStatistics() throws Exception {
        insertTasksAndReturnFirstId("it-" + UUID.randomUUID());

        JSONObject expected = tasksMapper.queryTaskStatistics(1L);
        JsonNode responseJson = queryTaskStatisticsResponse();
        JsonNode dataJson = responseJson.path("data");

        System.out.println("queryTaskStatistics expected = " + expected);
        System.out.println("queryTaskStatistics data = " + dataJson.toPrettyString());

        assertEquals(200, responseJson.path("code").asInt());
        assertTrue(dataJson.isObject());
        assertEquals(toInt(expected, "todayLoaded"), dataJson.path("todayLoaded").asInt());
        assertEquals(toInt(expected, "todayToLoad"), dataJson.path("todayToLoad").asInt());
        assertEquals(toInt(expected, "realTimeTasks"), readInt(dataJson, "realTimeTasks"));
        assertEquals(toInt(expected, "queuedTasks"), dataJson.path("queuedTasks").asInt());
        assertEquals(toInt(expected, "pendingTasks"), dataJson.path("pendingTasks").asInt());
        assertEquals(toInt(expected, "abnormalTasks"), dataJson.path("abnormalTasks").asInt());
    }

    private long insertTasksAndReturnFirstId(String unique) throws Exception {
        String body = """
                [
                  {
                    "taskName": "%s-a",
                    "workflowDefId": "wf-a",
                    "status": "pending"
                  },
                  {
                    "taskName": "%s-b",
                    "workflowDefId": "wf-b",
                    "status": "pending"
                  }
                ]
                """.formatted(unique, unique);

        MvcResult insertResult = mockMvc.perform(post("/api/1/tasks/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode insertJson = objectMapper.readTree(insertResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return insertJson.path("data").get(0).asLong();
    }

    private JsonNode queryTaskStatisticsResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/1/tasks/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private int toInt(JSONObject jsonObject, String key) {
        Object value = jsonObject == null ? null : jsonObject.get(key);
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }

    private int readInt(JsonNode jsonNode, String key) {
        JsonNode value = jsonNode.path(key);
        if (value.isMissingNode() || value.isNull()) {
            value = jsonNode.path(key.toLowerCase());
        }
        return value.isMissingNode() || value.isNull() ? 0 : value.asInt();
    }
}
