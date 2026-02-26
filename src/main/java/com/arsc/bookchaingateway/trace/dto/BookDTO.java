package com.arsc.bookchaingateway.trace.dto;

public class BookDTO {

    private String orgId; // 🌟 新增：机构身份标识 (如: ORG1, ORG2, ORG3)

    private String id;
    private String name;
    private String publisher;
    private String location;
    private String status;
    private String operator;
    private String operatorRole;

    // --- Getter 和 Setter ---

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }
}