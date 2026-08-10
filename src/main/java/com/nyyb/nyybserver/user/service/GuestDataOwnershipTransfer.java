package com.nyyb.nyybserver.user.service;

public interface GuestDataOwnershipTransfer {
    void transfer(Long guestUserId, Long targetUserId);
}
