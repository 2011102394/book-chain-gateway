# AGENTS.md — Book Chain Gateway

本项目是图书区块链溯源系统的网关服务，基于 Spring Boot 3.5 + Java 17 + Hyperledger Fabric Gateway 构建。
包根路径：`com.arsc.bookchaingateway`，业务代码位于 `trace` 子包下。

## 构建与测试命令

| 命令 | 说明 |
|------|------|
| `mvn clean install` | 全量编译 + 打包（跳过测试用 `-DskipTests`） |
| `mvn spring-boot:run` | 启动应用 |
| `./mvnw spring-boot:run` | 使用 Maven Wrapper 启动 |
| `mvn test` | 运行全部测试 |
| `mvn test -pl . -Dtest=BookChainGatewayApplicationTests#contextLoads` | 运行单个测试方法 |
| `mvn test -pl . -Dtest=BookChainGatewayApplicationTests` | 运行单个测试类 |
| `mvn compile` | 仅编译，不运行测试 |

本项目**没有**独立的 lint 或格式化插件（无 Checkstyle/SpotBugs/Prettier），代码风格以现有代码为准。

## 项目结构

```
src/main/java/com/arsc/bookchaingateway/
├── BookChainGatewayApplication.java          # 启动入口
└── trace/
    ├── config/FabricProperties.java          # @ConfigurationProperties 配置类
    ├── controller/BookController.java        # REST 控制器
    ├── dto/
    │   ├── ApiResponse.java                  # 统一响应封装
    │   └── BookDTO.java                      # 数据传输对象
    └── service/FabricGatewayService.java     # 区块链交互服务
```

## 代码风格指南

### 导入排序
- 导入按以下顺序分组：`com.arsc` → `com.fasterxml` → `io` → `jakarta` → `org` → `java`
- 每个分组内部按字母序排列，各组之间用一个空行分隔
- 禁止使用通配符导入（`.*`）

### 格式化
- 缩进：4 个空格（不使用 Tab）
- 左大括号不换行，右大括号独占一行（K&R 风格）
- 行宽无硬性限制，但保持可读性（建议不超过 120 字符）
- 方法之间用一个空行分隔

### 类型与命名
- **类名**：PascalCase（`BookController`、`FabricGatewayService`）
- **方法名 / 变量名**：camelCase（`createBook`、`orgId`、`bookDTO`）
- **常量**：UPPER_SNAKE_CASE（`MSP_ID`、`CHANNEL_NAME`）
- **包名**：全小写（`trace.controller`、`trace.dto`）
- DTO 字段使用 camelCase，通过 `@JsonProperty` 映射 JSON 键名（如 `id` → `bookId`）

### Lombok
- 项目依赖了 Lombok，但在现有 DTO 类（`BookDTO`、`ApiResponse`、`FabricProperties`）中**未使用** Lombok 注解
- 保持一致：新增 DTO/配置类时**手动编写 getter/setter**，不使用 `@Data` / `@Getter` / `@Setter`
- 如需在 Service/Controller 中减少样板代码，可使用 `@Slf4j` 或 `@RequiredArgsConstructor`，但需与团队确认

### 依赖注入
- Service 层使用**构造器注入**（见 `FabricGatewayService`），不用 `@Autowired` 字段注入
- Controller 层目前使用 `@Autowired` 字段注入，新增 Controller 建议改用构造器注入
- 不使用 `@Resource` 或 JSR-330 注解

### API 设计规范
- 所有 REST 接口返回统一的 `ApiResponse<T>` 包装格式：`{ code, msg, data }`
  - 成功：`code=200, msg="success"`
  - 失败：`code=500, msg="error"`
- Controller 方法返回类型统一使用 `ApiResponse<Object>`
- 使用 Springdoc OpenAPI（`@Tag`、`@Operation`、`@Parameter`、`@Schema`）注解标注接口文档
- API 路径前缀：`/api/books`

### 错误处理
- Controller 层使用 try-catch 包裹 Service 调用，捕获 `Exception` 后返回 `ApiResponse.error(描述)`
- 错误信息记录到日志（`logger.error`），不将异常堆栈暴露给前端
- Service 层方法签名直接 `throws Exception`，由 Controller 层统一处理

### 日志规范
- 使用 SLF4J（`LoggerFactory.getLogger(类名.class)`）
- 日志级别：DEBUG 用于入参/出参，INFO 用于关键操作成功，ERROR 用于异常
- 日志消息格式包含上下文信息：`[orgId] 操作描述: key=value`
- 配置文件：`src/main/resources/logback-spring.xml`，日志输出到 `logs/` 目录

### 配置管理
- 使用 `@ConfigurationProperties(prefix = "fabric")` 绑定配置，不使用 `@Value`
- 多组织配置通过 `Map<String, OrgConfig>` 管理，key 为小写 org 名（`org1`、`org2`、`org3`）
- 应用配置文件：`src/main/resources/application.yml`
- 区块链证书文件：`src/main/resources/network/{orgN}/` 目录下

### 区块链交互
- Service 层通过 `Contract.submitTransaction()` 提交写操作，`Contract.evaluateTransaction()` 执行查询
- 机构路由：通过 `orgId` 参数（大写，如 `ORG1`）从 `contractMap` 获取对应组织的 Contract
- 参数传递给智能合约时，复杂对象先序列化为 JSON 字符串（如批量操作）
- 字节结果通过 `new String(result, StandardCharsets.UTF_8)` 转为字符串

### 测试
- 测试框架：JUnit 5（`@Test`）、Spring Boot Test（`@SpringBootTest`）
- 测试类放在 `src/test/java` 对应包路径下
- 测试类命名：`XxxTests`（如 `BookChainGatewayApplicationTests`）
- 现有测试为上下文加载测试，新增业务测试应 mock Fabric 连接

### Swagger 文档
- 启动后访问 `http://localhost:8080/swagger-ui.html` 查看 API 文档
- 使用 `@Schema` 注解标注 DTO 字段描述和示例值
- 使用 `@Operation` 注解标注接口摘要和说明

## 注意事项
- Java 版本要求：**JDK 17+**
- 修改智能合约调用参数顺序时，必须同步修改链码侧参数，保持一致
- 证书文件（`*.pem`、`*.crt`）为敏感文件，不得提交到公开仓库或泄露
- 修改 `application.yml` 中的组织配置时，确保对应的证书文件存在于 `resources/network/` 目录
