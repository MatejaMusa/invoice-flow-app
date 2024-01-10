package com.matejamusa.InvoiceFlow.service;

import com.matejamusa.InvoiceFlow.model.Role;

import java.util.Collection;

public interface RoleService {
    Role getRoleByUserId(Long id);
    Collection<Role> getRoles();
}
