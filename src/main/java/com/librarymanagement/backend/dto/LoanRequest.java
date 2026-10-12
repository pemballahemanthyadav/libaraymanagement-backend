package com.librarymanagement.backend.dto;

import java.time.LocalDateTime;

public class LoanRequest {

    private Long inventoryId; // specific copy to borrow
    private Long memberId;    // used by admins to borrow on behalf of a member

    public LoanRequest() {
    }

    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
}
