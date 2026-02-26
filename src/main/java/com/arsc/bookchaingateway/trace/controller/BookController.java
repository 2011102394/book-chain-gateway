package com.arsc.bookchaingateway.trace.controller;

import com.arsc.bookchaingateway.trace.dto.ApiResponse;
import com.arsc.bookchaingateway.trace.dto.BookDTO;
import com.arsc.bookchaingateway.trace.service.FabricGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private FabricGatewayService fabricGatewayService;

    /**
     * 1. 初始上链 (Create)
     */
    @PostMapping
    public ApiResponse<String> createBook(@RequestBody BookDTO bookDTO) {
        try {
            String result = fabricGatewayService.createBook(
                    bookDTO.getId(),
                    bookDTO.getName(),
                    bookDTO.getPublisher(),
                    bookDTO.getLocation()
            );
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("图书上链失败: " + e.getMessage());
        }
    }

    /**
     * 2. 查询最新状态 (Read)
     */
    @GetMapping("/{id}")
    public ApiResponse<String> getBook(@PathVariable("id") String id) {
        try {
            String result = fabricGatewayService.queryBook(id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("查询区块链失败: " + e.getMessage());
        }
    }

    /**
     * 3. 流转更新 (Update)
     */
    @PutMapping("/{id}")
    public ApiResponse<String> updateBook(@PathVariable("id") String id,
                                             @RequestBody BookDTO bookDTO) {
        try {
            // 从 DTO 中提取前端想要更新的字段
            String result = fabricGatewayService.updateBookLocation(
                    id,
                    bookDTO.getLocation(),
                    bookDTO.getStatus()
            );
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("更新区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 4. 状态删除 (Delete)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteBook(@PathVariable("id") String id) {
        try {
            String result = fabricGatewayService.deleteBook(id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("删除区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 5. 查询历史轨迹 (Read Sub-resource)
     */
    @GetMapping("/{id}/history")
    public ApiResponse<String> getBookHistory(@PathVariable("id") String id) {
        try {
            String result = fabricGatewayService.getBookHistory(id);
            return ApiResponse.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("查询区块链历史数据失败: " + e.getMessage());
        }
    }
}