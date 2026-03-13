package com.ibm.wsspi.security.tai;

import java.util.Properties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;

/**
 * Stub interface for compilation with Jakarta EE 9+ (jakarta.servlet)
 * Actual implementation provided by Liberty runtime in ODM 9.5
 */
public interface TrustAssociationInterceptor {
    
    /**
     * Initialize the interceptor
     */
    int initialize(Properties properties) throws WebTrustAssociationFailedException;
    
    /**
     * Determine if this interceptor should handle the request
     */
    boolean isTargetInterceptor(HttpServletRequest request) throws WebTrustAssociationException;
    
    /**
     * Validate and establish trust
     */
    TAIResult negotiateValidateandEstablishTrust(HttpServletRequest request, HttpServletResponse response) 
        throws WebTrustAssociationFailedException;
    
    /**
     * Get the version of this interceptor
     */
    String getVersion();
    
    /**
     * Get the type/name of this interceptor
     */
    String getType();
    
    /**
     * Cleanup when the interceptor is being destroyed
     */
    void cleanup();
}

// Made with Bob
