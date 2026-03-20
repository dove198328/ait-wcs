# 01 - Requirements

## 1. 角色
- WMS：下发任务、查询状态、取消任务
- 现场运维：查看设备/流程状态，执行人工干预
- 第三方平台：AGV RCS（HTTP 接口）

## 2. 功能需求（FR）

### FR-1 任务类型
支持四类任务：
- OUTBOUND（出库）
- INBOUND（入库）
- INVENTORY（盘点）
- RELOCATE（移库）

### FR-2 双深位与临时移库
- 支持双深位货位模型
- 深位受阻时可自动插入临时移库步骤
- 临时移库可回滚/补偿，且幂等

### FR-3 同入同出
- 口/站台支持 IN/OUT/BOTH 模式
- 是否允许同口双向并发可配置（默认串行互斥）

### FR-4 堆垛机换巷道
- 作为可插拔能力，不同项目可开关
- 开启时，计划中可插入 ChangeAisle 步骤

### FR-5 设备与三方接入
- PLC：S7、Modbus（连接、读写、心跳、重连）
- AGV RCS：create/query/cancel，支持回调或轮询

### FR-6 异常处理
必须支持并策略化处理：
- 放货有货（Drop target occupied）
- 取货无货（Pick source empty）
- 通信异常（超时、断线、写失败、反馈丢失）
策略：复核、纠偏、改道、挂起、人工处理、告警

## 3. 工作流需求（WF）
- Camunda 7.19 嵌入式
- 以 `warehouseId` 作为仓库上下文
- 启动前经过 StartGate（可配置）
- businessKey 幂等：`warehouseId:upstreamTaskId:taskType`
- 业务异常 -> BpmnError；技术异常 -> failure/retry

## 4. 非功能需求（NFR）
- warehouse 隔离与查询过滤强约束
- 并发阈值按 warehouse 配置
- traceId/correlationId 全链路可追踪
- 指令审计与时间线可回放
- 配置化开关与责任链顺序可控

## 5. 最小 API
- `POST /api/{warehouseId}/tasks`
- `GET /api/{warehouseId}/tasks/{taskId}`
- `POST /api/{warehouseId}/tasks/{taskId}/cancel`
- `POST /api/{warehouseId}/tasks/{taskId}/ops/{action}`
- `GET /api/{warehouseId}/devices/status`



