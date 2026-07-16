package com.project.nic.repository;

import com.project.nic.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    List<Delivery> findByStatusAndDeliveryDateGreaterThanEqualAndMethod(String status, LocalDate date, String method);
    List<Delivery> findByStatusAndMethod(String status, String method);
    List<Delivery> findByStatusAndNicContainingIgnoreCaseOrRecipientContainingIgnoreCase(String status, String nicSearch, String recipientSearch);
    List<Delivery> findByStatusAndDeliveryDateBetween(String status, LocalDate startDate, LocalDate endDate);
}