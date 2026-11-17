package com.markethub.identity.api;
import java.time.Instant;
public record AdminCustomerResponse(Long id,String name,String email,String status,Instant createdAt){}
