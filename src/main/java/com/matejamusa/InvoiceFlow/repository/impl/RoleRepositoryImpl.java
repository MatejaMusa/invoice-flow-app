package com.matejamusa.InvoiceFlow.repository.impl;

import com.matejamusa.InvoiceFlow.exception.ApiException;
import com.matejamusa.InvoiceFlow.model.Role;
import com.matejamusa.InvoiceFlow.repository.RoleRepository;
import com.matejamusa.InvoiceFlow.rowmapper.RoleRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;


import static com.matejamusa.InvoiceFlow.enumeration.RoleType.ROLE_USER;
import static com.matejamusa.InvoiceFlow.query.RoleQuery.*;
import static java.util.Objects.requireNonNull;


@Repository
@RequiredArgsConstructor
@Slf4j
public class RoleRepositoryImpl implements RoleRepository<Role> {
    private final NamedParameterJdbcTemplate jdbc;
    @Override
    public Role create(Role data) {
        return null;
    }

    @Override
    public Collection<Role> list(int page, int pageSize) {
        return null;
    }

    @Override
    public Role get(Long id) {
        return null;
    }

    @Override
    public Role update(Role data) {
        return null;
    }

    @Override
    public Boolean delete(Long id) {
        return null;
    }

    @Override
    public void addRoleToUser(Long userId, String roleName) {
        log.info("Adding role {} to user id: {}", roleName, userId);
        try {
            Role role = jdbc.queryForObject(SELECT_ROLE_BY_NAME_QUERY, Map.of("name",roleName), new RoleRowMapper());
            jdbc.update(INSERT_ROLE_TO_USER_QUERY, Map.of("userId",userId,"roleId",requireNonNull(role).getId()));
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException("No role found by name: "+ ROLE_USER.name());
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Role getRoleByUserId(Long userId) {
        log.info("Fetching role for user id: {}", userId);
        try {
            System.out.println("in getRoleByUserId method");
            Role role = jdbc.queryForObject(SELECT_ROLE_BY_ID_QUERY, Map.of("id",userId), new RoleRowMapper());
            System.out.println(role);
            return role;
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException("No role found by name: "+ ROLE_USER.name());
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("An error occurred. Please try again.");
        }
    }

    @Override
    public Role getRoleByUserEmail(String email) {
        return null;
    }

    @Override
    public void updateUserRole(Long userId, String roleName) {

    }
}
