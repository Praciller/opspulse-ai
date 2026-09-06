package com.opspulse.shared.web;

import com.opspulse.shared.error.ApiException;
import com.opspulse.shared.error.ErrorCode;
import org.springframework.http.HttpStatus;

public record SortQuery(String field, boolean ascending) {

    public static SortQuery parse(String value) {
        String[] parts = value == null ? new String[0] : value.trim().split(",", -1);
        if (parts.length != 2 || parts[0].isBlank()
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST,
                    "sort must use field,asc or field,desc");
        }
        return new SortQuery(parts[0].trim(), parts[1].equalsIgnoreCase("asc"));
    }
}
