package org.example.app.security;

public class TenantContextHolder {
    private static final ThreadLocal<Long> tenantIdHolder = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) {
        tenantIdHolder.set(tenantId);
    }

    public static Long getTenantId() {
        return tenantIdHolder.get();
    }

    public static void clear() {
        tenantIdHolder.remove();
    }
}
