package com.example.rbpolab1.security;

import com.example.rbpolab1.model.Account;
import com.example.rbpolab1.model.AccountStatus;
import com.example.rbpolab1.model.Shipment;
import com.example.rbpolab1.repository.AccountRepository;
import com.example.rbpolab1.repository.ShipmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("accessRules")
@Transactional(readOnly = true)
public class AccessRules {

    private final AccountRepository accountRepository;
    private final ShipmentRepository shipmentRepository;

    public AccessRules(AccountRepository accountRepository, ShipmentRepository shipmentRepository) {
        this.accountRepository = accountRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public boolean isDispatcher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DISPATCHER"));
    }

    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Access denied");
        }

        return accountRepository.findByUsername(authentication.getName())
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .map(Account::getUserId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
    }

    public boolean canAccessUser(Long userId) {
        return isDispatcher() || (userId != null && userId.equals(currentUserId()));
    }

    public boolean canReadShipment(Long shipmentId) {
        if (isDispatcher()) {
            return true;
        }
        if (shipmentId == null) {
            return false;
        }

        Long userId = currentUserId();
        return shipmentRepository.findById(shipmentId)
                .map(shipment -> userId.equals(shipment.getSenderId()))
                .orElse(false);
    }

    public boolean canCreateShipment(Shipment request) {
        return isDispatcher() || request == null || request.getSenderId() == null
                || canAccessUser(request.getSenderId());
    }

    public boolean canUpdateShipment(Long shipmentId, Shipment request) {
        return canReadShipment(shipmentId) && canCreateShipment(request);
    }

    public void requireSenderAccess(Long senderId) {
        if (!canAccessUser(senderId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}