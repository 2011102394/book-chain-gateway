package com.arsc.bookchaingateway.trace.controller;

import com.arsc.bookchaingateway.trace.dto.BookDTO;
import com.arsc.bookchaingateway.trace.service.FabricGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> createBook(@RequestBody BookDTO bookDTO) {
        try {
            String result = fabricGatewayService.createBook(
                    bookDTO.getId(),
                    bookDTO.getName(),
                    bookDTO.getPublisher(),
                    bookDTO.getLocation()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("图书上链失败: " + e.getMessage());
        }
    }

    /**
     * 2. 查询最新状态 (Read)
     */
    @GetMapping("/{id}")
    public ResponseEntity<String> getBook(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(fabricGatewayService.queryBook(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("查询区块链失败: " + e.getMessage());
        }
    }

    /**
     * 3. 流转更新 (Update)
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> updateBook(@PathVariable("id") String id,
                                             @RequestBody BookDTO bookDTO) {
        try {
            // 从 DTO 中提取前端想要更新的字段
            String result = fabricGatewayService.updateBookLocation(
                    id,
                    bookDTO.getLocation(),
                    bookDTO.getStatus()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("更新区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 4. 状态删除 (Delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(fabricGatewayService.deleteBook(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("删除区块链数据失败: " + e.getMessage());
        }
    }

    /**
     * 5. 查询历史轨迹 (Read Sub-resource)
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<String> getBookHistory(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(fabricGatewayService.getBookHistory(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("查询区块链历史数据失败: " + e.getMessage());
        }
    }
}