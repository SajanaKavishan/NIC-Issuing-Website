package com.project.nic.service;

import com.project.nic.model.Delivery;
import com.project.nic.repository.DeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeliveryService {
    @Autowired
    private DeliveryRepository deliveryRepository;

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

    public Delivery saveDelivery(Delivery delivery) {
        return deliveryRepository.save(delivery);
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
}