package com.routex.dto;

import com.routex.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UserAdminResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private boolean enabled;
    private LocalDateTime createdAt;

    public UserAdminResponse(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.createdAt = user.getCreatedAt();
    }
}
