# 03 - Domain Model

## 1. 核心实体

### 1.1 Task
- id
- warehouseId
- upstreamTaskId
- type（OUTBOUND/INBOUND/INVENTORY/RELOCATE）
- status（CREATED/READY/RUNNING/SUSPENDED/FAILED/COMPLETED/CANCELED）
- priority
- payloadJson
- createdAt/updatedAt

### 1.2 Plan
- id
- warehouseId
- taskId
- status（CREATED/RUNNING/COMPLETED/FAILED）

### 1.3 Step
- id
- warehouseId
- planId
- seq
- type（如 ASRS_PICK / ASRS_DROP / CHANGE_AISLE / TEMP_RELOCATE / RCS_MOVE）
- status（PENDING/RUNNING/DONE/FAILED/SKIPPED）
- payloadJson
- resourceLocksJson

### 1.4 CommandExecution
- id
- warehouseId
- taskId/planId/stepId
- domain（S7/MODBUS/RCS）
- deviceId
- commandType
- idempotencyKey
- status（SENT/ACK/RUNNING/DONE/ERROR/TIMEOUT/CANCELED）
- requestJson/responseJson
- traceId/correlationId
- startedAt/endedAt

### 1.5 ResourceLock
- warehouseId
- lockKey
- ownerType（PLAN/STEP）
- ownerId
- status
- expireAt

## 2. 状态流转规则（核心）
- Task: CREATED→READY→RUNNING→(COMPLETED|SUSPENDED|FAILED|CANCELED)
- Step: PENDING→RUNNING→(DONE|FAILED|SKIPPED)
- Command: SENT→ACK→RUNNING→(DONE|ERROR|TIMEOUT|CANCELED)

## 3. 领域事件
- TaskCreated/Started/Completed/Failed/Suspended
- PlanGenerated
- StepStarted/Completed/Failed
- CommandSent/Done/Failed
- AlarmRaised

## 4. 插件 SPI（接口级）
- StartGatePlugin
- RoutingPlugin
- PlanExpanderPlugin
- DispatchPolicyPlugin
- ExceptionHandlerPlugin

## 5. 异常语义
- 业务异常（放货有货/取货无货/资源冲突）→ BpmnError
- 技术异常（超时/断线/5xx）→ failure + retry/backoff


