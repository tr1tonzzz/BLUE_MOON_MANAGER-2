package vn.bluemoon.model.dto;

/**
 * DTO for login request
 */
public class LoginRequest {
    private String username;
    private String password;
    private String facebookId; // For Facebook login

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public boolean isFacebookLogin() {
        return facebookId != null && !facebookId.isEmpty();
    }
}

















