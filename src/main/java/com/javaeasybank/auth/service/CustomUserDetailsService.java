package com.javaeasybank.auth.service;

import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.entity.AuthRole;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.javaeasybank.auth.repository.AuthRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security 的橋樑：
 * 登入時從 auth_emp 查員工 → 從 auth_role 查角色 → 轉成 Spring Security 認識的 UserDetails
 * 
 * 設定好之後，組員只需要在 Controller 上加 @PreAuthorize("hasRole('CISO')") 就能控制權限。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthEmpRepository authEmpRepository;
    private final AuthRoleRepository authRoleRepository;

    public CustomUserDetailsService(AuthEmpRepository authEmpRepository,
                                    AuthRoleRepository authRoleRepository) {
        this.authEmpRepository = authEmpRepository;
        this.authRoleRepository = authRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 用 email 查員工
        AuthEmp emp = authEmpRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("帳號不存在：" + email));

        // 2. 只有 ACTIVE 狀態的員工能登入
        if (!"ACTIVE".equals(emp.getStatus())) {
            throw new UsernameNotFoundException("此帳號已被停用或鎖定：" + email);
        }

        // 3. 查角色表，取得 roleCode（如 CISO、CSVO）
        AuthRole role = authRoleRepository.findById(emp.getRoleId())
                .orElseThrow(() -> new UsernameNotFoundException("角色設定異常：" + emp.getRoleId()));

        // 4. 轉成 Spring Security 的 UserDetails
        //    角色格式為 ROLE_CISO、ROLE_CSVO（Spring Security 會自動加 ROLE_ 前綴）
        return User.builder()
                .username(emp.getEmail())
                .password(emp.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode())))
                .build();
    }
}
