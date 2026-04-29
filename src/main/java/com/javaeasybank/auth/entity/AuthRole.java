package com.javaeasybank.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auth_role")
@Getter
@Setter
public class AuthRole {

    @Id
    @Column(name = "role_id", length = 10)
    private String roleId;

    @Column(name = "dept_id", length = 10, nullable = false)
    private String deptId;

    @Column(name = "role_code", length = 20, nullable = false, unique = true)
    private String roleCode;

    @Column(name = "role_name", length = 50, nullable = false)
    private String roleName;

    @Column(name = "perm_level", nullable = false)
    private Integer permLevel;

    @Column(name = "perm_scope", length = 10)
    private String permScope;
}
