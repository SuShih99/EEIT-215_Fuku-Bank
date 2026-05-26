package com.javaeasybank.risk.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.risk.enums.BlacklistType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BlackListRequest {

    private BlacklistType listType;

    private String listValue;

    private String source;

    private String reason;

    private Boolean status = Boolean.TRUE;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}