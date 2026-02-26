package com.arsc.bookchaingateway.trace.controller;

import com.arsc.bookchaingateway.trace.dto.ApiResponse;
import com.arsc.bookchaingateway.trace.dto.BookDTO;
import com.arsc.bookchaingateway.trace.service.FabricGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FabricGatewayService fabricGatewayService;

    /**
     * 1. 初始上链 (Create)
     * 传参方式：在 Body JSON 中传入 orgId，例如 "orgId": "ORG1"
     */
    @PostMapping
    public ApiResponse<Object> createBook(@RequestBody BookDTO bookDTO) {
        String orgId = bookDTO.getOrgId() != null ? bookDTO.getOrgId() : "ORG1";
        logger.debug("[{}] 收到图书上链请求: id={}, name={}, publisher={}, location={}",
                orgId, bookDTO.getId(), bookDTO.getName(), bookDTO.getPublisher(), bookDTO.getLocation());
        try {
            String resultStr = fabricGatewayService.createBook(
                    orgId,
                    bookDTO.getId(),
                    bookDTO.getName(),
                    bookDTO.getPublisher(),
                    bookDTO.getLocation(),
                    bookDTO.getOperator(),
                    bookDTO.getOperatorRole()
            );
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书上链成功: id={}", orgId, bookDTO.getId());
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书上链失败: id={}, error={}", orgId, bookDTO.getId(), e.getMessage(), e);
            return ApiResponse.error("图书上链失败: " + e.getMessage());
        }
    }

    /**
     * 2. 查询最新状态 (Read)
     * 传参方式：/api/books/ISBN-001?orgId=ORG2
     */
    @GetMapping("/{id}")
    public ApiResponse<Object> getBook(
            @PathVariable("id") String id,
            @RequestParam(value = "orgId", defaultValue = "ORG1") String orgId) {
        logger.debug("[{}] 收到图书查询请求: id={}", orgId, id);
        try {
            String resultStr = fabricGatewayService.queryBook(orgId, id);
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书查询成功: id={}", orgId, id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书查询失败: id={}, error={}", orgId, id, e.getMessage(), e);
            return ApiResponse.error("查询区块链失败: " + e.getMessage());
        }
    }

    /**
     * 3. 流转更新 (Update)
     * 传参方式：在 Body JSON 中传入 orgId，例如 "orgId": "ORG2"
     */
    @PutMapping("/{id}")
    public ApiResponse<Object> updateBook(@PathVariable("id") String id,
                                          @RequestBody BookDTO bookDTO) {
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
                    bookDTO.getOperatorRole()
            );
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书更新成功: id={}", orgId, id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书更新失败: id={}, error={}", orgId, id, e.getMessage(), e);
            return ApiResponse.error("更新区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 4. 状态删除 (Delete)
     * 传参方式：/api/books/ISBN-001?orgId=ORG1
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Object> deleteBook(
            @PathVariable("id") String id,
            @RequestParam(value = "orgId", defaultValue = "ORG1") String orgId) {
        logger.debug("[{}] 收到图书删除请求: id={}", orgId, id);
        try {
            String resultStr = fabricGatewayService.deleteBook(orgId, id);
            logger.info("[{}] 图书删除成功: id={}", orgId, id);
            return ApiResponse.success(resultStr); // 删除只返回字符串
        } catch (Exception e) {
            logger.error("[{}] 图书删除失败: id={}, error={}", orgId, id, e.getMessage(), e);
            return ApiResponse.error("删除区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 5. 查询历史轨迹 (Read Sub-resource)
     * 传参方式：/api/books/ISBN-001/history?orgId=ORG3
     */
    @GetMapping("/{id}/history")
    public ApiResponse<Object> getBookHistory(
            @PathVariable("id") String id,
            @RequestParam(value = "orgId", defaultValue = "ORG1") String orgId) {
        logger.debug("[{}] 收到图书历史查询请求: id={}", orgId, id);
        try {
            String resultStr = fabricGatewayService.getBookHistory(orgId, id);
            Object result = objectMapper.readValue(resultStr, Object.class);
            logger.info("[{}] 图书历史查询成功: id={}", orgId, id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("[{}] 图书历史查询失败: id={}, error={}", orgId, id, e.getMessage(), e);
            return ApiResponse.error("查询区块链历史数据失败: " + e.getMessage());
        }
    }
}