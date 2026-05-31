package com.app.model;

import javax.persistence.*;

@Entity
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String policyCode;
    private String name;
    private String category;
    private String effectiveDate;
    private String expiryDate;
    private String status;

    public Long getId() { return id; }
    public String getPolicyCode() { return policyCode; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getEffectiveDate() { return effectiveDate; }
    public String getExpiryDate() { return expiryDate; }
    public String getStatus() { return status; }

    public void setPolicyCode(String v) { this.policyCode = v; }
    public void setName(String v) { this.name = v; }
    public void setCategory(String v) { this.category = v; }
    public void setEffectiveDate(String v) { this.effectiveDate = v; }
    public void setExpiryDate(String v) { this.expiryDate = v; }
    public void setStatus(String v) { this.status = v; }
}
