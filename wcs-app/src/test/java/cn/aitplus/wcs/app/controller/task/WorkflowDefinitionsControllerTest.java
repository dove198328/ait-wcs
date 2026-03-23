package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import cn.aitplus.wcs.workflow.service.WorkflowDefinitionCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkflowDefinitionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private WorkflowDefinitionsService workflowDefinitionsService;

    @Mock
    private WorkflowDefinitionCommandService workflowDefinitionCommandService;

    @InjectMocks
    private WorkflowDefinitionsController workflowDefinitionsController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    /**
     * 测试查询流程定义列表接口 (POST /api/{wareHouseId}/workflow-definitions/search)
     */
    @Test
    @DisplayName("测试查询流程定义列表接口")
    void testQueryList() throws Exception {
        Long wareHouseId = 1L;
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setWarehouseId(wareHouseId.intValue());

        when(workflowDefinitionsService.queryByPage(eq(wareHouseId), any(), eq(workflowDefinition)))
                .thenReturn(null);

        String responseContent = mockMvc.perform(post("/api/{wareHouseId}/workflow-definitions/search", wareHouseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workflowDefinition)))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Query List Response: " + responseContent);
    }

    /**
     * 测试新增流程定义并部署到工作流接口 (POST /api/{wareHouseId}/workflow-definitions)
     */
    @Test
    @Transactional
    @Rollback(value = false)
    @DisplayName("测试新增流程定义并部署到工作流接口")
    void testCreate() throws Exception {
        Long wareHouseId = 1L;
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setWarehouseId(wareHouseId.intValue());
        workflowDefinition.setBizType("RK2");
        workflowDefinition.setWorkflowId("RK2");
        workflowDefinition.setConfig("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" id=\"Definitions_1763640437532\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"X6 to BPMN Converter\" exporterVersion=\"1.0\" camunda:diagramRelationId=\"Diagram_1763640437532\">\n" +
                "  <bpmn:error id=\"MyBpmnError\" name=\"子任务失败异常\" errorCode=\"SUBTASK_FAILED\" />\n" +
                "  <bpmn:process id=\"Process_1763640437532\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"startEvent-f9f2c6fa-0e5b-4038-b46a-8939026f1476\" name=\"开始\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-6e318e76-43ad-4178-aee4-68295ca81fcb\" name=\"1001监听请求\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-9afb31d1-a9bc-49c3-b911-6f09a9be006a\" name=\"1001监听请求\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_gtjib97\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-dddd3c0a-c90e-4bd2-aacd-be68d0fb5365\" name=\"1001下发退回\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:endEvent id=\"endEvent-ce496d40-8b80-4cb0-b075-ff616e956f74\" name=\"结束\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:executionListener class=\"cn.aitplus.wcs.listener.ProcessEndListener\" event=\"end\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:endEvent>\n" +
                "    <bpmn:exclusiveGateway id=\"exclusiveGateway-ExclusiveGateway-5ba11db1-6eea-4bd4-8429-967820f1f1ae\" name=\"判断货物宽度\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-962b7602-59cb-40c8-b2aa-4568659b603f\" name=\"1001下发\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-15f09883-89f5-45c5-a454-76fa3361f8c3\" name=\"监听巷道口信号/堆垛机\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-2fb0c785-772a-4426-b1c2-0df0e849fb42\" name=\"监听巷道口信号\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_fs9junf\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-a16965ae-65f3-4c0f-b204-34da26100bc6\" name=\"堆垛机下发\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-4d469b95-e5a3-4d30-be30-6315e9b29f82\" name=\"监听堆垛机完成\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-6bb73c06-2d6d-4083-85c2-cbb1de8f7830\" name=\"监听堆垛机完成\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_smp6k8q\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-0060a3f7-c67f-40e8-8f35-df29bd0bf06b\" name=\"流程结束通知\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-e9789efb-b382-4628-92c6-89df357e1a62\" name=\"1010监听请求\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-fad5337e-01b0-4a86-a925-1717a70f4e85\" name=\"1010监听请求\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_f4bj0e6\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:exclusiveGateway id=\"exclusiveGateway-ExclusiveGateway-0c38f4a0-fd74-46b0-8b15-883dd8c979b1\" name=\"判断货物宽度\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-c177d4db-6f0f-4d99-80a2-b0c0d93da311\" name=\"1010下发退回\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-f57a95ff-8165-40ea-adf4-085f5f482721\" name=\"监听1号堆垛机\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:exclusiveGateway id=\"exclusiveGateway-ExclusiveGateway-06fe1d15-8cac-463c-bdb1-5261fbf01ded\" name=\"网关\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-ef565a8d-91a2-43ff-ad76-88f96fc5c592\" name=\"1010下发\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:endEvent id=\"endEvent-f8b197bf-afd8-4454-8737-579f3131c7f4\" name=\"结束\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:executionListener class=\"cn.aitplus.wcs.listener.ProcessEndListener\" event=\"end\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:endEvent>\n" +
                "    <bpmn:endEvent id=\"endEvent-c5c3dc35-f3f9-433f-ac36-086cc404b5c0\" name=\"结束\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:executionListener class=\"cn.aitplus.wcs.listener.ProcessEndListener\" event=\"end\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:endEvent>\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-e018c59b-ceba-4821-97ff-d929651fae3d\" name=\"监听1号堆垛机\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_8gnmnac\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-abf72dcd-0723-4ee8-b862-6858bf6b79e4\" name=\"1号堆垛机下发\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-6574e020-fbad-4027-8cd2-bdf9ac9958a1\" name=\"监听1号堆垛机完成\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-6d62cd93-72ca-43a4-aa0c-812ae5ea2d33\" name=\"监听1号堆垛机完成\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_ntz5izv\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-2edcaeba-c111-418d-afcb-29c36ea9460a\" name=\"流程结束通知\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:endEvent id=\"endEvent-7206bd23-cab8-4895-9a30-1c0bb06d7bf4\" name=\"结束\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:executionListener class=\"cn.aitplus.wcs.listener.ProcessEndListener\" event=\"end\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:endEvent>\n" +
                "    <bpmn:exclusiveGateway id=\"exclusiveGateway-ExclusiveGateway-d8f96d5c-e410-4a84-998b-c41cdc33b82b\" name=\"判断流向\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-fce7170b-7d3e-47ff-a86c-ecff28d7cebb\" name=\"监听1巷道堆垛机\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-da00243d-3d85-4416-ae53-c0316b6fc3bf\" name=\"监听1巷道信号\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_ynsotfp\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-e2cdf56c-a5a9-4ad6-bdd4-60641322f649\" name=\"1号堆垛机下发\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-c3ae26fe-ab2a-4432-9c4c-02742fb9f2a8\" name=\"监听1号堆垛机完成\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-c65c1539-d28d-4d0a-aede-251553ad7e39\" name=\"监听1号堆垛机完成\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_9vn3vdj\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-64df1928-4d13-4ad0-9361-6ffe94b87b0f\" name=\"监听2巷道堆垛机\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-43a87738-370d-4528-9e76-77cc0968da14\" name=\"监听2巷道堆垛机\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_v93rhe8\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-41737f78-3587-4405-96d1-cc9cbb525438\" name=\"2号堆垛机下发\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-5a42df5a-531b-4e9d-8748-7943aba65101\" name=\"监听2号堆垛机完成\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-9c49bb62-82fb-490d-bf30-d19963dbd8b7\" name=\"监听2号堆垛机完成\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_nrksuzy\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-cc9e15a5-a2d9-4087-b474-a4e4a5c375f7\" name=\"流程结束通知\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:endEvent id=\"endEvent-5b35d890-2a9c-4c04-bda1-3c6ed54cde82\" name=\"结束\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <camunda:executionListener class=\"cn.aitplus.wcs.listener.ProcessEndListener\" event=\"end\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:endEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-2cd26aa2-a2ee-413e-af02-31a721664b5a\" name=\"任务开始通知\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-c871ddc2-a099-4f32-8134-05027e218ded\" name=\"任务开始通知\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-aafd35b7-c3ba-4c79-9e98-75954bc26ebe\" name=\"1001入库拍照\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-32a1a3ed-e447-4c32-a5d7-2a343d5f6a56\" name=\"1010入库拍照\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-77cb4373-e84d-448f-b2eb-20f37aba701f\" name=\"判断货位宽度\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-69311060-bb86-4c8e-bcfd-eeb31f8b8dd1\" name=\"判断货位宽度\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-2286251d-38ee-499a-9b19-4d9083e455c8\" name=\"启动\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:serviceTask id=\"serviceTask-6d62f328-ed7a-4aea-8c71-90ce543218d3\" name=\"监听入口巷道\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-1f95516b-5b5b-4251-bd82-5669f97a1693\" name=\"监听巷道入库口\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_qv8p78i\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-c70332e6-f162-4f97-98cf-dde2136d0f8e\" name=\"监听1巷道口输送机\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-01a9700a-8096-465e-9818-3ab853991403\" name=\"监听1巷道口输送机\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_8idthe9\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-2e79854f-0104-4730-a64c-6d41fd041d5a\" name=\"监听1巷道输送机信号\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-1bcf040c-c8b1-472d-9d86-445427ad1595\" name=\"监听1巷道输送机信号\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_dknto1y\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:serviceTask id=\"serviceTask-44d9c837-28e6-4fe2-8959-4265b325d0a9\" name=\"监听2巷道输送机\" camunda:type=\"external\" camunda:topic=\"executeSubtask\" />\n" +
                "    <bpmn:intermediateCatchEvent id=\"Event-ece747e1-9247-47f6-98ef-e04f761058b6\" name=\"监听2巷道输送机\" camunda:asyncBefore=\"true\">\n" +
                "      <bpmn:messageEventDefinition messageRef=\"Message_ze6bmo4\" />\n" +
                "    </bpmn:intermediateCatchEvent>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-341e874f-affa-4fdf-a6e5-3c6cd18ae9b2\" name=\"\" sourceRef=\"Event-6d62cd93-72ca-43a4-aa0c-812ae5ea2d33\" targetRef=\"serviceTask-44d9c837-28e6-4fe2-8959-4265b325d0a9\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-8129375d-8c67-4d85-aefb-d7b9f53997c5\" name=\"\" sourceRef=\"serviceTask-44d9c837-28e6-4fe2-8959-4265b325d0a9\" targetRef=\"Event-ece747e1-9247-47f6-98ef-e04f761058b6\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-51453cef-ed8b-4f64-8b59-bb89c9b52024\" name=\"\" sourceRef=\"Event-ece747e1-9247-47f6-98ef-e04f761058b6\" targetRef=\"serviceTask-64df1928-4d13-4ad0-9361-6ffe94b87b0f\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-c9a4916d-4e62-4ffe-803c-fa933f64da57\" name=\"\" sourceRef=\"serviceTask-962b7602-59cb-40c8-b2aa-4568659b603f\" targetRef=\"serviceTask-6d62f328-ed7a-4aea-8c71-90ce543218d3\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-18461c7e-225e-4185-b0bd-c60a74ce6056\" name=\"\" sourceRef=\"serviceTask-6d62f328-ed7a-4aea-8c71-90ce543218d3\" targetRef=\"Event-1f95516b-5b5b-4251-bd82-5669f97a1693\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-161fc5fb-969d-486a-b739-1a8bca3dab64\" name=\"\" sourceRef=\"Event-1f95516b-5b5b-4251-bd82-5669f97a1693\" targetRef=\"serviceTask-15f09883-89f5-45c5-a454-76fa3361f8c3\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-e8a5af69-ec4a-4a5f-b2d4-0f1a5e1ad43c\" name=\"\" sourceRef=\"Event-da00243d-3d85-4416-ae53-c0316b6fc3bf\" targetRef=\"serviceTask-e2cdf56c-a5a9-4ad6-bdd4-60641322f649\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-92289535-d7f7-46c1-b176-aeb18a7a3d53\" name=\"\" sourceRef=\"serviceTask-c70332e6-f162-4f97-98cf-dde2136d0f8e\" targetRef=\"Event-01a9700a-8096-465e-9818-3ab853991403\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-56a23dce-e3b2-4258-bd09-80586f146019\" name=\"\" sourceRef=\"Event-01a9700a-8096-465e-9818-3ab853991403\" targetRef=\"serviceTask-fce7170b-7d3e-47ff-a86c-ecff28d7cebb\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-07945d47-09d8-4825-8c8d-ff712cea3a9d\" name=\"${taskDirection == 1}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-d8f96d5c-e410-4a84-998b-c41cdc33b82b\" targetRef=\"serviceTask-c70332e6-f162-4f97-98cf-dde2136d0f8e\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${taskDirection == 1}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-15061aa4-b741-4c06-bdb1-35f1cd341599\" name=\"\" sourceRef=\"serviceTask-f57a95ff-8165-40ea-adf4-085f5f482721\" targetRef=\"Event-e018c59b-ceba-4821-97ff-d929651fae3d\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-028a3aa9-480a-4fa8-86ca-73183276c55c\" name=\"${taskDirection == 2}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-d8f96d5c-e410-4a84-998b-c41cdc33b82b\" targetRef=\"serviceTask-2e79854f-0104-4730-a64c-6d41fd041d5a\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${taskDirection == 2}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-e2ce2ada-f258-457f-8cbd-77c8dc12730f\" name=\"\" sourceRef=\"serviceTask-2e79854f-0104-4730-a64c-6d41fd041d5a\" targetRef=\"Event-1bcf040c-c8b1-472d-9d86-445427ad1595\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-36154ba2-5531-4de3-bac5-9642246c530f\" name=\"\" sourceRef=\"Event-1bcf040c-c8b1-472d-9d86-445427ad1595\" targetRef=\"serviceTask-f57a95ff-8165-40ea-adf4-085f5f482721\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-e8ca9d8a-74dc-4bf4-9a8e-f95075574443\" name=\"\" sourceRef=\"startEvent-f9f2c6fa-0e5b-4038-b46a-8939026f1476\" targetRef=\"serviceTask-2286251d-38ee-499a-9b19-4d9083e455c8\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-ec62b810-2680-41a8-aa39-416149954302\" name=\"\" sourceRef=\"serviceTask-2286251d-38ee-499a-9b19-4d9083e455c8\" targetRef=\"exclusiveGateway-ExclusiveGateway-06fe1d15-8cac-463c-bdb1-5261fbf01ded\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-40287693-d65f-48c7-b3fa-b9f29eda41eb\" name=\"\" sourceRef=\"Event-9afb31d1-a9bc-49c3-b911-6f09a9be006a\" targetRef=\"serviceTask-77cb4373-e84d-448f-b2eb-20f37aba701f\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-1b08432d-15c2-47d1-a5e9-b446cef5db6b\" name=\"\" sourceRef=\"serviceTask-77cb4373-e84d-448f-b2eb-20f37aba701f\" targetRef=\"exclusiveGateway-ExclusiveGateway-5ba11db1-6eea-4bd4-8429-967820f1f1ae\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-72e7ff98-4fe1-4ecd-96ab-b69fd7d17370\" name=\"\" sourceRef=\"Event-fad5337e-01b0-4a86-a925-1717a70f4e85\" targetRef=\"serviceTask-69311060-bb86-4c8e-bcfd-eeb31f8b8dd1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-c0e8fe91-77bc-47f1-bfcb-254cb2e6d487\" name=\"\" sourceRef=\"serviceTask-69311060-bb86-4c8e-bcfd-eeb31f8b8dd1\" targetRef=\"exclusiveGateway-ExclusiveGateway-0c38f4a0-fd74-46b0-8b15-883dd8c979b1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-e6ce235a-5df3-412b-af5f-44569ea80e67\" name=\"\" sourceRef=\"serviceTask-2cd26aa2-a2ee-413e-af02-31a721664b5a\" targetRef=\"serviceTask-aafd35b7-c3ba-4c79-9e98-75954bc26ebe\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-a9e76a1c-4535-451e-8153-76f23709cad0\" name=\"\" sourceRef=\"serviceTask-aafd35b7-c3ba-4c79-9e98-75954bc26ebe\" targetRef=\"serviceTask-962b7602-59cb-40c8-b2aa-4568659b603f\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-52023362-1715-4412-bdc2-2f827bb6c48b\" name=\"\" sourceRef=\"serviceTask-c871ddc2-a099-4f32-8134-05027e218ded\" targetRef=\"serviceTask-32a1a3ed-e447-4c32-a5d7-2a343d5f6a56\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-d5de1aa4-e70e-4f37-8bfd-2a51fb07a29e\" name=\"\" sourceRef=\"serviceTask-32a1a3ed-e447-4c32-a5d7-2a343d5f6a56\" targetRef=\"serviceTask-ef565a8d-91a2-43ff-ad76-88f96fc5c592\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-a7b42068-c021-4e6c-98ff-cecd317b1191\" name=\"${taskDistribution != 2}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-5ba11db1-6eea-4bd4-8429-967820f1f1ae\" targetRef=\"serviceTask-2cd26aa2-a2ee-413e-af02-31a721664b5a\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${taskDistribution != 2}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-0b7b0790-df79-47eb-b77c-a7a348ec7ba1\" name=\"${taskDistribution != 2}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-0c38f4a0-fd74-46b0-8b15-883dd8c979b1\" targetRef=\"serviceTask-c871ddc2-a099-4f32-8134-05027e218ded\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${taskDistribution != 2}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-550600a4-43ad-48b5-a8a5-436a6aec6cef\" name=\"\" sourceRef=\"serviceTask-64df1928-4d13-4ad0-9361-6ffe94b87b0f\" targetRef=\"Event-43a87738-370d-4528-9e76-77cc0968da14\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-86509e68-eb19-4a06-884d-c52265ebf754\" name=\"\" sourceRef=\"Event-43a87738-370d-4528-9e76-77cc0968da14\" targetRef=\"serviceTask-41737f78-3587-4405-96d1-cc9cbb525438\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-5a4477f1-23cd-4f93-a946-951ff5426449\" name=\"\" sourceRef=\"serviceTask-41737f78-3587-4405-96d1-cc9cbb525438\" targetRef=\"serviceTask-5a42df5a-531b-4e9d-8748-7943aba65101\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-b4547df7-8df2-4542-94d8-9eaaec849364\" name=\"\" sourceRef=\"serviceTask-5a42df5a-531b-4e9d-8748-7943aba65101\" targetRef=\"Event-9c49bb62-82fb-490d-bf30-d19963dbd8b7\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-449b2511-45c1-4383-831b-b92f29d118be\" name=\"\" sourceRef=\"Event-9c49bb62-82fb-490d-bf30-d19963dbd8b7\" targetRef=\"serviceTask-cc9e15a5-a2d9-4087-b474-a4e4a5c375f7\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-77928878-66c0-446d-ba76-09e4bc85a9ba\" name=\"\" sourceRef=\"serviceTask-cc9e15a5-a2d9-4087-b474-a4e4a5c375f7\" targetRef=\"endEvent-5b35d890-2a9c-4c04-bda1-3c6ed54cde82\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-27a76ff0-3e8f-4b28-ac27-2b4a20a1ed9e\" name=\"\" sourceRef=\"serviceTask-e2cdf56c-a5a9-4ad6-bdd4-60641322f649\" targetRef=\"serviceTask-c3ae26fe-ab2a-4432-9c4c-02742fb9f2a8\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-2bd8d76a-74f8-401e-b300-6e0cb536c4b4\" name=\"\" sourceRef=\"serviceTask-c3ae26fe-ab2a-4432-9c4c-02742fb9f2a8\" targetRef=\"Event-c65c1539-d28d-4d0a-aede-251553ad7e39\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-6d23d15c-1964-4d34-9903-3948491dd979\" name=\"\" sourceRef=\"Event-c65c1539-d28d-4d0a-aede-251553ad7e39\" targetRef=\"serviceTask-2edcaeba-c111-418d-afcb-29c36ea9460a\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-2824e340-edb6-456c-8fd6-dcc92030cdf1\" name=\"\" sourceRef=\"serviceTask-ef565a8d-91a2-43ff-ad76-88f96fc5c592\" targetRef=\"exclusiveGateway-ExclusiveGateway-d8f96d5c-e410-4a84-998b-c41cdc33b82b\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-55550eb8-9d06-4af4-9284-34b7e05ed789\" name=\"\" sourceRef=\"serviceTask-fce7170b-7d3e-47ff-a86c-ecff28d7cebb\" targetRef=\"Event-da00243d-3d85-4416-ae53-c0316b6fc3bf\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-51c7c2f7-cb57-4266-b8f0-e701701eb3de\" name=\"\" sourceRef=\"Event-e018c59b-ceba-4821-97ff-d929651fae3d\" targetRef=\"serviceTask-abf72dcd-0723-4ee8-b862-6858bf6b79e4\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-944fb86b-f3eb-44c3-8fdb-4c876ff16bed\" name=\"\" sourceRef=\"serviceTask-abf72dcd-0723-4ee8-b862-6858bf6b79e4\" targetRef=\"serviceTask-6574e020-fbad-4027-8cd2-bdf9ac9958a1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-bb0d89ba-81df-4d70-bd8b-c9ef80590e5e\" name=\"\" sourceRef=\"serviceTask-6574e020-fbad-4027-8cd2-bdf9ac9958a1\" targetRef=\"Event-6d62cd93-72ca-43a4-aa0c-812ae5ea2d33\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-dafebdb6-16bd-49fe-971e-88a774c0fad6\" name=\"\" sourceRef=\"serviceTask-2edcaeba-c111-418d-afcb-29c36ea9460a\" targetRef=\"endEvent-7206bd23-cab8-4895-9a30-1c0bb06d7bf4\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-687013c1-b7c9-4b9b-98b5-64485c5e2689\" name=\"${aliveRkk == 3}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-06fe1d15-8cac-463c-bdb1-5261fbf01ded\" targetRef=\"serviceTask-6e318e76-43ad-4178-aee4-68295ca81fcb\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${aliveRkk == 3}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-0ae0fb08-32b0-442e-ad4e-3977c21d3678\" name=\"${aliveRkk == 4}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-06fe1d15-8cac-463c-bdb1-5261fbf01ded\" targetRef=\"serviceTask-e9789efb-b382-4628-92c6-89df357e1a62\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${aliveRkk == 4}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-5ba2444f-938f-4119-b0b8-48d022d358e9\" name=\"\" sourceRef=\"serviceTask-dddd3c0a-c90e-4bd2-aacd-be68d0fb5365\" targetRef=\"endEvent-f8b197bf-afd8-4454-8737-579f3131c7f4\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-3c172a87-c277-4fd6-a95f-8ed128e5c0a8\" name=\"\" sourceRef=\"serviceTask-c177d4db-6f0f-4d99-80a2-b0c0d93da311\" targetRef=\"endEvent-c5c3dc35-f3f9-433f-ac36-086cc404b5c0\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-880eba0e-4d74-4fb4-8024-49b3abce25ec\" name=\"\" sourceRef=\"serviceTask-e9789efb-b382-4628-92c6-89df357e1a62\" targetRef=\"Event-fad5337e-01b0-4a86-a925-1717a70f4e85\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-ccfa102a-31f3-4348-8c3f-7a9e69bdb3c5\" name=\"${taskDistribution == 2}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-0c38f4a0-fd74-46b0-8b15-883dd8c979b1\" targetRef=\"serviceTask-c177d4db-6f0f-4d99-80a2-b0c0d93da311\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${taskDistribution == 2}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-dd419f4b-ea9f-44f6-ac82-b62d4ecec488\" name=\"\" sourceRef=\"Event-2fb0c785-772a-4426-b1c2-0df0e849fb42\" targetRef=\"serviceTask-a16965ae-65f3-4c0f-b204-34da26100bc6\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-6aa2ccce-6901-478b-800b-13aa0be1f7b8\" name=\"\" sourceRef=\"serviceTask-6e318e76-43ad-4178-aee4-68295ca81fcb\" targetRef=\"Event-9afb31d1-a9bc-49c3-b911-6f09a9be006a\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-26acb5ee-3868-4e24-a877-fbc5e9a1f613\" name=\"${taskDistribution == 2}\" sourceRef=\"exclusiveGateway-ExclusiveGateway-5ba11db1-6eea-4bd4-8429-967820f1f1ae\" targetRef=\"serviceTask-dddd3c0a-c90e-4bd2-aacd-be68d0fb5365\">\n" +
                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${taskDistribution == 2}</bpmn:conditionExpression>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow-b3be735c-45a2-44fe-be85-9ec2232f6ba9\" name=\"\" sourceRef=\"serviceTask-15f09883-89f5-45c5-a454-76fa3361f8c3\" targetRef=\"Event-2fb0c785-772a-4426-b1c2-0df0e849fb42\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-08824249-49b8-4dbd-b1cf-17fef15e1c5c\" name=\"\" sourceRef=\"serviceTask-a16965ae-65f3-4c0f-b204-34da26100bc6\" targetRef=\"serviceTask-4d469b95-e5a3-4d30-be30-6315e9b29f82\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-945150f9-273c-4d37-9b30-d8139ffdf043\" name=\"\" sourceRef=\"serviceTask-4d469b95-e5a3-4d30-be30-6315e9b29f82\" targetRef=\"Event-6bb73c06-2d6d-4083-85c2-cbb1de8f7830\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-7bd69ef6-a055-415c-9c6a-9c7649bd3bbf\" name=\"\" sourceRef=\"Event-6bb73c06-2d6d-4083-85c2-cbb1de8f7830\" targetRef=\"serviceTask-0060a3f7-c67f-40e8-8f35-df29bd0bf06b\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow-3a8820ae-ec9c-4b82-a27b-12a922ccb325\" name=\"\" sourceRef=\"serviceTask-0060a3f7-c67f-40e8-8f35-df29bd0bf06b\" targetRef=\"endEvent-ce496d40-8b80-4cb0-b075-ff616e956f74\" />\n" +
                "    <bpmn:subProcess id=\"ErrorCatcher\" triggeredByEvent=\"true\">\n" +
                "      <bpmn:startEvent id=\"errorStart\" name=\"错误捕获\">\n" +
                "        <bpmn:errorEventDefinition errorRef=\"MyBpmnError\" />\n" +
                "      </bpmn:startEvent>\n" +
                "      <bpmn:serviceTask id=\"errorHandler\" name=\"统一错误处理\" camunda:asyncBefore=\"true\" camunda:exclusive=\"false\" camunda:class=\"cn.aitplus.wcs.handler.ErrorHandler\" />\n" +
                "      <bpmn:endEvent id=\"errorEnd\" />\n" +
                "      <bpmn:sequenceFlow id=\"flow_errorStart_to_handler\" sourceRef=\"errorStart\" targetRef=\"errorHandler\" />\n" +
                "      <bpmn:sequenceFlow id=\"flow_handler_to_end\" sourceRef=\"errorHandler\" targetRef=\"errorEnd\" />\n" +
                "    </bpmn:subProcess>\n" +
                "  </bpmn:process>\n" +
                "  <bpmn:message id=\"Message_gtjib97\" name=\"SSJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_fs9junf\" name=\"DEVICE_VALUE_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_smp6k8q\" name=\"DDJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_f4bj0e6\" name=\"SSJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_8gnmnac\" name=\"DEVICE_VALUE_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_ntz5izv\" name=\"DDJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_ynsotfp\" name=\"DEVICE_VALUE_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_9vn3vdj\" name=\"DDJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_v93rhe8\" name=\"DEVICE_VALUE_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_nrksuzy\" name=\"DDJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_qv8p78i\" name=\"SSJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_8idthe9\" name=\"SSJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_dknto1y\" name=\"SSJ_EVENT\" />\n" +
                "  <bpmn:message id=\"Message_ze6bmo4\" name=\"SSJ_EVENT\" />\n" +
                "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\" name=\"流程图\">\n" +
                "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_1763640437532\">\n" +
                "      <bpmndi:BPMNShape id=\"Shape_startEvent-f9f2c6fa-0e5b-4038-b46a-8939026f1476\" bpmnElement=\"startEvent-f9f2c6fa-0e5b-4038-b46a-8939026f1476\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"-453.5\" y=\"352.5\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-6e318e76-43ad-4178-aee4-68295ca81fcb\" bpmnElement=\"serviceTask-6e318e76-43ad-4178-aee4-68295ca81fcb\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"219\" y=\"190\" width=\"100\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-9afb31d1-a9bc-49c3-b911-6f09a9be006a\" bpmnElement=\"Event-9afb31d1-a9bc-49c3-b911-6f09a9be006a\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"461\" y=\"187.5\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-dddd3c0a-c90e-4bd2-aacd-be68d0fb5365\" bpmnElement=\"serviceTask-dddd3c0a-c90e-4bd2-aacd-be68d0fb5365\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"818\" y=\"20\" width=\"130\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_endEvent-ce496d40-8b80-4cb0-b075-ff616e956f74\" bpmnElement=\"endEvent-ce496d40-8b80-4cb0-b075-ff616e956f74\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2740\" y=\"230\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_exclusiveGateway-ExclusiveGateway-5ba11db1-6eea-4bd4-8429-967820f1f1ae\" bpmnElement=\"exclusiveGateway-ExclusiveGateway-5ba11db1-6eea-4bd4-8429-967820f1f1ae\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"true\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"858\" y=\"185\" width=\"50\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-962b7602-59cb-40c8-b2aa-4568659b603f\" bpmnElement=\"serviceTask-962b7602-59cb-40c8-b2aa-4568659b603f\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1500\" y=\"-5\" width=\"140\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-15f09883-89f5-45c5-a454-76fa3361f8c3\" bpmnElement=\"serviceTask-15f09883-89f5-45c5-a454-76fa3361f8c3\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1490\" y=\"195\" width=\"160\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-2fb0c785-772a-4426-b1c2-0df0e849fb42\" bpmnElement=\"Event-2fb0c785-772a-4426-b1c2-0df0e849fb42\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1770\" y=\"192.5\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-a16965ae-65f3-4c0f-b204-34da26100bc6\" bpmnElement=\"serviceTask-a16965ae-65f3-4c0f-b204-34da26100bc6\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1989\" y=\"197.5\" width=\"100\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-4d469b95-e5a3-4d30-be30-6315e9b29f82\" bpmnElement=\"serviceTask-4d469b95-e5a3-4d30-be30-6315e9b29f82\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2180\" y=\"235\" width=\"130\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-6bb73c06-2d6d-4083-85c2-cbb1de8f7830\" bpmnElement=\"Event-6bb73c06-2d6d-4083-85c2-cbb1de8f7830\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2350.5\" y=\"230\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-0060a3f7-c67f-40e8-8f35-df29bd0bf06b\" bpmnElement=\"serviceTask-0060a3f7-c67f-40e8-8f35-df29bd0bf06b\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2471\" y=\"232.5\" width=\"110\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-e9789efb-b382-4628-92c6-89df357e1a62\" bpmnElement=\"serviceTask-e9789efb-b382-4628-92c6-89df357e1a62\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"229\" y=\"532.5\" width=\"100\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-fad5337e-01b0-4a86-a925-1717a70f4e85\" bpmnElement=\"Event-fad5337e-01b0-4a86-a925-1717a70f4e85\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"451\" y=\"530\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_exclusiveGateway-ExclusiveGateway-0c38f4a0-fd74-46b0-8b15-883dd8c979b1\" bpmnElement=\"exclusiveGateway-ExclusiveGateway-0c38f4a0-fd74-46b0-8b15-883dd8c979b1\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"true\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"838\" y=\"527.5\" width=\"50\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-c177d4db-6f0f-4d99-80a2-b0c0d93da311\" bpmnElement=\"serviceTask-c177d4db-6f0f-4d99-80a2-b0c0d93da311\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"793\" y=\"775\" width=\"140\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-f57a95ff-8165-40ea-adf4-085f5f482721\" bpmnElement=\"serviceTask-f57a95ff-8165-40ea-adf4-085f5f482721\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1605\" y=\"787.5\" width=\"210\" height=\"70\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_exclusiveGateway-ExclusiveGateway-06fe1d15-8cac-463c-bdb1-5261fbf01ded\" bpmnElement=\"exclusiveGateway-ExclusiveGateway-06fe1d15-8cac-463c-bdb1-5261fbf01ded\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"true\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"-40\" y=\"350\" width=\"50\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-ef565a8d-91a2-43ff-ad76-88f96fc5c592\" bpmnElement=\"serviceTask-ef565a8d-91a2-43ff-ad76-88f96fc5c592\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1361\" y=\"710\" width=\"120\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_endEvent-f8b197bf-afd8-4454-8737-579f3131c7f4\" bpmnElement=\"endEvent-f8b197bf-afd8-4454-8737-579f3131c7f4\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"860.5\" y=\"-146\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_endEvent-c5c3dc35-f3f9-433f-ac36-086cc404b5c0\" bpmnElement=\"endEvent-c5c3dc35-f3f9-433f-ac36-086cc404b5c0\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"840.5\" y=\"930\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-e018c59b-ceba-4821-97ff-d929651fae3d\" bpmnElement=\"Event-e018c59b-ceba-4821-97ff-d929651fae3d\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1974\" y=\"787.5\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-abf72dcd-0723-4ee8-b862-6858bf6b79e4\" bpmnElement=\"serviceTask-abf72dcd-0723-4ee8-b862-6858bf6b79e4\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2170\" y=\"847.5\" width=\"110\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-6574e020-fbad-4027-8cd2-bdf9ac9958a1\" bpmnElement=\"serviceTask-6574e020-fbad-4027-8cd2-bdf9ac9958a1\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2310\" y=\"847.5\" width=\"140\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-6d62cd93-72ca-43a4-aa0c-812ae5ea2d33\" bpmnElement=\"Event-6d62cd93-72ca-43a4-aa0c-812ae5ea2d33\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2503.5\" y=\"845\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-2edcaeba-c111-418d-afcb-29c36ea9460a\" bpmnElement=\"serviceTask-2edcaeba-c111-418d-afcb-29c36ea9460a\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2750\" y=\"455\" width=\"110\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_endEvent-7206bd23-cab8-4895-9a30-1c0bb06d7bf4\" bpmnElement=\"endEvent-7206bd23-cab8-4895-9a30-1c0bb06d7bf4\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"3020\" y=\"410\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_exclusiveGateway-ExclusiveGateway-d8f96d5c-e410-4a84-998b-c41cdc33b82b\" bpmnElement=\"exclusiveGateway-ExclusiveGateway-d8f96d5c-e410-4a84-998b-c41cdc33b82b\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"true\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1540\" y=\"625\" width=\"50\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-fce7170b-7d3e-47ff-a86c-ecff28d7cebb\" bpmnElement=\"serviceTask-fce7170b-7d3e-47ff-a86c-ecff28d7cebb\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1725\" y=\"460\" width=\"90\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-da00243d-3d85-4416-ae53-c0316b6fc3bf\" bpmnElement=\"Event-da00243d-3d85-4416-ae53-c0316b6fc3bf\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1929\" y=\"455\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-e2cdf56c-a5a9-4ad6-bdd4-60641322f649\" bpmnElement=\"serviceTask-e2cdf56c-a5a9-4ad6-bdd4-60641322f649\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2070\" y=\"436\" width=\"110\" height=\"70\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-c3ae26fe-ab2a-4432-9c4c-02742fb9f2a8\" bpmnElement=\"serviceTask-c3ae26fe-ab2a-4432-9c4c-02742fb9f2a8\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2320\" y=\"455\" width=\"130\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-c65c1539-d28d-4d0a-aede-251553ad7e39\" bpmnElement=\"Event-c65c1539-d28d-4d0a-aede-251553ad7e39\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2581\" y=\"455\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-64df1928-4d13-4ad0-9361-6ffe94b87b0f\" bpmnElement=\"serviceTask-64df1928-4d13-4ad0-9361-6ffe94b87b0f\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2935\" y=\"847.5\" width=\"130\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-43a87738-370d-4528-9e76-77cc0968da14\" bpmnElement=\"Event-43a87738-370d-4528-9e76-77cc0968da14\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"3147\" y=\"845\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-41737f78-3587-4405-96d1-cc9cbb525438\" bpmnElement=\"serviceTask-41737f78-3587-4405-96d1-cc9cbb525438\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"3232\" y=\"847.5\" width=\"120\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-5a42df5a-531b-4e9d-8748-7943aba65101\" bpmnElement=\"serviceTask-5a42df5a-531b-4e9d-8748-7943aba65101\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"3212\" y=\"1033\" width=\"160\" height=\"90\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-9c49bb62-82fb-490d-bf30-d19963dbd8b7\" bpmnElement=\"Event-9c49bb62-82fb-490d-bf30-d19963dbd8b7\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"3010\" y=\"1055.5\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-cc9e15a5-a2d9-4087-b474-a4e4a5c375f7\" bpmnElement=\"serviceTask-cc9e15a5-a2d9-4087-b474-a4e4a5c375f7\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2740\" y=\"1055.5\" width=\"110\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_endEvent-5b35d890-2a9c-4c04-bda1-3c6ed54cde82\" bpmnElement=\"endEvent-5b35d890-2a9c-4c04-bda1-3c6ed54cde82\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2536\" y=\"1058\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-2cd26aa2-a2ee-413e-af02-31a721664b5a\" bpmnElement=\"serviceTask-2cd26aa2-a2ee-413e-af02-31a721664b5a\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1251\" y=\"180\" width=\"110\" height=\"60\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-c871ddc2-a099-4f32-8134-05027e218ded\" bpmnElement=\"serviceTask-c871ddc2-a099-4f32-8134-05027e218ded\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1127\" y=\"522.5\" width=\"110\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-aafd35b7-c3ba-4c79-9e98-75954bc26ebe\" bpmnElement=\"serviceTask-aafd35b7-c3ba-4c79-9e98-75954bc26ebe\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1251\" y=\"-20\" width=\"110\" height=\"70\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-32a1a3ed-e447-4c32-a5d7-2a343d5f6a56\" bpmnElement=\"serviceTask-32a1a3ed-e447-4c32-a5d7-2a343d5f6a56\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1127\" y=\"705\" width=\"110\" height=\"50\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-77cb4373-e84d-448f-b2eb-20f37aba701f\" bpmnElement=\"serviceTask-77cb4373-e84d-448f-b2eb-20f37aba701f\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"643\" y=\"192\" width=\"66\" height=\"36\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-69311060-bb86-4c8e-bcfd-eeb31f8b8dd1\" bpmnElement=\"serviceTask-69311060-bb86-4c8e-bcfd-eeb31f8b8dd1\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"631\" y=\"534.5\" width=\"66\" height=\"36\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-2286251d-38ee-499a-9b19-4d9083e455c8\" bpmnElement=\"serviceTask-2286251d-38ee-499a-9b19-4d9083e455c8\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"-270\" y=\"357\" width=\"66\" height=\"36\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-6d62f328-ed7a-4aea-8c71-90ce543218d3\" bpmnElement=\"serviceTask-6d62f328-ed7a-4aea-8c71-90ce543218d3\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1538\" y=\"103\" width=\"80\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-1f95516b-5b5b-4251-bd82-5669f97a1693\" bpmnElement=\"Event-1f95516b-5b5b-4251-bd82-5669f97a1693\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1693\" y=\"103\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-c70332e6-f162-4f97-98cf-dde2136d0f8e\" bpmnElement=\"serviceTask-c70332e6-f162-4f97-98cf-dde2136d0f8e\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1424\" y=\"462\" width=\"66\" height=\"36\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-01a9700a-8096-465e-9818-3ab853991403\" bpmnElement=\"Event-01a9700a-8096-465e-9818-3ab853991403\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1565\" y=\"457.5\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-2e79854f-0104-4730-a64c-6d41fd041d5a\" bpmnElement=\"serviceTask-2e79854f-0104-4730-a64c-6d41fd041d5a\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1660\" y=\"633\" width=\"170\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-1bcf040c-c8b1-472d-9d86-445427ad1595\" bpmnElement=\"Event-1bcf040c-c8b1-472d-9d86-445427ad1595\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"1920\" y=\"628\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_serviceTask-44d9c837-28e6-4fe2-8959-4265b325d0a9\" bpmnElement=\"serviceTask-44d9c837-28e6-4fe2-8959-4265b325d0a9\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2626\" y=\"851\" width=\"120\" height=\"40\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNShape id=\"Shape_Event-ece747e1-9247-47f6-98ef-e04f761058b6\" bpmnElement=\"Event-ece747e1-9247-47f6-98ef-e04f761058b6\" isHorizontal=\"true\" isExpanded=\"false\" isMarkerVisible=\"false\" isMessageVisible=\"false\">\n" +
                "        <dc:Bounds x=\"2815\" y=\"859\" width=\"45\" height=\"45\" />\n" +
                "      </bpmndi:BPMNShape>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-341e874f-affa-4fdf-a6e5-3c6cd18ae9b2\" bpmnElement=\"Flow-341e874f-affa-4fdf-a6e5-3c6cd18ae9b2\">\n" +
                "        <di:waypoint x=\"2526\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"2686\" y=\"871\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-8129375d-8c67-4d85-aefb-d7b9f53997c5\" bpmnElement=\"Flow-8129375d-8c67-4d85-aefb-d7b9f53997c5\">\n" +
                "        <di:waypoint x=\"2686\" y=\"871\" />\n" +
                "        <di:waypoint x=\"2837.5\" y=\"881.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-51453cef-ed8b-4f64-8b59-bb89c9b52024\" bpmnElement=\"Flow-51453cef-ed8b-4f64-8b59-bb89c9b52024\">\n" +
                "        <di:waypoint x=\"2837.5\" y=\"881.5\" />\n" +
                "        <di:waypoint x=\"3000\" y=\"867.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-c9a4916d-4e62-4ffe-803c-fa933f64da57\" bpmnElement=\"Flow-c9a4916d-4e62-4ffe-803c-fa933f64da57\">\n" +
                "        <di:waypoint x=\"1570\" y=\"15\" />\n" +
                "        <di:waypoint x=\"1574\" y=\"15\" />\n" +
                "        <di:waypoint x=\"1574\" y=\"123\" />\n" +
                "        <di:waypoint x=\"1578\" y=\"123\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-18461c7e-225e-4185-b0bd-c60a74ce6056\" bpmnElement=\"Flow-18461c7e-225e-4185-b0bd-c60a74ce6056\">\n" +
                "        <di:waypoint x=\"1578\" y=\"123\" />\n" +
                "        <di:waypoint x=\"1715.5\" y=\"125.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-161fc5fb-969d-486a-b739-1a8bca3dab64\" bpmnElement=\"Flow-161fc5fb-969d-486a-b739-1a8bca3dab64\">\n" +
                "        <di:waypoint x=\"1715.5\" y=\"125.5\" />\n" +
                "        <di:waypoint x=\"1642.75\" y=\"125.5\" />\n" +
                "        <di:waypoint x=\"1642.75\" y=\"215\" />\n" +
                "        <di:waypoint x=\"1570\" y=\"215\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-e8a5af69-ec4a-4a5f-b2d4-0f1a5e1ad43c\" bpmnElement=\"Flow-e8a5af69-ec4a-4a5f-b2d4-0f1a5e1ad43c\">\n" +
                "        <di:waypoint x=\"1951.5\" y=\"477.5\" />\n" +
                "        <di:waypoint x=\"2125\" y=\"471\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-92289535-d7f7-46c1-b176-aeb18a7a3d53\" bpmnElement=\"Flow-92289535-d7f7-46c1-b176-aeb18a7a3d53\">\n" +
                "        <di:waypoint x=\"1457\" y=\"480\" />\n" +
                "        <di:waypoint x=\"1587.5\" y=\"480\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-56a23dce-e3b2-4258-bd09-80586f146019\" bpmnElement=\"Flow-56a23dce-e3b2-4258-bd09-80586f146019\">\n" +
                "        <di:waypoint x=\"1587.5\" y=\"480\" />\n" +
                "        <di:waypoint x=\"1770\" y=\"480\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-07945d47-09d8-4825-8c8d-ff712cea3a9d\" bpmnElement=\"Flow-07945d47-09d8-4825-8c8d-ff712cea3a9d\">\n" +
                "        <di:waypoint x=\"1565\" y=\"650\" />\n" +
                "        <di:waypoint x=\"1511\" y=\"650\" />\n" +
                "        <di:waypoint x=\"1511\" y=\"480\" />\n" +
                "        <di:waypoint x=\"1457\" y=\"480\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-15061aa4-b741-4c06-bdb1-35f1cd341599\" bpmnElement=\"Flow-15061aa4-b741-4c06-bdb1-35f1cd341599\">\n" +
                "        <di:waypoint x=\"1710\" y=\"822.5\" />\n" +
                "        <di:waypoint x=\"1996.5\" y=\"810\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-028a3aa9-480a-4fa8-86ca-73183276c55c\" bpmnElement=\"Flow-028a3aa9-480a-4fa8-86ca-73183276c55c\">\n" +
                "        <di:waypoint x=\"1565\" y=\"650\" />\n" +
                "        <di:waypoint x=\"1745\" y=\"653\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-e2ce2ada-f258-457f-8cbd-77c8dc12730f\" bpmnElement=\"Flow-e2ce2ada-f258-457f-8cbd-77c8dc12730f\">\n" +
                "        <di:waypoint x=\"1745\" y=\"653\" />\n" +
                "        <di:waypoint x=\"1942.5\" y=\"650.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-36154ba2-5531-4de3-bac5-9642246c530f\" bpmnElement=\"Flow-36154ba2-5531-4de3-bac5-9642246c530f\">\n" +
                "        <di:waypoint x=\"1942.5\" y=\"650.5\" />\n" +
                "        <di:waypoint x=\"1826.25\" y=\"650.5\" />\n" +
                "        <di:waypoint x=\"1826.25\" y=\"822.5\" />\n" +
                "        <di:waypoint x=\"1710\" y=\"822.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-e8ca9d8a-74dc-4bf4-9a8e-f95075574443\" bpmnElement=\"Flow-e8ca9d8a-74dc-4bf4-9a8e-f95075574443\">\n" +
                "        <di:waypoint x=\"-431\" y=\"375\" />\n" +
                "        <di:waypoint x=\"-237\" y=\"375\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-ec62b810-2680-41a8-aa39-416149954302\" bpmnElement=\"Flow-ec62b810-2680-41a8-aa39-416149954302\">\n" +
                "        <di:waypoint x=\"-237\" y=\"375\" />\n" +
                "        <di:waypoint x=\"-15\" y=\"375\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-40287693-d65f-48c7-b3fa-b9f29eda41eb\" bpmnElement=\"Flow-40287693-d65f-48c7-b3fa-b9f29eda41eb\">\n" +
                "        <di:waypoint x=\"483.5\" y=\"210\" />\n" +
                "        <di:waypoint x=\"676\" y=\"210\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-1b08432d-15c2-47d1-a5e9-b446cef5db6b\" bpmnElement=\"Flow-1b08432d-15c2-47d1-a5e9-b446cef5db6b\">\n" +
                "        <di:waypoint x=\"676\" y=\"210\" />\n" +
                "        <di:waypoint x=\"883\" y=\"210\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-72e7ff98-4fe1-4ecd-96ab-b69fd7d17370\" bpmnElement=\"Flow-72e7ff98-4fe1-4ecd-96ab-b69fd7d17370\">\n" +
                "        <di:waypoint x=\"473.5\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"664\" y=\"552.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-c0e8fe91-77bc-47f1-bfcb-254cb2e6d487\" bpmnElement=\"Flow-c0e8fe91-77bc-47f1-bfcb-254cb2e6d487\">\n" +
                "        <di:waypoint x=\"664\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"863\" y=\"552.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-e6ce235a-5df3-412b-af5f-44569ea80e67\" bpmnElement=\"Flow-e6ce235a-5df3-412b-af5f-44569ea80e67\">\n" +
                "        <di:waypoint x=\"1306\" y=\"210\" />\n" +
                "        <di:waypoint x=\"1306\" y=\"210\" />\n" +
                "        <di:waypoint x=\"1306\" y=\"15\" />\n" +
                "        <di:waypoint x=\"1306\" y=\"15\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-a9e76a1c-4535-451e-8153-76f23709cad0\" bpmnElement=\"Flow-a9e76a1c-4535-451e-8153-76f23709cad0\">\n" +
                "        <di:waypoint x=\"1306\" y=\"15\" />\n" +
                "        <di:waypoint x=\"1570\" y=\"15\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-52023362-1715-4412-bdc2-2f827bb6c48b\" bpmnElement=\"Flow-52023362-1715-4412-bdc2-2f827bb6c48b\">\n" +
                "        <di:waypoint x=\"1182\" y=\"547.5\" />\n" +
                "        <di:waypoint x=\"1182\" y=\"547.5\" />\n" +
                "        <di:waypoint x=\"1182\" y=\"730\" />\n" +
                "        <di:waypoint x=\"1182\" y=\"730\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-d5de1aa4-e70e-4f37-8bfd-2a51fb07a29e\" bpmnElement=\"Flow-d5de1aa4-e70e-4f37-8bfd-2a51fb07a29e\">\n" +
                "        <di:waypoint x=\"1182\" y=\"730\" />\n" +
                "        <di:waypoint x=\"1421\" y=\"730\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-a7b42068-c021-4e6c-98ff-cecd317b1191\" bpmnElement=\"Flow-a7b42068-c021-4e6c-98ff-cecd317b1191\">\n" +
                "        <di:waypoint x=\"883\" y=\"210\" />\n" +
                "        <di:waypoint x=\"1306\" y=\"210\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-0b7b0790-df79-47eb-b77c-a7a348ec7ba1\" bpmnElement=\"Flow-0b7b0790-df79-47eb-b77c-a7a348ec7ba1\">\n" +
                "        <di:waypoint x=\"863\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"1182\" y=\"547.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-550600a4-43ad-48b5-a8a5-436a6aec6cef\" bpmnElement=\"Flow-550600a4-43ad-48b5-a8a5-436a6aec6cef\">\n" +
                "        <di:waypoint x=\"3000\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"3169.5\" y=\"867.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-86509e68-eb19-4a06-884d-c52265ebf754\" bpmnElement=\"Flow-86509e68-eb19-4a06-884d-c52265ebf754\">\n" +
                "        <di:waypoint x=\"3169.5\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"3292\" y=\"867.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-5a4477f1-23cd-4f93-a946-951ff5426449\" bpmnElement=\"Flow-5a4477f1-23cd-4f93-a946-951ff5426449\">\n" +
                "        <di:waypoint x=\"3292\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"3292\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"3292\" y=\"1078\" />\n" +
                "        <di:waypoint x=\"3292\" y=\"1078\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-b4547df7-8df2-4542-94d8-9eaaec849364\" bpmnElement=\"Flow-b4547df7-8df2-4542-94d8-9eaaec849364\">\n" +
                "        <di:waypoint x=\"3292\" y=\"1078\" />\n" +
                "        <di:waypoint x=\"3032.5\" y=\"1078\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-449b2511-45c1-4383-831b-b92f29d118be\" bpmnElement=\"Flow-449b2511-45c1-4383-831b-b92f29d118be\">\n" +
                "        <di:waypoint x=\"3032.5\" y=\"1078\" />\n" +
                "        <di:waypoint x=\"2795\" y=\"1080.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-77928878-66c0-446d-ba76-09e4bc85a9ba\" bpmnElement=\"Flow-77928878-66c0-446d-ba76-09e4bc85a9ba\">\n" +
                "        <di:waypoint x=\"2795\" y=\"1080.5\" />\n" +
                "        <di:waypoint x=\"2558.5\" y=\"1080.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-27a76ff0-3e8f-4b28-ac27-2b4a20a1ed9e\" bpmnElement=\"Flow-27a76ff0-3e8f-4b28-ac27-2b4a20a1ed9e\">\n" +
                "        <di:waypoint x=\"2125\" y=\"471\" />\n" +
                "        <di:waypoint x=\"2385\" y=\"475\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-2bd8d76a-74f8-401e-b300-6e0cb536c4b4\" bpmnElement=\"Flow-2bd8d76a-74f8-401e-b300-6e0cb536c4b4\">\n" +
                "        <di:waypoint x=\"2385\" y=\"475\" />\n" +
                "        <di:waypoint x=\"2603.5\" y=\"477.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-6d23d15c-1964-4d34-9903-3948491dd979\" bpmnElement=\"Flow-6d23d15c-1964-4d34-9903-3948491dd979\">\n" +
                "        <di:waypoint x=\"2603.5\" y=\"477.5\" />\n" +
                "        <di:waypoint x=\"2805\" y=\"480\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-2824e340-edb6-456c-8fd6-dcc92030cdf1\" bpmnElement=\"Flow-2824e340-edb6-456c-8fd6-dcc92030cdf1\">\n" +
                "        <di:waypoint x=\"1421\" y=\"730\" />\n" +
                "        <di:waypoint x=\"1493\" y=\"730\" />\n" +
                "        <di:waypoint x=\"1493\" y=\"650\" />\n" +
                "        <di:waypoint x=\"1565\" y=\"650\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-55550eb8-9d06-4af4-9284-34b7e05ed789\" bpmnElement=\"Flow-55550eb8-9d06-4af4-9284-34b7e05ed789\">\n" +
                "        <di:waypoint x=\"1770\" y=\"480\" />\n" +
                "        <di:waypoint x=\"1951.5\" y=\"477.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-51c7c2f7-cb57-4266-b8f0-e701701eb3de\" bpmnElement=\"Flow-51c7c2f7-cb57-4266-b8f0-e701701eb3de\">\n" +
                "        <di:waypoint x=\"1996.5\" y=\"810\" />\n" +
                "        <di:waypoint x=\"2110.75\" y=\"810\" />\n" +
                "        <di:waypoint x=\"2110.75\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"2225\" y=\"867.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-944fb86b-f3eb-44c3-8fdb-4c876ff16bed\" bpmnElement=\"Flow-944fb86b-f3eb-44c3-8fdb-4c876ff16bed\">\n" +
                "        <di:waypoint x=\"2225\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"2380\" y=\"867.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-bb0d89ba-81df-4d70-bd8b-c9ef80590e5e\" bpmnElement=\"Flow-bb0d89ba-81df-4d70-bd8b-c9ef80590e5e\">\n" +
                "        <di:waypoint x=\"2380\" y=\"867.5\" />\n" +
                "        <di:waypoint x=\"2526\" y=\"867.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-dafebdb6-16bd-49fe-971e-88a774c0fad6\" bpmnElement=\"Flow-dafebdb6-16bd-49fe-971e-88a774c0fad6\">\n" +
                "        <di:waypoint x=\"2805\" y=\"480\" />\n" +
                "        <di:waypoint x=\"2923.75\" y=\"480\" />\n" +
                "        <di:waypoint x=\"2923.75\" y=\"432.5\" />\n" +
                "        <di:waypoint x=\"3042.5\" y=\"432.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-687013c1-b7c9-4b9b-98b5-64485c5e2689\" bpmnElement=\"Flow-687013c1-b7c9-4b9b-98b5-64485c5e2689\">\n" +
                "        <di:waypoint x=\"-15\" y=\"375\" />\n" +
                "        <di:waypoint x=\"127\" y=\"375\" />\n" +
                "        <di:waypoint x=\"127\" y=\"210\" />\n" +
                "        <di:waypoint x=\"269\" y=\"210\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-0ae0fb08-32b0-442e-ad4e-3977c21d3678\" bpmnElement=\"Flow-0ae0fb08-32b0-442e-ad4e-3977c21d3678\">\n" +
                "        <di:waypoint x=\"-15\" y=\"375\" />\n" +
                "        <di:waypoint x=\"132\" y=\"375\" />\n" +
                "        <di:waypoint x=\"132\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"279\" y=\"552.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-5ba2444f-938f-4119-b0b8-48d022d358e9\" bpmnElement=\"Flow-5ba2444f-938f-4119-b0b8-48d022d358e9\">\n" +
                "        <di:waypoint x=\"883\" y=\"40\" />\n" +
                "        <di:waypoint x=\"883\" y=\"40\" />\n" +
                "        <di:waypoint x=\"883\" y=\"-123.5\" />\n" +
                "        <di:waypoint x=\"883\" y=\"-123.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-3c172a87-c277-4fd6-a95f-8ed128e5c0a8\" bpmnElement=\"Flow-3c172a87-c277-4fd6-a95f-8ed128e5c0a8\">\n" +
                "        <di:waypoint x=\"863\" y=\"795\" />\n" +
                "        <di:waypoint x=\"863\" y=\"795\" />\n" +
                "        <di:waypoint x=\"863\" y=\"952.5\" />\n" +
                "        <di:waypoint x=\"863\" y=\"952.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-880eba0e-4d74-4fb4-8024-49b3abce25ec\" bpmnElement=\"Flow-880eba0e-4d74-4fb4-8024-49b3abce25ec\">\n" +
                "        <di:waypoint x=\"279\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"473.5\" y=\"552.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-ccfa102a-31f3-4348-8c3f-7a9e69bdb3c5\" bpmnElement=\"Flow-ccfa102a-31f3-4348-8c3f-7a9e69bdb3c5\">\n" +
                "        <di:waypoint x=\"863\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"863\" y=\"552.5\" />\n" +
                "        <di:waypoint x=\"863\" y=\"795\" />\n" +
                "        <di:waypoint x=\"863\" y=\"795\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-dd419f4b-ea9f-44f6-ac82-b62d4ecec488\" bpmnElement=\"Flow-dd419f4b-ea9f-44f6-ac82-b62d4ecec488\">\n" +
                "        <di:waypoint x=\"1792.5\" y=\"215\" />\n" +
                "        <di:waypoint x=\"2039\" y=\"217.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-6aa2ccce-6901-478b-800b-13aa0be1f7b8\" bpmnElement=\"Flow-6aa2ccce-6901-478b-800b-13aa0be1f7b8\">\n" +
                "        <di:waypoint x=\"269\" y=\"210\" />\n" +
                "        <di:waypoint x=\"483.5\" y=\"210\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-26acb5ee-3868-4e24-a877-fbc5e9a1f613\" bpmnElement=\"Flow-26acb5ee-3868-4e24-a877-fbc5e9a1f613\">\n" +
                "        <di:waypoint x=\"883\" y=\"210\" />\n" +
                "        <di:waypoint x=\"883\" y=\"210\" />\n" +
                "        <di:waypoint x=\"883\" y=\"40\" />\n" +
                "        <di:waypoint x=\"883\" y=\"40\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-b3be735c-45a2-44fe-be85-9ec2232f6ba9\" bpmnElement=\"Flow-b3be735c-45a2-44fe-be85-9ec2232f6ba9\">\n" +
                "        <di:waypoint x=\"1570\" y=\"215\" />\n" +
                "        <di:waypoint x=\"1792.5\" y=\"215\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-08824249-49b8-4dbd-b1cf-17fef15e1c5c\" bpmnElement=\"Flow-08824249-49b8-4dbd-b1cf-17fef15e1c5c\">\n" +
                "        <di:waypoint x=\"2039\" y=\"217.5\" />\n" +
                "        <di:waypoint x=\"2142\" y=\"217.5\" />\n" +
                "        <di:waypoint x=\"2142\" y=\"255\" />\n" +
                "        <di:waypoint x=\"2245\" y=\"255\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-945150f9-273c-4d37-9b30-d8139ffdf043\" bpmnElement=\"Flow-945150f9-273c-4d37-9b30-d8139ffdf043\">\n" +
                "        <di:waypoint x=\"2245\" y=\"255\" />\n" +
                "        <di:waypoint x=\"2373\" y=\"252.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-7bd69ef6-a055-415c-9c6a-9c7649bd3bbf\" bpmnElement=\"Flow-7bd69ef6-a055-415c-9c6a-9c7649bd3bbf\">\n" +
                "        <di:waypoint x=\"2373\" y=\"252.5\" />\n" +
                "        <di:waypoint x=\"2526\" y=\"252.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "      <bpmndi:BPMNEdge id=\"BPMNEdge_Flow-3a8820ae-ec9c-4b82-a27b-12a922ccb325\" bpmnElement=\"Flow-3a8820ae-ec9c-4b82-a27b-12a922ccb325\">\n" +
                "        <di:waypoint x=\"2526\" y=\"252.5\" />\n" +
                "        <di:waypoint x=\"2762.5\" y=\"252.5\" />\n" +
                "      </bpmndi:BPMNEdge>\n" +
                "    </bpmndi:BPMNPlane>\n" +
                "  </bpmndi:BPMNDiagram>\n" +
                "</bpmn:definitions>\n");
        workflowDefinition.setCreatedAt(LocalDateTime.now());
        workflowDefinition.setUpdatedAt(LocalDateTime.now());

        when(workflowDefinitionCommandService.createAndDeploy(eq(wareHouseId), eq(workflowDefinition)))
                .thenReturn(workflowDefinition);

        String responseContent = mockMvc.perform(post("/api/{wareHouseId}/workflow-definitions", wareHouseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workflowDefinition)))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Create Response: " + responseContent);
    }

    /**
     * 测试更新流程定义并重新部署到工作流接口 (PUT /api/{wareHouseId}/workflow-definitions/{id})
     */
    @Test
    @Transactional
    @Rollback(value = false)
    @DisplayName("测试更新流程定义并重新部署到工作流接口")
    void testUpdate() throws Exception {
        Long wareHouseId = 1L;
        Long id = 1L;
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setWarehouseId(wareHouseId.intValue());

        when(workflowDefinitionCommandService.updateAndRedeploy(eq(wareHouseId), eq(id), eq(workflowDefinition)))
                .thenReturn(workflowDefinition);

        String responseContent = mockMvc.perform(put("/api/{wareHouseId}/workflow-definitions/{id}", wareHouseId, id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workflowDefinition)))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Update Response: " + responseContent);
    }

    /**
     * 测试物理删除流程定义接口 (DELETE /api/{wareHouseId}/workflow-definitions/{id})
     */
    @Test
    @DisplayName("测试物理删除流程定义接口")
    void testDelete() throws Exception {
        Long wareHouseId = 1L;
        Long id = 1L;

        doNothing().when(workflowDefinitionCommandService).deletePhysically(eq(wareHouseId), eq(id));

        String responseContent = mockMvc.perform(delete("/api/{wareHouseId}/workflow-definitions/{id}", wareHouseId, id))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Delete Response: " + responseContent);
    }

    /**
     * 测试根据业务类型查询流程定义接口 (GET /api/{wareHouseId}/workflow-definitions/biz-type/{bizType})
     */
    @Test
    @DisplayName("测试根据业务类型查询流程定义接口")
    void testFindByBizType() throws Exception {
        Long wareHouseId = 1L;
        String bizType = "exampleBizType";

        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setWarehouseId(wareHouseId.intValue());

        when(workflowDefinitionsService.findByBizType(eq(wareHouseId), eq(bizType)))
                .thenReturn(workflowDefinition);

        String responseContent = mockMvc.perform(get("/api/{wareHouseId}/workflow-definitions/biz-type/{bizType}", wareHouseId, bizType))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Find by BizType Response: " + responseContent);
    }

    /**
     * 测试根据流程ID查询流程定义接口 (GET /api/{wareHouseId}/workflow-definitions/workflow/{workflowId})
     */
    @Test
    @DisplayName("测试根据流程ID查询流程定义接口")
    void testQueryByWorkflowId() throws Exception {
        Long wareHouseId = 1L;
        String workflowId = "workflow123";

        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        workflowDefinition.setWarehouseId(wareHouseId.intValue());

        when(workflowDefinitionsService.queryByWorkflowId(eq(wareHouseId), eq(workflowId)))
                .thenReturn(workflowDefinition);

        String responseContent = mockMvc.perform(get("/api/{wareHouseId}/workflow-definitions/workflow/{workflowId}", wareHouseId, workflowId))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn().getResponse().getContentAsString();

        System.out.println("Query by WorkflowId Response: " + responseContent);
    }
}