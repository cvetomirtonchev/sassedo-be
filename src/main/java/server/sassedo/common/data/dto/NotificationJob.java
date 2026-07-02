package server.sassedo.common.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String body;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private int totalRecipients;
    private int successCount;
    private int failureCount;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
