// package com.traymate.backend.admin.assignment.dto;

// public class AssignmentStatsDto {
    
// }

package com.traymate.backend.admin.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignmentStatsDto {
    private long totalResidents;
    private long assigned;
    private long unassigned;
}
