package com.javaeasybank.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auth_dept")
@Getter
@Setter
public class AuthDept {

    @Id
    @Column(name = "dept_id", length = 10)
    private String deptId;

    @Column(name = "dept_code", length = 20, nullable = false, unique = true)
    private String deptCode;

    @Column(name = "dept_name", length = 50, nullable = false)
    private String deptName;
}
