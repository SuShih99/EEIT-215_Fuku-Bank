package com.javaeasybank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 測試啟動才用下面的Exclude 因為還沒設定資料庫 JPA會報錯
//@SpringBootApplication(exclude = {
//        DataSourceAutoConfiguration.class,
//        HibernateJpaAutoConfiguration.class
//})
public class JavaEasyBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaEasyBankApplication.class, args);
    }

}
