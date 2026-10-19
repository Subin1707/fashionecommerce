package com.fashion.event.service;

import com.fashion.event.dto.CampaignEventRequest;
import com.fashion.event.dto.CampaignEventResponse;
import com.fashion.event.entity.CampaignEvent;
import com.fashion.event.entity.CampaignEventStatus;
import com.fashion.event.repository.CampaignEventRepository;
import com.fashion.voucher.entity.Voucher;
import com.fashion.voucher.repository.VoucherRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignEventService {
    private final CampaignEventRepository eventRepository;
    private final VoucherRepository voucherRepository;
    private final CampaignImageStorageService imageStorage;
    private final ZoneId eventZone;

    @Transactional(readOnly = true)
    public List<CampaignEventResponse> getActiveEvents() {
        LocalDateTime now = now();
        return eventRepository.findAllByOrderByPriorityDescStartAtAscIdAsc().stream()
                .filter(event -> getEffectiveStatus(event, now) == CampaignEventStatus.ACTIVE)
                .map(event -> CampaignEventResponse.from(event, CampaignEventStatus.ACTIVE))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignEventResponse> getAllEvents() {
        LocalDateTime now = now();
        return eventRepository.findAllByOrderByPriorityDescStartAtAscIdAsc().stream()
                .map(event -> CampaignEventResponse.from(event, getEffectiveStatus(event, now)))
                .toList();
    }

    @Transactional
    public CampaignEventResponse create(CampaignEventRequest request) {
        validate(request);
        CampaignEvent event = new CampaignEvent();
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public CampaignEventResponse update(Long id, CampaignEventRequest request) {
        validate(request);
        CampaignEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện."));
        String oldDesktop = event.getDesktopImageUrl();
        String oldMobile = event.getMobileImageUrl();
        apply(event, request);
        CampaignEventResponse saved = toResponse(eventRepository.save(event));
        if (!oldDesktop.equals(event.getDesktopImageUrl())) imageStorage.deleteIfManaged(oldDesktop);
        if (oldMobile != null && !oldMobile.equals(event.getMobileImageUrl())) imageStorage.deleteIfManaged(oldMobile);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        CampaignEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sự kiện."));
        eventRepository.delete(event);
        imageStorage.deleteIfManaged(event.getDesktopImageUrl());
        imageStorage.deleteIfManaged(event.getMobileImageUrl());
    }

    private void apply(CampaignEvent event, CampaignEventRequest request) {
        event.setEventName(request.eventName().trim());
        event.setTitle(request.title().trim());
        event.setMessage(blankToNull(request.message()));
        event.setDesktopImageUrl(request.desktopImageUrl().trim());
        event.setMobileImageUrl(blankToNull(request.mobileImageUrl()));
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setOfferLabel(blankToNull(request.offerLabel()));
        event.setVoucherCode(blankToNull(request.voucherCode()) == null ? null : request.voucherCode().trim().toUpperCase(Locale.ROOT));
        event.setTargetUrl(request.targetUrl().trim());
        event.setPriority(request.priority());
        event.setStatus(resolveStatus(request.status(), request.startAt(), request.endAt()));
    }

    private void validate(CampaignEventRequest request) {
        if (request == null) throw new IllegalArgumentException("Thông tin sự kiện không hợp lệ.");
        if (request.startAt() == null || request.endAt() == null || !request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (request.endAt().isBefore(now())) {
            throw new IllegalArgumentException("Thời gian kết thúc đã qua. Hãy chọn thời gian hiện tại hoặc trong tương lai.");
        }
        if (request.status() == null) throw new IllegalArgumentException("Vui lòng chọn trạng thái sự kiện.");
        validateImageUrl(request.desktopImageUrl(), true);
        validateImageUrl(request.mobileImageUrl(), false);
        validateTargetUrl(request.targetUrl());
        if (request.priority() == null || request.priority() < 0 || request.priority() > 100000) {
            throw new IllegalArgumentException("Độ ưu tiên phải từ 0 đến 100000.");
        }

        String code = blankToNull(request.voucherCode());
        String offerLabel = blankToNull(request.offerLabel());
        if (offerLabel != null && offerLabel.matches(".*\\d+\\s*%.*") && code == null) {
            throw new IllegalArgumentException("Ưu đãi theo phần trăm cần liên kết với mã voucher đang có.");
        }
        if (code != null) {
            Voucher voucher = voucherRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new IllegalArgumentException("Mã voucher liên kết không tồn tại."));
            boolean publishing = request.status() == CampaignEventStatus.ACTIVE || request.status() == CampaignEventStatus.SCHEDULED;
            if (publishing && (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())
                    || voucher.getStartDate() == null || voucher.getEndDate() == null
                    || request.startAt().isBefore(voucher.getStartDate())
                    || request.endAt().isAfter(voucher.getEndDate()))) {
                throw new IllegalArgumentException("Voucher phải đang hoạt động và có thời hạn bao phủ toàn bộ sự kiện. Hãy chỉnh lịch sự kiện hoặc dùng voucher khác.");
            }
        }
    }

    private void validateImageUrl(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) throw new IllegalArgumentException("Vui lòng tải ảnh banner desktop lên.");
            return;
        }
        String url = value.trim();
        if (url.startsWith("/uploads/events/")) {
            if (!url.matches("/uploads/events/[0-9a-fA-F-]{36}\\.(jpg|png)")) {
                throw new IllegalArgumentException("Đường dẫn ảnh banner không hợp lệ.");
            }
            return;
        }
        if (!url.matches("https://[^\\s<>\"']{1,480}")) {
            throw new IllegalArgumentException("Ảnh banner phải là ảnh tải lên hoặc đường dẫn HTTPS.");
        }
    }

    private void validateTargetUrl(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Đường dẫn điều hướng không được để trống.");
        String url = value.trim();
        if (url.startsWith("https://") || url.startsWith("http://")) return;
        if (url.startsWith("//") || url.contains("..") || !url.matches("[a-zA-Z0-9/_?=&%#.-]{1,300}")) {
            throw new IllegalArgumentException("Đường dẫn chỉ được là route nội bộ hoặc URL HTTP(S).");
        }
    }

    private CampaignEventStatus resolveStatus(CampaignEventStatus requested, LocalDateTime startAt, LocalDateTime endAt) {
        if (requested == CampaignEventStatus.DRAFT || requested == CampaignEventStatus.HIDDEN
                || requested == CampaignEventStatus.ENDED) return requested;
        return getScheduledStatus(startAt, endAt, now());
    }

    private CampaignEventStatus getEffectiveStatus(CampaignEvent event, LocalDateTime now) {
        if (event.getStatus() == CampaignEventStatus.DRAFT || event.getStatus() == CampaignEventStatus.HIDDEN
                || event.getStatus() == CampaignEventStatus.ENDED) return event.getStatus();
        return getScheduledStatus(event.getStartAt(), event.getEndAt(), now);
    }

    private CampaignEventStatus getScheduledStatus(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (now.isBefore(startAt)) return CampaignEventStatus.SCHEDULED;
        if (now.isAfter(endAt)) return CampaignEventStatus.ENDED;
        return CampaignEventStatus.ACTIVE;
    }

    private CampaignEventResponse toResponse(CampaignEvent event) {
        return CampaignEventResponse.from(event, getEffectiveStatus(event, now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(eventZone);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public CampaignEventService(CampaignEventRepository eventRepository, VoucherRepository voucherRepository,
            CampaignImageStorageService imageStorage,
            @Value("${fashion.events.zone-id:Asia/Ho_Chi_Minh}") String eventZone) {
        this.eventRepository = eventRepository;
        this.voucherRepository = voucherRepository;
        this.imageStorage = imageStorage;
        this.eventZone = ZoneId.of(eventZone);
    }
}
