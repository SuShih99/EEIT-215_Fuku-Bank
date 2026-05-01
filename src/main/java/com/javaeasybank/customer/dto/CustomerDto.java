package com.javaeasybank.customer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class CustomerDto {

    @Getter
    @Setter
    public static class CustomerRequest {
        private String customerId;
        private String idNumber;
        private String name;
        private LocalDate birthday;
        private String gender;
        private String email;
        private String phone;
        private String address;
    }

    @Getter
    @Setter
    public static class CustomerResponse {
        private String customerId;
        private String cif;
        private String idNumber;
        private String name;
        private LocalDate birthday;
        private String gender;
        private String email;
        private String phone;
        private String address;
        private String status;
    }
}