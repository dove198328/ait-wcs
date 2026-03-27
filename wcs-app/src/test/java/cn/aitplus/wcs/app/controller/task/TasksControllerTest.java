package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.core.domain.model.Task;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TasksControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static Long wareHouseId = 1L;

    @Test
    @Order(1)
    @DisplayName("查询任务列表")
    void queryList() {
        String url = "/api/" + wareHouseId + "/tasks/search?pageNum=1&pageSize=10";
        Task query = new Task();
        query.setWarehouseId(wareHouseId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Task> entity = new HttpEntity<>(query, headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);
        System.out.println("=== 查询任务列表 ===");
        System.out.println(response.getBody());
    }

    @Test
    @Order(2)
    @DisplayName("根据ID查询任务")
    void queryById() {
        Long id = 6200L;
        String url = "/api/" + wareHouseId + "/tasks/" + id;
        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        System.out.println("=== 根据ID查询 ===");
        System.out.println(response.getBody());
    }

    @Test
    @Order(3)
    @DisplayName("新增任务")
    void create() {
        String url = "/api/" + wareHouseId + "/tasks/complete";
        Task task = new Task();
        task.setWorkflowDefId("workflow_1758110275067_2579");
        task.setTaskName("RK1");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Task> entity = new HttpEntity<>(task, headers);
        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);
        System.out.println("=== 新增任务 ===");
        System.out.println(response.getBody());
    }

    @Test
    @Order(4)
    @DisplayName("批量新增任务")
    void batchCreate() {
        String url = "/api/" + wareHouseId + "/tasks/batch";
        Task task = new Task();
        task.setWorkflowDefId("workflow_1758110275067_2579");
        task.setTaskName("RK1");
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        Task task1 = new Task();
        task1.setWorkflowDefId("workflow_1758110275067_2579");
        task1.setTaskName("RK1");
        task1.setCreatedAt(LocalDateTime.now());
        task1.setUpdatedAt(LocalDateTime.now());

        // ===== 组装 List =====
        List<Task> taskList = Arrays.asList(task, task1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<Task>> entity = new HttpEntity<>(taskList, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);

        System.out.println("=== 批量新增任务 ===");
        System.out.println(response.getBody());
    }

    @Test
    @Order(5)
    @DisplayName("修改任务")
    void update() {
        Long id = 6206L;
        String url = "/api/" + wareHouseId + "/tasks/" + id;
        Task task = new Task();
        task.setBizType("更新后的任务");
        task.setUpdatedAt(LocalDateTime.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Task> entity = new HttpEntity<>(task, headers);
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

        System.out.println("=== 更新任务 ===");
        System.out.println(response.getBody());
    }

    @Test
    @Order(6)
    @DisplayName("删除任务")
    void delete() {
        Long id = 6201L;
        String url = "/api/" + wareHouseId + "/tasks/" + id;
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
        System.out.println("=== 删除任务 ===");
        System.out.println(response.getBody());
    }
}