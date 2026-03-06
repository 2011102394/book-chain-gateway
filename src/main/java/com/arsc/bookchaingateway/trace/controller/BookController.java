package com.arsc.bookchaingateway.trace.controller;

import com.arsc.bookchaingateway.trace.dto.ApiResponse;
import com.arsc.bookchaingateway.trace.dto.BookDTO;
import com.arsc.bookchaingateway.trace.service.FabricGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 图书溯源管理控制器
 */
@Tag(name = "图书溯源管理", description = "提供基于区块链的图书全生命周期管理接口 (上链、查询、流转、溯源)")
@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FabricGatewayService fabricGatewayService;

    /**
     * 1. 初始上链 (Create)
     */
    @Operation(summary = "单本图书上链", description = "将一本新书的元数据录入区块链账本。需要指定操作机构身份 (orgId)。")
    @PostMapping
    public ApiResponse<Object> createBook(
            @Parameter(description = "图书信息DTO", required = true) @RequestBody BookDTO bookDTO) {

        String orgId = bookDTO.getOrgId() != null ? bookDTO.getOrgId() : "ORG1";
        logger.debug("[{}] 收到图书上链请求: id={}, name={}", orgId, bookDTO.getId(), bookDTO.getName());
        try {
            String resultStr = fabricGatewayService.createBook(
                    orgId,
                    bookDTO.getId(),
                    bookDTO.getName(),
                    bookDTO.getIsbn(),
                    bookDTO.getAuthor(),
                    bookDTO.getPublishDate(),
                    bookDTO.getPublisher(),
                    bookDTO.getLocation(),
                    bookDTO.getOperator(),
                    bookDTO.getOperatorRole());
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书上链成功: id={}", orgId, bookDTO.getId());
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书上链失败: id={}, error={}", orgId, bookDTO.getId(), e.getMessage());
            return ApiResponse.error("图书上链失败: " + e.getMessage());
        }
    }

    /**
     * 6. 批量上链 (Batch Create)
     */
    @Operation(summary = "批量图书上链", description = "原子性地将一批图书录入区块链。如果其中任何一本ID重复，整批操作将失败。")
    @PostMapping("/batch")
    public ApiResponse<Object> batchCreateBooks(
            @Parameter(description = "图书列表DTO", required = true) @RequestBody List<BookDTO> bookList) {

        if (bookList == null || bookList.isEmpty()) {
            return ApiResponse.error("批量数据不能为空");
        }
        String orgId = bookList.get(0).getOrgId() != null ? bookList.get(0).getOrgId() : "ORG1";

        logger.info("[{}] 收到批量上链请求，数量: {}", orgId, bookList.size());

        try {
            String resultStr = fabricGatewayService.batchCreateBooks(orgId, bookList);
            return ApiResponse.success(resultStr);
        } catch (Exception e) {
            logger.error("[{}] 批量上链失败: {}", orgId, e.getMessage());
            return ApiResponse.error("批量上链失败: " + e.getMessage());
        }
    }

    /**
     * 2. 查询最新状态 (Read)
     */
    @Operation(summary = "查询图书详情", description = "根据图书ID查询当前最新的账本状态。")
    @GetMapping("/{id}")
    public ApiResponse<Object> getBook(
            @Parameter(description = "图书唯一ID", example = "ISBN-001") @PathVariable("id") String id,

            @Parameter(description = "查询发起方机构ID", example = "ORG1") @RequestParam(value = "orgId", defaultValue = "ORG1") String orgId) {

        logger.debug("[{}] 收到图书查询请求: id={}", orgId, id);
        try {
            String resultStr = fabricGatewayService.queryBook(orgId, id);
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书查询成功: id={}", orgId, id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书查询失败: id={}, error={}", orgId, id, e.getMessage());
            return ApiResponse.error("查询区块链失败: " + e.getMessage());
        }
    }

    /**
     * 3. 流转更新 (Update)
     */
    @Operation(summary = "更新图书流转状态", description = "更新图书的当前位置、状态及操作人信息，形成流转记录。")
    @PutMapping("/{id}")
    public ApiResponse<Object> updateBook(
            @Parameter(description = "图书唯一ID", example = "ISBN-001") @PathVariable("id") String id,

            @Parameter(description = "包含位置和状态更新信息的DTO", required = true) @RequestBody BookDTO bookDTO) {

        String orgId = bookDTO.getOrgId() != null ? bookDTO.getOrgId() : "ORG1";
        logger.debug("[{}] 收到图书更新请求: id={}, location={}, status={}",
                orgId, id, bookDTO.getLocation(), bookDTO.getStatus());
        try {
            String resultStr = fabricGatewayService.updateBookLocation(
                    orgId,
                    id,
                    bookDTO.getLocation(),
                    bookDTO.getStatus(),
                    bookDTO.getOperator(),
                    bookDTO.getOperatorRole());
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书更新成功: id={}", orgId, id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书更新失败: id={}, error={}", orgId, id, e.getMessage());
            return ApiResponse.error("更新区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 7. 批量流转更新 (Batch Update)
     */
    @Operation(summary = "批量更新图书流转状态", description = "原子性地更新多本图书的当前位置、状态及操作人信息，形成批量流转记录。只有当整批包含的所有的图书全都有效时才会成功。")
    @PutMapping("/batch")
    public ApiResponse<Object> batchUpdateBooks(
            @Parameter(description = "包含更新信息的图书列表DTO（列表中的元素需要指定 bookId 以及要更新的信息）", required = true) @RequestBody List<BookDTO> bookList) {

        if (bookList == null || bookList.isEmpty()) {
            return ApiResponse.error("批量更新数据不能为空");
        }
        String orgId = bookList.get(0).getOrgId() != null ? bookList.get(0).getOrgId() : "ORG1";

        logger.info("[{}] 收到批量流转更新请求，数量: {}", orgId, bookList.size());

        try {
            String resultStr = fabricGatewayService.batchUpdateBookLocation(orgId, bookList);
            return ApiResponse.success(resultStr);
        } catch (Exception e) {
            logger.error("[{}] 批量流转更新失败: {}", orgId, e.getMessage());
            return ApiResponse.error("批量更流转新失败: " + e.getMessage());
        }
    }

    /**
     * 4. 状态删除 (Delete)
     */
    @Operation(summary = "删除图书状态", description = "从世界状态中删除图书（标记删除），但历史溯源记录依然保留。")
    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteBook(
            @Parameter(description = "图书唯一ID", example = "ISBN-001") @PathVariable("id") String id,

            @Parameter(description = "删除发起方机构ID", example = "ORG1") @RequestParam(value = "orgId", defaultValue = "ORG1") String orgId) {

        logger.debug("[{}] 收到图书删除请求: id={}", orgId, id);
        try {
            String resultStr = fabricGatewayService.deleteBook(orgId, id);
            logger.info("[{}] 图书删除成功: id={}", orgId, id);
            return ApiResponse.success(resultStr);
        } catch (Exception e) {
            logger.error("[{}] 图书删除失败: id={}, error={}", orgId, id, e.getMessage());
            return ApiResponse.error("删除区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 5. 查询历史轨迹
     */
    @Operation(summary = "查询历史溯源轨迹", description = "获取该图书从创建至今的所有流转历史记录（包含时间戳、交易ID）。")
    @GetMapping("/{id}/history")
    public ApiResponse<Object> getBookHistory(
            @Parameter(description = "图书唯一ID", example = "ISBN-001") @PathVariable("id") String id,

            @Parameter(description = "查询发起方机构ID", example = "ORG1") @RequestParam(value = "orgId", defaultValue = "ORG1") String orgId) {

        logger.debug("[{}] 收到图书历史查询请求: id={}", orgId, id);
        try {
            String resultStr = fabricGatewayService.getBookHistory(orgId, id);
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书历史查询成功: id={}", orgId, id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书历史查询失败: id={}, error={}", orgId, id, e.getMessage());
            return ApiResponse.error("查询区块链历史数据失败: " + e.getMessage());
        }
    }
}