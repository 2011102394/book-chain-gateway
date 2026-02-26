# Book Chain Gateway

基于 Spring Boot 和 Hyperledger Fabric 的图书区块链溯源系统网关服务，用于将图书信息上链、查询和管理图书流转状态。

## 项目简介

本项目是一个图书区块链溯源系统的网关服务，通过 RESTful API 接口提供图书信息的上链、查询、更新、删除和历史轨迹查询等功能。系统基于 Hyperledger Fabric 区块链技术，确保图书信息的不可篡改和可追溯性。

## 技术栈

| 技术/框架 | 版本 | 用途 |
|---------|------|------|
| Spring Boot | 3.5.11 | 基础框架，提供 RESTful API 支持 |
| Java | 17 | 开发语言 |
| Hyperledger Fabric Gateway | 1.4.0 | 与 Fabric 区块链网络交互的 SDK |
| gRPC Netty | 1.53.0 | 提供与区块链节点的通信支持 |
| Lombok | 最新 | 简化代码，减少样板代码 |
| SLF4J | 最新 | 日志框架 |

## 功能特性

- **图书上链管理**：将新图书信息初始上链，包括图书ID、名称、出版社、位置等信息
- **图书状态查询**：查询指定图书的最新状态
- **图书流转更新**：更新图书的位置和状态，记录流转信息
- **图书记录删除**：从区块链中删除图书记录
- **图书历史轨迹查询**：查询图书的完整流转历史
- **区块链事件监听**：实时监听区块链网络的事件广播
- **统一响应格式**：所有接口返回统一的 JSON 格式响应
- **日志管理**：支持日志按天滚动和压缩，便于问题排查

## 项目结构

```
book-chain-gateway/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/arsc/bookchaingateway/
│   │   │       ├── trace/
│   │   │       │   ├── controller/
│   │   │       │   │   └── BookController.java    # REST API 控制器
│   │   │       │   ├── dto/
│   │   │       │   │   ├── ApiResponse.java         # 统一响应结构
│   │   │       │   │   └── BookDTO.java            # 数据传输对象
│   │   │       │   └── service/
│   │   │       │       └── FabricGatewayService.java  # 区块链交互服务
│   │   │       └── BookChainGatewayApplication.java   # 应用入口
│   │   └── resources/
│   │       ├── network/                            # 区块链网络配置文件
│   │       │   ├── tls-ca.crt                      # TLS CA 证书
│   │       │   ├── user-cert.pem                   # 用户证书
│   │       │   └── user-key.pem                    # 用户私钥
│   │       ├── application.yml                     # 应用配置文件
│   │       └── logback-spring.xml                  # 日志配置文件
│   └── test/
│       └── java/
│           └── com/arsc/bookchaingateway/
│               └── BookChainGatewayApplicationTests.java  # 测试类
├── pom.xml                                         # Maven 依赖配置
└── README.md                                       # 项目说明文档
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.6 或更高版本
- Hyperledger Fabric 区块链网络（已部署并运行）
- 网络连接到 Fabric peer 节点（默认：localhost:7051）

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/2011102394/book-chain-gateway.git
cd book-chain-gateway
```

### 2. 配置区块链网络

确保以下证书文件存在于 `src/main/resources/network/` 目录：
- `tls-ca.crt`：TLS CA 证书
- `user-cert.pem`：用户证书
- `user-key.pem`：用户私钥

### 3. 修改配置（可选）

如需修改区块链网络配置，请编辑 `FabricGatewayService.java` 中的以下参数：

```java
private static final String MSP_ID = "Org1MSP";
private static final String CHANNEL_NAME = "mychannel";
private static final String CHAINCODE_NAME = "booktrace";
private static final String PEER_ENDPOINT = "localhost:7051";
private static final String OVERRIDE_AUTH = "peer0.org1.example.com";
```

### 4. 编译项目

```bash
mvn clean install
```

### 5. 启动应用

```bash
mvn spring-boot:run
```

或者使用 Maven Wrapper：

```bash
./mvnw spring-boot:run
```

### 6. 验证启动

应用启动成功后，访问 `http://localhost:8080/api/books/{id}` 测试接口是否正常。

## 接口文档

### 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **响应格式**: 统一的 JSON 格式

### 响应格式

所有接口返回统一的响应格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": "响应数据"
}
```

- **code**: 状态码，200 表示成功，500 表示失败
- **msg**: 消息，成功为 "success"，失败为 "error"
- **data**: 响应数据，内容可以为空

### 接口列表

#### 1. 图书上链

**接口地址**: `POST /api/books`

**请求参数**:

```json
{
  "id": "BOOK001",
  "name": "Java编程思想",
  "publisher": "机械工业出版社",
  "location": "北京图书馆"
}
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": "图书上链成功"
}
```

**参数说明**:
- `id`: 图书ID（必填）
- `name`: 图书名称（必填）
- `publisher`: 出版社（必填）
- `location`: 当前位置（必填）

---

#### 2. 查询图书状态

**接口地址**: `GET /api/books/{id}`

**请求参数**:
- `id`: 图书ID（路径参数）

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": "{\"id\":\"BOOK001\",\"name\":\"Java编程思想\",\"publisher\":\"机械工业出版社\",\"location\":\"北京图书馆\",\"status\":\"在馆\"}"
}
```

---

#### 3. 更新图书信息

**接口地址**: `PUT /api/books/{id}`

**请求参数**:

```json
{
  "location": "上海图书馆",
  "status": "借出"
}
```

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": "图书信息更新成功"
}
```

**参数说明**:
- `location`: 新位置（必填）
- `status`: 新状态（必填）

---

#### 4. 删除图书

**接口地址**: `DELETE /api/books/{id}`

**请求参数**:
- `id`: 图书ID（路径参数）

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": "图书 [BOOK001] 已成功从当前账本状态中删除！"
}
```

---

#### 5. 查询图书历史轨迹

**接口地址**: `GET /api/books/{id}/history`

**请求参数**:
- `id`: 图书ID（路径参数）

**响应示例**:

```json
{
  "code": 200,
  "msg": "success",
  "data": "[{\"txId\":\"tx1\",\"timestamp\":\"2026-02-26T10:00:00Z\",\"location\":\"北京图书馆\",\"status\":\"在馆\"},{\"txId\":\"tx2\",\"timestamp\":\"2026-02-26T14:00:00Z\",\"location\":\"上海图书馆\",\"status\":\"借出\"}]"
}
```

---

## 日志配置

系统使用 SLF4J 日志框架，日志配置如下：

### 日志文件

- **主日志文件**: `logs/book-chain-gateway.log`
- **错误日志文件**: `logs/book-chain-gateway-error.log`
- **历史日志**: `logs/book-chain-gateway-2026-02-26.0.log.gz`（按天滚动并压缩）

### 日志级别

- **root**: INFO
- **com.arsc.bookchaingateway**: DEBUG

### 日志滚动策略

- **按天滚动**: 每天自动创建新的日志文件
- **文件大小**: 单个日志文件最大 10MB
- **保留时间**: 保留最近 30 天的日志
- **自动压缩**: 历史日志文件自动压缩为 .gz 格式

## 开发说明

### 添加新接口

1. 在 `BookController.java` 中添加新的接口方法
2. 在 `FabricGatewayService.java` 中添加对应的业务逻辑
3. 确保返回统一的 `ApiResponse` 格式
4. 添加适当的日志记录

### 异常处理

所有接口都包含异常处理，当发生错误时返回：

```json
{
  "code": 500,
  "msg": "error",
  "data": null
}
```

详细的错误信息会记录在日志文件中，便于问题排查。

## 常见问题

### 1. 无法连接到 Fabric 网络

检查以下配置：
- Fabric peer 节点是否正常运行
- 网络地址和端口是否正确
- 证书文件是否正确配置
- 网络连接是否正常

### 2. 日志文件过大

日志文件会自动按天滚动和压缩，如需调整：
- 修改 `logback-spring.xml` 中的 `maxFileSize` 参数
- 修改 `maxHistory` 参数调整保留天数

### 3. 接口返回 500 错误

查看日志文件 `logs/book-chain-gateway-error.log` 获取详细的错误信息。

## 许可证

本项目采用 MIT 许可证。

## 联系方式

如有问题或建议，请通过以下方式联系：
- GitHub Issues: https://github.com/2011102394/book-chain-gateway/issues

## 更新日志

### v0.0.1 (2026-02-26)
- 初始版本发布
- 实现图书上链、查询、更新、删除和历史轨迹查询功能
- 集成 SLF4J 日志框架
- 配置日志按天滚动和压缩
- 实现统一的 API 响应格式