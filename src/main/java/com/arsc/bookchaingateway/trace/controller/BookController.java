package com.arsc.bookchaingateway.trace.controller;

import com.arsc.bookchaingateway.trace.dto.ApiResponse;
import com.arsc.bookchaingateway.trace.dto.BookDTO;
import com.arsc.bookchaingateway.trace.service.FabricGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private FabricGatewayService fabricGatewayService;

    /**
     * 1. 初始上链 (Create)
     */
    @PostMapping
    public ApiResponse<String> createBook(@RequestBody BookDTO bookDTO) {
        logger.debug("收到图书上链请求: id={}, name={}, publisher={}, location={}", 
                bookDTO.getId(), bookDTO.getName(), bookDTO.getPublisher(), bookDTO.getLocation());
        try {
            String result = fabricGatewayService.createBook(
                    bookDTO.getId(),
                    bookDTO.getName(),
                    bookDTO.getPublisher(),
                    bookDTO.getLocation()
            );
            logger.info("图书上链成功: id={}", bookDTO.getId());
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("图书上链失败: id={}, error={}", bookDTO.getId(), e.getMessage(), e);
            return ApiResponse.error("图书上链失败: " + e.getMessage());
        }
    }

    /**
     * 2. 查询最新状态 (Read)
     */
    @GetMapping("/{id}")
    public ApiResponse<String> getBook(@PathVariable("id") String id) {
        logger.debug("收到图书查询请求: id={}", id);
        try {
            String result = fabricGatewayService.queryBook(id);
            logger.info("图书查询成功: id={}", id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("图书查询失败: id={}, error={}", id, e.getMessage(), e);
            return ApiResponse.error("查询区块链失败: " + e.getMessage());
        }
    }

    /**
     * 3. 流转更新 (Update)
     */
    @PutMapping("/{id}")
    public ApiResponse<String> updateBook(@PathVariable("id") String id,
                                             @RequestBody BookDTO bookDTO) {
        logger.debug("收到图书更新请求: id={}, location={}, status={}", 
                id, bookDTO.getLocation(), bookDTO.getStatus());
        try {
            // 从 DTO 中提取前端想要更新的字段
            String result = fabricGatewayService.updateBookLocation(
                    id,
                    bookDTO.getLocation(),
                    bookDTO.getStatus()
            );
            logger.info("图书更新成功: id={}", id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("图书更新失败: id={}, error={}", id, e.getMessage(), e);
            return ApiResponse.error("更新区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 4. 状态删除 (Delete)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteBook(@PathVariable("id") String id) {
        logger.debug("收到图书删除请求: id={}", id);
        try {
            String result = fabricGatewayService.deleteBook(id);
            logger.info("图书删除成功: id={}", id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("图书删除失败: id={}, error={}", id, e.getMessage(), e);
            return ApiResponse.error("删除区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 5. 查询历史轨迹 (Read Sub-resource)
     */
    @GetMapping("/{id}/history")
    public ApiResponse<String> getBookHistory(@PathVariable("id") String id) {
        logger.debug("收到图书历史查询请求: id={}", id);
        try {
            String result = fabricGatewayService.getBookHistory(id);
            logger.info("图书历史查询成功: id={}", id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("图书历史查询失败: id={}, error={}", id, e.getMessage(), e);
            return ApiResponse.error("查询区块链历史数据失败: " + e.getMessage());
        }
    }
}