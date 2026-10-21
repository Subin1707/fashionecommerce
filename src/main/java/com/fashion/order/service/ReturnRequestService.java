package com.fashion.order.service;

import com.fashion.order.dto.CreateReturnRequest;
import com.fashion.order.entity.Order;
import com.fashion.order.entity.OrderItem;
import com.fashion.order.entity.ReturnRequest;
import com.fashion.order.repository.OrderItemRepository;
import com.fashion.order.repository.OrderRepository;
import com.fashion.order.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ReturnRequestService {
    private final ReturnRequestRepository repository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final Path evidenceRoot;

    public ReturnRequestService(ReturnRequestRepository repository, OrderRepository orderRepository, OrderItemRepository itemRepository,
            @Value("${fashion.upload.directory:uploads}") String uploadDirectory) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.evidenceRoot = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("returns").normalize();
    }

    public ReturnRequest create(Long userId, Long orderId, CreateReturnRequest request, List<MultipartFile> files) {
        validateEvidence(files);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (!List.of("DELIVERED", "COMPLETED").contains(order.getOrderStatus()))
            throw new IllegalArgumentException("Chỉ có thể yêu cầu đổi trả sau khi đã nhận hàng");
        OrderItem item = itemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn hàng"));
        if (!orderId.equals(item.getOrder().getId())) throw new IllegalArgumentException("Sản phẩm không thuộc đơn hàng này");
        if (request.getQuantity() > item.getQuantity()) throw new IllegalArgumentException("Số lượng yêu cầu vượt quá số lượng đã mua");
        LocalDateTime receivedAt = order.getCompletedAt() == null ? order.getUpdatedAt() : order.getCompletedAt();
        if (receivedAt != null && receivedAt.isBefore(LocalDateTime.now().minusDays(14)))
            throw new IllegalArgumentException("Đã quá thời hạn yêu cầu đổi trả 14 ngày");
        if (repository.existsByOrderItemIdAndStatusIn(item.getId(), List.of("PENDING", "APPROVED", "RECEIVED", "COMPLETED")))
            throw new IllegalArgumentException("Sản phẩm này đã có yêu cầu đổi trả đang xử lý");
        ReturnRequest saved = repository.save(ReturnRequest.builder().orderId(orderId).orderItemId(item.getId()).userId(userId)
                .requestType(request.getRequestType().toUpperCase()).quantity(request.getQuantity())
                .reason(request.getReason().trim()).status("PENDING").build());
        List<String> stored = new ArrayList<>();
        try {
            Path requestDirectory = evidenceRoot.resolve(saved.getId().toString()).normalize();
            if (!requestDirectory.startsWith(evidenceRoot)) throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ");
            Files.createDirectories(requestDirectory);
            for (MultipartFile file : files) {
                String extension = validatedExtension(file);
                String filename = UUID.randomUUID() + "." + extension;
                Files.write(requestDirectory.resolve(filename), file.getBytes(), StandardOpenOption.CREATE_NEW);
                stored.add("/api/orders/return-requests/" + saved.getId() + "/evidence/" + filename);
            }
            saved.setEvidenceFiles(stored);
            return repository.save(saved);
        } catch (IOException | RuntimeException exception) {
            deleteStoredFiles(saved.getId(), stored);
            throw exception instanceof RuntimeException runtime ? runtime : new IllegalArgumentException("Không lưu được tệp minh chứng", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getMine(Long userId) { return repository.findByUserIdOrderByCreatedAtDesc(userId); }

    @Transactional(readOnly = true)
    public List<ReturnRequest> getAll() { return repository.findAllByOrderByCreatedAtDesc(); }

    @Transactional(readOnly = true)
    public Path getEvidence(Long userId, boolean admin, Long requestId, String filename) {
        if (filename == null || !filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp|mp4|webm)"))
            throw new IllegalArgumentException("Tệp minh chứng không hợp lệ");
        ReturnRequest request = repository.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đổi trả"));
        if (!admin && !request.getUserId().equals(userId)) throw new org.springframework.security.access.AccessDeniedException("Access denied");
        String expectedUrl = "/api/orders/return-requests/" + requestId + "/evidence/" + filename;
        if (!request.getEvidenceFiles().contains(expectedUrl)) throw new IllegalArgumentException("Không tìm thấy tệp minh chứng");
        Path file = evidenceRoot.resolve(requestId.toString()).resolve(filename).normalize();
        if (!file.startsWith(evidenceRoot) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Không tìm thấy tệp minh chứng");
        return file;
    }

    private void validateEvidence(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > 5)
            throw new IllegalArgumentException("Vui lòng chọn từ 1 đến 5 ảnh hoặc video minh chứng");
        for (MultipartFile file : files) validatedExtension(file);
    }

    private String validatedExtension(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 20L * 1024 * 1024)
            throw new IllegalArgumentException("Mỗi tệp minh chứng phải có dung lượng không quá 20 MB");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            default -> throw new IllegalArgumentException("Chỉ nhận ảnh JPEG, PNG, WebP hoặc video MP4, WebM");
        };
        try {
            byte[] header = file.getBytes();
            boolean valid = switch (extension) {
                case "jpg" -> header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
                case "png" -> header.length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
                case "webp" -> header.length >= 12 && new String(header, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                        && new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
                case "mp4" -> header.length >= 8 && new String(header, 4, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp");
                case "webm" -> header.length >= 4 && (header[0] & 0xff) == 0x1a && (header[1] & 0xff) == 0x45
                        && (header[2] & 0xff) == 0xdf && (header[3] & 0xff) == 0xa3;
                default -> false;
            };
            if (!valid) throw new IllegalArgumentException("Nội dung tệp không khớp với định dạng đã chọn");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không đọc được tệp minh chứng", exception);
        }
        return extension;
    }

    private void deleteStoredFiles(Long requestId, List<String> urls) {
        for (String url : urls) {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            try { Files.deleteIfExists(evidenceRoot.resolve(requestId.toString()).resolve(filename).normalize()); }
            catch (IOException ignored) { }
        }
    }

    public ReturnRequest review(Long id, String status, String note) {
        ReturnRequest request = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu"));
        String current = request.getStatus();
        boolean valid = ("PENDING".equals(current) && List.of("APPROVED", "REJECTED").contains(status))
                || ("APPROVED".equals(current) && "RECEIVED".equals(status))
                || ("RECEIVED".equals(current) && "COMPLETED".equals(status));
        if (!valid) throw new IllegalArgumentException("Không thể chuyển yêu cầu từ " + current + " sang " + status);
        request.setStatus(status);
        request.setAdminNote(note == null ? null : note.trim());
        return repository.save(request);
    }
}
