package com.fashion.user.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String address;
    private String avatar;

    public UpdateProfileRequest() {
    }
    public UpdateProfileRequest(final String fullName, final String phone, final String address, final String avatar) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.avatar = avatar;
    }
}
