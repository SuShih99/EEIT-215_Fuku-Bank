package com.javaeasybank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class JavaEasyBankApplication {

    public static void main(String[] args) {
        // 1. 在 Spring 啟動前先重置資料庫
//        restoreDatabase();

        // 2. 正式啟動 Spring
        SpringApplication.run(JavaEasyBankApplication.class, args);
    }

//    private static void restoreDatabase() {
//        Properties prop = new Properties();
//        String propFileName = "application-local.properties";
//
//        try (InputStream inputStream = JavaEasyBankApplication.class.getClassLoader().getResourceAsStream(propFileName)) {
//            if (inputStream == null) {
//                System.out.println("找不到配置文件: " + propFileName + "，跳過重置步驟。");
//                return;
//            }
//            // 載入設定檔
//            prop.load(inputStream);
//
//            String url = prop.getProperty("spring.datasource.url");
//            String user = prop.getProperty("spring.datasource.username");
//            String pass = prop.getProperty("spring.datasource.password");
//
//            // 解析資料庫名稱 (例如: java_easy_bank)
//            String dbName = extractDatabaseName(url);
//            // 產生連向系統 master 資料庫的 URL
//            String masterUrl = url.replace("databaseName=" + dbName, "databaseName=master");
//
//            System.out.println("正在連線至 master 準備重置資料庫 [" + dbName + "]...");
//
//            // 使用原生 JDBC 連線
//            try (Connection conn = DriverManager.getConnection(masterUrl, user, pass);
//                 Statement stmt = conn.createStatement()) {
//
//                // MSSQL 重置語法
//                String sql =
//                    "IF EXISTS (SELECT name FROM sys.databases WHERE name = N'" + dbName + "') " +
//                    "BEGIN " +
//                    "   ALTER DATABASE [" + dbName + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; " +
//                    "   DROP DATABASE [" + dbName + "]; " +
//                    "END " +
//                    "CREATE DATABASE [" + dbName + "];";
//
//                stmt.executeUpdate(sql);
//                System.out.println("資料庫 [" + dbName + "] 重置成功。");
//
//            }
//        } catch (Exception e) {
//            System.err.println("重置資料庫時發生錯誤: " + e.getMessage());
//            // 這裡可以選擇是否要中斷程式執行
//            // throw new RuntimeException(e);
//        }
//    }

//    private static String extractDatabaseName(String url) {
//        Pattern pattern = Pattern.compile("databaseName=([^;]+)");
//        Matcher matcher = pattern.matcher(url);
//        return matcher.find() ? matcher.group(1) : null;
//    }
}