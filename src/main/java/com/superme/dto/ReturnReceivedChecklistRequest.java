package com.superme.dto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReturnReceivedChecklistRequest {

    private Long orderItemId;

    private Boolean isTagIntact;             // "Is the tag intact?"
    private Boolean isItemDamaged;           // "Is the item damaged?"
    private Boolean hygieneCheckPassed;      // etc, as per your exact checklist
    private String warehouseNote;            // free text note

    // outcome buttons: "Add item back to inventory", "Discard item" etc.
    private String disposition;              // e.g. "RESTOCK" or "DISCARD"

    // banner text ("Item received on Dec 12. Checklist passed...")
    private String statusNote;
}