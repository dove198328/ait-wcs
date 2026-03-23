package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import cn.aitplus.wcs.workflow.service.WorkflowDefinitionCommandService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowDefinitionsController.class)
class WorkflowDefinitionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkflowDefinitionsService workflowDefinitionsService;

    @MockBean
    private WorkflowDefinitionCommandService workflowDefinitionCommandService;

    @Test
    @DisplayName("查询流程定义列表接口")
    void queryList() throws Exception {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(1L);

        when(workflowDefinitionsService.queryList(eq(1L), any(WorkflowDefinition.class))).thenReturn(List.of(definition));

        String body = "{}";
        MvcResult result = mockMvc.perform(post("/api/1/workflow-definitions/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.rows", hasSize(1)))
                .andExpect(jsonPath("$.data.rows[0].id").value(1))
                .andReturn();

        ArgumentCaptor<WorkflowDefinition> captor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(workflowDefinitionsService).queryList(eq(1L), captor.capture());
        assertEquals(1, captor.getValue().getWarehouseId());

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions queryList request body = " + body);
        System.out.println("workflowDefinitions queryList data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("新增流程定义接口")
    void create() throws Exception {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setBizType("INBOUND");

        when(workflowDefinitionCommandService.createAndDeploy(eq(1L), any(WorkflowDefinition.class))).thenReturn(definition);

        String body = "{\"bizType\":\"INBOUND\"}";
        MvcResult result = mockMvc.perform(post("/api/1/workflow-definitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bizType").value("INBOUND"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions create request body = " + body);
        System.out.println("workflowDefinitions create data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("更新流程定义接口")
    void update() throws Exception {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setBizType("OUTBOUND");

        when(workflowDefinitionCommandService.updateAndRedeploy(eq(1L), eq(2L), any(WorkflowDefinition.class))).thenReturn(definition);

        String body = "{\"bizType\":\"OUTBOUND\"}";
        MvcResult result = mockMvc.perform(put("/api/1/workflow-definitions/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bizType").value("OUTBOUND"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions update id = 2");
        System.out.println("workflowDefinitions update request body = " + body);
        System.out.println("workflowDefinitions update data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("删除流程定义接口")
    void deleteById() throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/1/workflow-definitions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        verify(workflowDefinitionCommandService).deletePhysically(1L, 2L);

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions delete id = 2");
        System.out.println("workflowDefinitions delete data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("根据业务类型查询流程定义接口")
    void findByBizType() throws Exception {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setBizType("INBOUND");

        when(workflowDefinitionsService.findByBizType(1L, "INBOUND")).thenReturn(definition);

        MvcResult result = mockMvc.perform(get("/api/1/workflow-definitions/biz-type/INBOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bizType").value("INBOUND"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions findByBizType bizType = INBOUND");
        System.out.println("workflowDefinitions findByBizType data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("根据流程ID查询流程定义接口")
    void queryByWorkflowId() throws Exception {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setWorkflowId("wf-1");

        when(workflowDefinitionsService.queryByWorkflowId(1L, "wf-1")).thenReturn(definition);

        MvcResult result = mockMvc.perform(get("/api/1/workflow-definitions/workflow/wf-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.workflowId").value("wf-1"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions queryByWorkflowId workflowId = wf-1");
        System.out.println("workflowDefinitions queryByWorkflowId data = " + responseJson.path("data").toPrettyString());
    }

    @Test
    @DisplayName("根据流程ID查询首个子流程定义接口")
    void getFirstSubDefString() throws Exception {
        when(workflowDefinitionsService.getFirstSubDefString(1L, "wf-1")).thenReturn("sub-def-1");

        MvcResult result = mockMvc.perform(get("/api/1/workflow-definitions/workflow/wf-1/first-sub-def"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("sub-def-1"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        System.out.println("workflowDefinitions getFirstSubDefString workflowId = wf-1");
        System.out.println("workflowDefinitions getFirstSubDefString data = " + responseJson.path("data").toPrettyString());
    }
}
