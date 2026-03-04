package com.arsc.bookchaingateway.trace.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * 图书信息传输对象
 */
@Schema(description = "图书信息数据传输对象 (DTO)，用于与网关交互")
public class BookDTO {

    @Schema(description = "当前操作的机构身份ID (MSP ID)，注意ORG1:出版社  ORG2:物流中心 ORG3:书店", example = "ORG1", defaultValue = "ORG1")
    private String orgId;

    @Schema(description = "图书唯一标识符 (ISBN+流水号)", example = "ISBN-978-7-111-0001", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("bookId") // 确保映射到合约的 bookId
    private String id;

    @Schema(description = "图书名称", example = "深入理解区块链技术")
    private String name;

    @Schema(description = "标准书号 (ISBN)", example = "978-7-111-21382-6")
    private String isbn;

    @Schema(description = "图书作者", example = "张三丰")
    private String author;

    @Schema(description = "出版日期 (格式: yyyy-MM-dd HH:mm:ss)", example = "2023-10-01 10:00:00", type = "string")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishDate;

    @Schema(description = "出版社名称", example = "机械工业出版社")
    private String publisher;

    @Schema(description = "当前物理位置 (仓库/书店/转运中心)", example = "北京朝阳区总仓")
    @JsonProperty("currentLocation") // 确保映射到合约的 currentLocation
    private String location;

    @Schema(description = "当前图书状态", example = "已入库")
    private String status;

    @Schema(description = "业务操作员姓名", example = "李四")
    private String operator;

    @Schema(description = "操作员角色岗位", example = "入库质检员")
    private String operatorRole;

    // ================= Getter 和 Setter 方法 =================

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperatorRole() {
        return operatorRole;
    }

    public void setOperatorRole(String operatorRole) {
        this.operatorRole = operatorRole;
    }
}