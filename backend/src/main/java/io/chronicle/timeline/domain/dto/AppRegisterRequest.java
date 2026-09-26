package io.chronicle.timeline.domain.dto;

public class AppRegisterRequest extends AppLoginRequest {
    private String nickname;
    private String confirmPassword;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
