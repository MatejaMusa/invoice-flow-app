package com.matejamusa.InvoiceFlow.service.impl;

import com.matejamusa.InvoiceFlow.model.Role;
import com.matejamusa.InvoiceFlow.repository.RoleRepository;
import com.matejamusa.InvoiceFlow.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository<Role> roleRepository;
    @Override
    public Role getRoleByUserId(Long id) {
        return roleRepository.getRoleByUserId(id);
    }
}
