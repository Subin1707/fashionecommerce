package com.fashion.event.repository;

import com.fashion.event.entity.CampaignEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignEventRepository extends JpaRepository<CampaignEvent, Long> {
    java.util.List<CampaignEvent> findAllByOrderByPriorityDescStartAtAscIdAsc();
    java.util.Optional<CampaignEvent> findFirstByEventNameIgnoreCase(String eventName);
}
