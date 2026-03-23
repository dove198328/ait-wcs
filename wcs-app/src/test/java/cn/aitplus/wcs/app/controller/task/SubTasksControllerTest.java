package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.SubTask;
import cn.aitplus.wcs.infra.service.task.SubTasksService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubTasksController.class)
class SubTasksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubTasksService subTasksService;

    @Test
    @DisplayName("查询子任务列表接口")
    void queryList() throws Exception {
        SubTask subTask = new SubTask();
        subTask.setId(1L);

        when(subTasksService.queryList(eq(1L), any(SubTask.class))).thenReturn(List.of(subTask));

        String body = "{}";
        MvcResult result = mockMvc.perform(post("/api/1/subtasks/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows", hasSize(1)))
                .andExpect(jsonPath("$.data.rows[0].id").value(1))
                .andReturn();

        ArgumentCaptor<SubTask> captor = ArgumentCaptor.forClass(SubTask.class);
        verify(subTasksService).queryList(eq(1L), captor.capture());
        assertEquals(1L, captor.getValue().getWarehouseId());

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("subTasks queryList request body = " + body);
        System.out.println("subTasks queryList data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("根据ID查询子任务接口")
    void queryById() throws Exception {
        SubTask subTask = new SubTask();
        subTask.setId(8L);

        when(subTasksService.queryById(1L, 8L)).thenReturn(subTask);

        MvcResult result = mockMvc.perform(get("/api/1/subtasks/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(8))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("subTasks queryById id = 8");
        System.out.println("subTasks queryById data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("新增或更新子任务接口")
    void upsert() throws Exception {
        SubTask subTask = new SubTask();
        subTask.setId(3L);

        when(subTasksService.upsertSubtask(any(SubTask.class))).thenReturn(subTask);

        String body = "{}";
        MvcResult result = mockMvc.perform(post("/api/1/subtasks/upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(3))
                .andReturn();

        ArgumentCaptor<SubTask> captor = ArgumentCaptor.forClass(SubTask.class);
        verify(subTasksService).upsertSubtask(captor.capture());
        assertEquals(1L, captor.getValue().getWarehouseId());

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("subTasks upsert request body = " + body);
        System.out.println("subTasks upsert data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("查询TopN子任务接口")
    void findTopNSubtasks() throws Exception {
        SubTask subTask = new SubTask();
        subTask.setId(9L);
        when(subTasksService.findTopNSubtasks(1L, "READY", 3)).thenReturn(List.of(subTask));

        MvcResult result = mockMvc.perform(get("/api/1/subtasks/top")
                        .param("status", "READY")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(9))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("subTasks findTopNSubtasks status = READY, limit = 3");
        System.out.println("subTasks findTopNSubtasks data = " + responseJson.path("data").toPrettyString());
    }
}
