package com.matejamusa.InvoiceFlow.constant;

public class Constants {
    // Security
    public static final String[] PUBLIC_URLS = {"/user/refresh/token/**","/user/verify/account/**",
            "/user/verify/password/**","/user/login/**","/user/register/**",
            "/user/verify/**","/user/resetpassword/**","/user/image/**", "/user/new/password/**"};
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String[] PUBLIC_ROUTES = {"/user/new/password", "user/login","/user/verify/code", "/user/register","/user/refresh/token","/user/image"};

    public static final String INVOICE_FLOW_DOO = "INVOICE_FLOW_DOO";
    public static final String CUSTOMER_MANAGEMENT_SERVICE = "CUSTOMER_MANAGEMENT_SERVICE";
    public static final String AUTHORITIES = "authorities";
    public static final long ACCESS_TOKEN_EXPIRATION_TIME = 1_800_000;
    public static final long REFRESH_TOKEN_EXPIRATION_TIME = 432_000_000;

    // Date
    public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
}
