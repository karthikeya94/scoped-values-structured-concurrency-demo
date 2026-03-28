package com.demo.structured.threadlocal;

import java.util.List;

public class ThreadLocalContext {

    public static final InheritableThreadLocal<String> TRACE_ID = new InheritableThreadLocal<>();
    public static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    public static final InheritableThreadLocal<AuthData> AUTH_DATA = new InheritableThreadLocal<>();
    public static final InheritableThreadLocal<List<String>> EXPERIMENT_FLAGS = new InheritableThreadLocal<>();

    public static class AuthData {
        private String user;
        private String role;
        
        public AuthData(String user, String role) { 
            this.user = user; 
            this.role = role;
        }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
