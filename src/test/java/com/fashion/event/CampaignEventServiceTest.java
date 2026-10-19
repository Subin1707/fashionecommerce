package com.fashion.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fashion.event.dto.CampaignEventRequest;
import com.fashion.event.dto.CampaignEventResponse;
import com.fashion.event.entity.CampaignEventStatus;
import com.fashion.event.repository.CampaignEventRepository;
import com.fashion.event.service.CampaignEventService;
import com.fashion.voucher.entity.Voucher;
import com.fashion.voucher.repository.VoucherRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CampaignEventServiceTest {
    @Autowired private CampaignEventService eventService;
    @Autowired private CampaignEventRepository eventRepository;
    @Autowired private VoucherRepository voucherRepository;

    @BeforeEach
    void cleanEvents() {
        eventRepository.deleteAll();
        voucherRepository.deleteAll();
    }

    @Test
    void createsSchedulesEditsAndDeletesCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        CampaignEventResponse scheduled = eventService.create(request("Summer event", CampaignEventStatus.ACTIVE, now.plusDays(1), now.plusDays(5), null, null));
        assertThat(scheduled.status()).isEqualTo(CampaignEventStatus.SCHEDULED);
        assertThat(eventService.getActiveEvents()).isEmpty();

        CampaignEventResponse active = eventService.update(scheduled.id(), request("Summer event", CampaignEventStatus.ACTIVE, now.minusMinutes(1), now.plusDays(1), null, null));
        assertThat(active.status()).isEqualTo(CampaignEventStatus.ACTIVE);
        assertThat(eventService.getActiveEvents()).extracting(CampaignEventResponse::id).containsExactly(scheduled.id());

        CampaignEventResponse ended = eventService.update(scheduled.id(), request("Summer event", CampaignEventStatus.ENDED, now.minusMinutes(1), now.plusDays(1), null, null));
        assertThat(ended.status()).isEqualTo(CampaignEventStatus.ENDED);
        assertThat(eventService.getActiveEvents()).isEmpty();

        eventService.update(scheduled.id(), request("Summer event", CampaignEventStatus.ACTIVE, now.minusMinutes(1), now.plusDays(1), null, null));
        eventService.update(scheduled.id(), request("Summer event", CampaignEventStatus.HIDDEN, now.minusMinutes(1), now.plusDays(1), null, null));
        assertThat(eventService.getActiveEvents()).isEmpty();
        eventService.delete(scheduled.id());
        assertThat(eventRepository.existsById(scheduled.id())).isFalse();
    }

    @Test
    void rejectsPercentOfferWithoutARealVoucher() {
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> eventService.create(request("Sale", CampaignEventStatus.DRAFT, now, now.plusDays(1), "Giảm 20%", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voucher");
    }

    @Test
    void validatesLinkedVoucherAndKeepsItsCodeOnCampaign() {
        LocalDateTime now = LocalDateTime.now();
        Voucher voucher = Voucher.builder().code("EVENT10").discountType("PERCENTAGE").discountValue(10.0)
                .minOrderValue(0.0).maxDiscount(100000.0).startDate(now.minusDays(1))
                .endDate(now.plusDays(3)).status("ACTIVE").build();
        voucherRepository.save(voucher);

        CampaignEventResponse saved = eventService.create(request("Spring event", CampaignEventStatus.ACTIVE, now, now.plusDays(2), "Giảm 10%", "EVENT10"));
        assertThat(saved.voucherCode()).isEqualTo("EVENT10");
        assertThat(saved.status()).isEqualTo(CampaignEventStatus.ACTIVE);
    }

    private CampaignEventRequest request(String name, CampaignEventStatus status, LocalDateTime start, LocalDateTime end,
            String offerLabel, String voucherCode) {
        return new CampaignEventRequest(name, "Thời trang mùa mới", "Khám phá phong cách mới.",
                "https://example.com/banner.jpg", null, start, end, offerLabel, voucherCode,
                "products", status, 10);
    }
}
