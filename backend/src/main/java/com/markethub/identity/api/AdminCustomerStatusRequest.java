package com.markethub.identity.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record AdminCustomerStatusRequest(@NotBlank @Pattern(regexp="ACTIVE|SUSPENDED") String status){}
