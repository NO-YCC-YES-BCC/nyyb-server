package com.nyyb.nyybserver.event.data.repository;

import com.nyyb.nyybserver.event.data.entity.FunnelEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunnelEventRepository extends JpaRepository<FunnelEvent, Long> {
}
