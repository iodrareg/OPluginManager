package io.ottoscripts.usermanagement;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoredUser {
    private String username;
    private String password;
}
