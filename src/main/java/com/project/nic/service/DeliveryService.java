package com.project.nic.service;

import com.project.nic.model.Delivery;
import com.project.nic.repository.DeliveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryService.class);
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PROCESSING", "APPROVED", "REJECTED", "DELIVERED");

    private final DeliveryRepository deliveryRepository;
    private final NewNicFormService newNicFormService;
    private final RenewNicService renewNicService;
    private final LostNicService lostNicService;

    public DeliveryService(
            DeliveryRepository deliveryRepository,
            NewNicFormService newNicFormService,
            RenewNicService renewNicService,
            LostNicService lostNicService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.newNicFormService = newNicFormService;
        this.renewNicService = renewNicService;
        this.lostNicService = lostNicService;
    }

    public List<Delivery> getAllDeliveries(String dateRange, String deliveryMethod, String search) {
        // Fetch all deliveries and filter in-memory to keep logic simple and correct.
        List<Delivery> all = deliveryRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate startDate = null;

        if ("today".equalsIgnoreCase(dateRange)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(dateRange)) {
            startDate = today.minusDays(7);
        } else if ("month".equalsIgnoreCase(dateRange)) {
            startDate = today.minusDays(30);
        }

        final LocalDate finalStart = startDate; // effectively final for lambda
        String methodFilter = (deliveryMethod == null || deliveryMethod.equalsIgnoreCase("all")) ? null : deliveryMethod.toLowerCase();
        String searchFilter = (search == null || search.trim().isEmpty()) ? null : search.trim().toLowerCase();

        return all.stream()
                 // date range filter
                 .filter(d -> {
                     if (finalStart == null) return true;
                     if (d.getDeliveryDate() == null) return false;
                     return !d.getDeliveryDate().isBefore(finalStart) && !d.getDeliveryDate().isAfter(today);
                 })
                 // method filter (be tolerant: dropdown uses shorthand like 'postal' but DB may store 'Postal Service')
                 .filter(d -> {
                     if (methodFilter == null) return true;
                     String m = d.getMethod() == null ? "" : d.getMethod().trim().toLowerCase();
                     switch (methodFilter) {
                         case "postal":
                             return m.contains("postal");
                         case "courier":
                             return m.contains("courier");
                         case "pickup":
                             return m.contains("pickup") || m.contains("pick up") || m.contains("office pickup");
                         default:
                             return m.equals(methodFilter) || m.contains(methodFilter);
                     }
                 })
                 // search filter (NIC or recipient)
                 .filter(d -> {
                     if (searchFilter == null) return true;
                     boolean nicMatch = d.getNic() != null && d.getNic().trim().toLowerCase().contains(searchFilter);
                     boolean recipientMatch = d.getRecipient() != null && d.getRecipient().trim().toLowerCase().contains(searchFilter);
                     return nicMatch || recipientMatch;
                 })
                 .collect(Collectors.toList());
    }

    public Optional<Delivery> getDeliveryByNic(String nic) {
        return deliveryRepository.findById(nic);
    }

    @Transactional
    public Delivery saveDelivery(Delivery delivery) {
        delivery.setStatus(normalizeStatus(delivery.getStatus()));
        Delivery saved = deliveryRepository.save(delivery);
        updateRelatedApplicationStatus(saved);
        return saved;
    }

    public List<Delivery> getAll() {
        return deliveryRepository.findAll();
    }

    public List<Delivery> getWeeklyDeliveries(LocalDate startDate) {
        if (startDate == null) return List.of();
        LocalDate endDate = startDate.plusDays(6);
        // fallback: filter in-memory for Delivered between startDate and endDate
        return deliveryRepository.findAll().stream()
                .filter(d -> d != null && "Delivered".equalsIgnoreCase(d.getStatus()) && d.getDeliveryDate() != null)
                .filter(d -> !d.getDeliveryDate().isBefore(startDate) && !d.getDeliveryDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    // Delete a delivery by NIC. Returns true if a delivery was found and deleted, false otherwise.
    public boolean deleteByNic(String nic) {
        if (nic == null) return false;
        if (!deliveryRepository.existsById(nic)) return false;
        deliveryRepository.deleteById(nic);
        return true;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "PENDING" : status.trim().toUpperCase().replaceAll("\\s+", "_");
        if ("IN_TRANSIT".equals(normalized)) return "PROCESSING";
        if ("RETURNED".equals(normalized) || "CANCELLED".equals(normalized)) return "REJECTED";
        return ALLOWED_STATUSES.contains(normalized) ? normalized : "PENDING";
    }

    private void updateRelatedApplicationStatus(Delivery delivery) {
        String status = normalizeStatus(delivery.getStatus());

        if (delivery.getAppId() != null) {
            if (updateByApplicationId(delivery.getAppId(), status)) {
                return;
            }
        }

        String nic = delivery.getNic();
        if (nic != null && !nic.isBlank()) {
            Optional<com.project.nic.model.RenewNic> renewNic = renewNicService.findLatestByOldNicNumber(nic);
            if (renewNic.isPresent()) {
                Long id = renewNic.get().getId();
                renewNicService.updateStatus(id, status);
                logger.info("Updated Renew NIC application {} to {} from delivery {}", id, status, nic);
                return;
            }
            Optional<com.project.nic.model.LostNic> lostNic = lostNicService.findLatestByNicNumber(nic);
            if (lostNic.isPresent()) {
                Long id = lostNic.get().getId();
                lostNicService.updateStatus(id, status);
                logger.info("Updated Lost NIC application {} to {} from delivery {}", id, status, nic);
                return;
            }
        }

        logger.warn("No related NIC application found for delivery nic='{}', appId='{}'", delivery.getNic(), delivery.getAppId());
    }

    private boolean updateByApplicationId(Long appId, String status) {
        if (newNicFormService.findById(appId).isPresent()) {
            newNicFormService.updateStatus(appId, status);
            logger.info("Updated New NIC application {} to {} from delivery appId", appId, status);
            return true;
        }
        if (renewNicService.findById(appId).isPresent()) {
            renewNicService.updateStatus(appId, status);
            logger.info("Updated Renew NIC application {} to {} from delivery appId", appId, status);
            return true;
        }
        if (lostNicService.findById(appId).isPresent()) {
            lostNicService.updateStatus(appId, status);
            logger.info("Updated Lost NIC application {} to {} from delivery appId", appId, status);
            return true;
        }
        return false;
    }
}
