package com.fashion.event.config;

import com.fashion.event.entity.CampaignEvent;
import com.fashion.event.entity.CampaignEventStatus;
import com.fashion.event.repository.CampaignEventRepository;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@Order(3)
public class CampaignEventInitializer implements CommandLineRunner {
    private static final int EVENT_YEAR = 2027;
    private static final String WOMEN_DAY_EVENT = "Ngày Phụ nữ Việt Nam 20-10";
    private static final String FALL_EVENT = "Thu vàng - thanh lịch mỗi ngày";
    private final CampaignEventRepository eventRepository;
    private final ZoneId eventZone;

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now(eventZone);
        ensureWomenDayEvent(now);
        ensureSeasonalEvents();
    }

    private void ensureWomenDayEvent(LocalDateTime now) {
        LocalDateTime start = LocalDateTime.of(2026, Month.OCTOBER, 20, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, Month.OCTOBER, 21, 0, 0);
        CampaignEventStatus status = now.isBefore(end) ? CampaignEventStatus.ACTIVE : CampaignEventStatus.ENDED;
        String image = "https://images.unsplash.com/photo-1681566865827-4e5989266573?auto=format&fit=crop&w=1800&q=85";
        migrateSeedEvent(WOMEN_DAY_EVENT, "Ngay Phu nu Viet Nam 20-10",
                "https://images.unsplash.com/photo-1529139574466-a303027c1d8b?auto=format&fit=crop&w=1800&q=85", image);
        createIfAbsent(WOMEN_DAY_EVENT, "Tôn vinh nàng", "20 tháng 10 · Dành tặng những điều thật đẹp.",
                image, start, end, null, null, "products", status, 100);
    }

    private void ensureSeasonalEvents() {
        LocalDateTime winterStart = LocalDateTime.of(EVENT_YEAR, Month.DECEMBER, 1, 0, 0);
        LocalDateTime winterEnd = LocalDateTime.of(EVENT_YEAR, Month.DECEMBER, 31, 23, 59);
        LocalDateTime springStart = LocalDateTime.of(EVENT_YEAR, Month.MARCH, 1, 0, 0);
        LocalDateTime springEnd = LocalDateTime.of(EVENT_YEAR, Month.MAY, 31, 23, 59);
        LocalDateTime summerStart = LocalDateTime.of(EVENT_YEAR, Month.JUNE, 1, 0, 0);
        LocalDateTime summerEnd = LocalDateTime.of(EVENT_YEAR, Month.AUGUST, 31, 23, 59);
        LocalDateTime fallStart = LocalDateTime.of(EVENT_YEAR, Month.SEPTEMBER, 1, 0, 0);
        LocalDateTime fallEnd = LocalDateTime.of(EVENT_YEAR, Month.NOVEMBER, 30, 23, 59);

        String springImage = "https://images.unsplash.com/photo-1674401737764-b06c16041f0b?auto=format&fit=crop&w=1800&q=85";
        String summerImage = "https://images.unsplash.com/photo-1723649563005-1932a682864e?auto=format&fit=crop&w=1800&q=85";
        String fallImage = "https://afamilycdn.com/2019/10/27/va07020-1572169333440.jpg";
        String winterImage = "https://kenh14cdn.com/203336854389633024/2025/11/4/4485123614258375137467602706146881496888182n-1762169888746579138417-1762226349133-1762226349770256563166.jpg";
        migrateSeedEvent("Xuân rạng rỡ", "Xuan rang ro",
                "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=1800&q=85", springImage);
        migrateSeedEvent("Hè rực nắng", "He ruc nang",
                "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=1800&q=85", summerImage);
        migrateSeedEvent(FALL_EVENT, "Thu vang - thanh lich moi ngay",
                "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1800&q=85", fallImage);
        migrateSeedEvent("Đông ấm áp", "Dong am ap",
                "https://images.unsplash.com/photo-1548126032-079a0fb0099d?auto=format&fit=crop&w=1800&q=85", winterImage);

        createIfAbsent("Xuân rạng rỡ", "Chạm sắc xuân mới", "Sắc màu tươi mới cho những ngày đầy cảm hứng.",
                springImage, springStart, springEnd, null, null, "products", CampaignEventStatus.SCHEDULED, 40);
        createIfAbsent("Hè rực nắng", "Phong cách ngày nắng", "Nhẹ nhàng, thoáng mát và sẵn sàng cho mọi chuyến đi.",
                summerImage, summerStart, summerEnd, null, null, "products", CampaignEventStatus.SCHEDULED, 40);
        createIfAbsent(FALL_EVENT, "Thu vàng thanh lịch", "Làm mới tủ đồ với những thiết kế dành cho ngày thu.",
                fallImage, fallStart, fallEnd, null, null, "products", CampaignEventStatus.SCHEDULED, 40);
        createIfAbsent("Đông ấm áp", "Ấm áp theo cách riêng", "Hoàn thiện phong cách mùa lạnh với những lớp áo tinh tế.",
                winterImage, winterStart, winterEnd, null, null, "products", CampaignEventStatus.SCHEDULED, 40);
    }

    private void migrateSeedEvent(String eventName, String legacyName, String previousSeedImage, String vietnameseImage) {
        CampaignEvent event = eventRepository.findFirstByEventNameIgnoreCase(legacyName).orElse(null);
        if (event == null) return;
        event.setEventName(eventName);
        if (previousSeedImage.equals(event.getDesktopImageUrl())) event.setDesktopImageUrl(vietnameseImage);
        eventRepository.save(event);
    }

    private void createIfAbsent(String name, String title, String message, String desktopImage,
            LocalDateTime start, LocalDateTime end, String offer, String voucherCode,
            String targetUrl, CampaignEventStatus requestedStatus, int priority) {
        if (eventRepository.findFirstByEventNameIgnoreCase(name).isPresent()) return;
        CampaignEvent event = new CampaignEvent();
        CampaignEventStatus status = requestedStatus;
        if (requestedStatus == CampaignEventStatus.SCHEDULED && LocalDateTime.now(eventZone).isAfter(end)) {
            status = CampaignEventStatus.ENDED;
        }
        event.setEventName(name);
        event.setTitle(title);
        event.setMessage(message);
        event.setDesktopImageUrl(desktopImage);
        event.setStartAt(start);
        event.setEndAt(end);
        event.setOfferLabel(offer);
        event.setVoucherCode(voucherCode);
        event.setTargetUrl(targetUrl);
        event.setPriority(priority);
        event.setStatus(status);
        eventRepository.save(event);
    }

    public CampaignEventInitializer(CampaignEventRepository eventRepository,
            @Value("${fashion.events.zone-id:Asia/Ho_Chi_Minh}") String eventZone) {
        this.eventRepository = eventRepository;
        this.eventZone = ZoneId.of(eventZone);
    }
}
