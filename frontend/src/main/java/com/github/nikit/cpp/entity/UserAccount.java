package com.github.nikit.cpp.entity;

import com.github.nikit.cpp.Constants;
import org.hibernate.validator.constraints.NotEmpty;

import javax.persistence.*;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

@Entity
@Table(name = "users", schema = Constants.Schemas.AUTH)
public class UserAccount {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private String username;
    private String password; // hash
    private URL avatar;
    private boolean expired;
    private boolean locked;
    private boolean enabled; // synonym to "confirmed"

    @Enumerated(EnumType.ORDINAL)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="user_roles", schema = Constants.Schemas.AUTH, joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role_id")
    private Collection<UserRole> roles = new HashSet<>(); // synonym to "authority"

    public UserAccount() { }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public URL getAvatar() {
        return avatar;
    }

    public void setAvatar(URL avatar) {
        this.avatar = avatar;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Collection<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(Collection<UserRole> roles) {
        this.roles = roles;
    }
}
