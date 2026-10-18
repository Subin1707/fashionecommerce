package com.fashion.user.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateUserRequest {
    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String avatar;
    private String role;
    private String status;

    public CreateUserRequest() {
    }
    public CreateUserRequest(final String fullName, final String email, final String password, final String phone, final String address, final String avatar, final String role, final String status) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.avatar = avatar;
        this.role = role;
        this.status = status;
    }
}