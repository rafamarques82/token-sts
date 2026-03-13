package com.ibm.wsspi.security.tai;

/**
 * Stub interface for compilation - actual implementation provided by Liberty runtime
 */
public class TAIResult {
    private int status;
    private String principal;
    
    private TAIResult(int status, String principal) {
        this.status = status;
        this.principal = principal;
    }
    
    private TAIResult(int status) {
        this.status = status;
        this.principal = null;
    }
    
    public static TAIResult create(int status, String principal) {
        return new TAIResult(status, principal);
    }
    
    public static TAIResult create(int status) {
        return new TAIResult(status);
    }
    
    public int getStatus() {
        return status;
    }
    
    public String getPrincipal() {
        return principal;
    }
}

// Made with Bob
