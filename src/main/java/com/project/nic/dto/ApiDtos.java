package com.project.nic.dto;

import com.project.nic.model.AssistanceRequest;
import com.project.nic.model.AssistantLog;
import com.project.nic.model.Delivery;
import com.project.nic.model.DeliveryLog;
import com.project.nic.model.Feedback;
import com.project.nic.model.LostNic;
import com.project.nic.model.NewNicForm;
import com.project.nic.model.Payment;
import com.project.nic.model.PaymentRecord;
import com.project.nic.model.RenewNic;
import com.project.nic.model.User;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {
    }

    public static String fileNameOnly(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }
        return Paths.get(storedPath).getFileName().toString();
    }

    public static class UserRequest {
        @Size(max = 100)
        public String firstName;
        @Size(max = 100)
        public String lastName;
        @Email
        @Size(max = 255)
        public String email;
        @Size(min = 6, max = 100)
        public String password;
        @Pattern(regexp = "ADMIN|CITIZEN|PRO|RECOVERY|DELIVERY|FINANCE|ASSISTANT", flags = Pattern.Flag.CASE_INSENSITIVE)
        public String role;

        public User toEntity() {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(role);
            return user;
        }
    }

    public static class NewNicFormDto {
        public Long id;
        public String nameWithInitials;
        public String gender;
        public int age;
        public String civilStatus;
        public String profession;
        public LocalDate birthdate;
        public String address;
        public String contactNumber;
        public String birthCertificatePath;
        public String photoPath;
        public String status;
        public Long userId;
        public String userEmail;

        public static NewNicFormDto from(NewNicForm form) {
            NewNicFormDto dto = new NewNicFormDto();
            dto.id = form.getId();
            dto.nameWithInitials = form.getNameWithInitials();
            dto.gender = form.getGender();
            dto.age = form.getAge();
            dto.civilStatus = form.getCivilStatus();
            dto.profession = form.getProfession();
            dto.birthdate = form.getBirthdate();
            dto.address = form.getAddress();
            dto.contactNumber = form.getContactNumber();
            dto.birthCertificatePath = fileNameOnly(form.getBirthCertificatePath());
            dto.photoPath = fileNameOnly(form.getPhotoPath());
            dto.status = form.getStatus();
            dto.userId = form.getUserId();
            dto.userEmail = form.getUserEmail();
            return dto;
        }
    }

    public static class RenewNicDto {
        public Long id;
        public String oldNicNumber;
        public LocalDate birthdate;
        public String reason;
        public String otherReason;
        public String contactNumber;
        public String birthCertificatePath;
        public String photoPath;
        public String status;
        public Long userId;
        public String userEmail;

        public static RenewNicDto from(RenewNic renewNic) {
            RenewNicDto dto = new RenewNicDto();
            dto.id = renewNic.getId();
            dto.oldNicNumber = renewNic.getOldNicNumber();
            dto.birthdate = renewNic.getBirthdate();
            dto.reason = renewNic.getReason();
            dto.otherReason = renewNic.getOtherReason();
            dto.contactNumber = renewNic.getContactNumber();
            dto.birthCertificatePath = fileNameOnly(renewNic.getBirthCertificatePath());
            dto.photoPath = fileNameOnly(renewNic.getPhotoPath());
            dto.status = renewNic.getStatus();
            dto.userId = renewNic.getUserId();
            dto.userEmail = renewNic.getUserEmail();
            return dto;
        }
    }

    public static class LostNicDto {
        public Long id;
        public String nicNumber;
        public LocalDate lostDate;
        public String contactNumber;
        public String birthCertificatePath;
        public String policeReportPath;
        public String status;
        public Long userId;
        public String userEmail;

        public static LostNicDto from(LostNic lostNic) {
            LostNicDto dto = new LostNicDto();
            dto.id = lostNic.getId();
            dto.nicNumber = lostNic.getNicNumber();
            dto.lostDate = lostNic.getLostDate();
            dto.contactNumber = lostNic.getContactNumber();
            dto.birthCertificatePath = fileNameOnly(lostNic.getBirthCertificatePath());
            dto.policeReportPath = fileNameOnly(lostNic.getPoliceReportPath());
            dto.status = lostNic.getStatus();
            dto.userId = lostNic.getUserId();
            dto.userEmail = lostNic.getUserEmail();
            return dto;
        }
    }

    public static class LostNicUpdateRequest {
        @Size(max = 30)
        public String nicNumber;
        @PastOrPresent
        public LocalDate lostDate;
        @Pattern(regexp = "^[0-9+\\-()\\s]{7,20}$")
        public String contactNumber;

        public LostNic toEntity() {
            LostNic lostNic = new LostNic();
            lostNic.setNicNumber(nicNumber);
            lostNic.setLostDate(lostDate);
            lostNic.setContactNumber(contactNumber);
            return lostNic;
        }
    }

    public static class PaymentDto {
        public Long id;
        @Size(max = 80)
        public String paymentId;
        public LocalDateTime date;
        @Pattern(regexp = "new|renew|lost|New NIC|Renew NIC|Lost NIC", flags = Pattern.Flag.CASE_INSENSITIVE)
        public String serviceType;
        @Pattern(regexp = "card|deposit|online|credit card|bank deposit|online banking", flags = Pattern.Flag.CASE_INSENSITIVE)
        public String paymentMethod;
        @DecimalMin(value = "0.0", inclusive = false)
        public Double amount;
        @Pattern(regexp = "pending|completed|failed", flags = Pattern.Flag.CASE_INSENSITIVE)
        public String status;
        @Positive
        public Long userId;
        @Size(max = 30)
        public String nic;
        @Email
        @Size(max = 255)
        public String email;
        @Size(max = 255)
        public String customerInfo;

        public static PaymentDto from(Payment payment) {
            PaymentDto dto = new PaymentDto();
            dto.id = payment.getId();
            dto.paymentId = payment.getPaymentId();
            dto.date = payment.getDate();
            dto.serviceType = payment.getServiceType();
            dto.paymentMethod = payment.getPaymentMethod();
            dto.amount = payment.getAmount();
            dto.status = payment.getStatus();
            dto.userId = payment.getUserId();
            dto.nic = payment.getNic();
            dto.email = payment.getEmail();
            dto.customerInfo = payment.getCustomerInfo();
            return dto;
        }

        public Payment toEntity() {
            Payment payment = new Payment();
            payment.setPaymentId(paymentId);
            payment.setDate(date);
            payment.setServiceType(serviceType);
            payment.setPaymentMethod(paymentMethod);
            payment.setAmount(amount);
            payment.setStatus(status);
            payment.setUserId(userId);
            payment.setNic(nic);
            payment.setEmail(email);
            payment.setCustomerInfo(customerInfo);
            return payment;
        }
    }

    public static class PaymentRecordDto {
        public Long id;
        public String userId;
        public String nicType;
        public String nicReference;
        public double amount;
        public String paymentMethod;
        public String transactionId;
        public LocalDateTime transactionDate;

        public static PaymentRecordDto from(PaymentRecord record) {
            PaymentRecordDto dto = new PaymentRecordDto();
            dto.id = record.getId();
            dto.userId = record.getUserId();
            dto.nicType = record.getNicType();
            dto.nicReference = record.getNicReference();
            dto.amount = record.getAmount();
            dto.paymentMethod = record.getPaymentMethod();
            dto.transactionId = record.getTransactionId();
            dto.transactionDate = record.getTransactionDate();
            return dto;
        }
    }

    public static class DeliveryDto {
        @Size(max = 30)
        public String nic;
        @Positive
        public Long appId;
        @Size(max = 150)
        public String recipient;
        public LocalDate deliveryDate;
        @Size(max = 50)
        public String method;
        @Size(max = 50)
        public String status;
        @Size(max = 255)
        public String address;

        public static DeliveryDto from(Delivery delivery) {
            DeliveryDto dto = new DeliveryDto();
            dto.nic = delivery.getNic();
            dto.appId = delivery.getAppId();
            dto.recipient = delivery.getRecipient();
            dto.deliveryDate = delivery.getDeliveryDate();
            dto.method = delivery.getMethod();
            dto.status = delivery.getStatus();
            dto.address = delivery.getAddress();
            return dto;
        }

        public Delivery toEntity() {
            Delivery delivery = new Delivery();
            delivery.setNic(nic);
            delivery.setAppId(appId);
            delivery.setRecipient(recipient);
            delivery.setDeliveryDate(deliveryDate);
            delivery.setMethod(method);
            delivery.setStatus(status);
            delivery.setAddress(address);
            return delivery;
        }
    }

    public static class AssistanceRequestDto {
        public Long id;
        @Positive
        public Long userId;
        @Email
        @Size(max = 255)
        public String email;
        @Size(min = 2, max = 1000)
        public String query;
        @Size(max = 50)
        public String status;
        @Size(max = 1000)
        public String reply;
        @Positive
        public Long applicantId;

        public static AssistanceRequestDto from(AssistanceRequest request) {
            AssistanceRequestDto dto = new AssistanceRequestDto();
            dto.id = request.getId();
            dto.userId = request.getUserId();
            dto.email = request.getEmail();
            dto.query = request.getQuery();
            dto.status = request.getStatus();
            dto.reply = request.getReply();
            dto.applicantId = request.getApplicantId();
            return dto;
        }

        public AssistanceRequest toEntity() {
            AssistanceRequest request = new AssistanceRequest();
            request.setUserId(userId);
            request.setEmail(email);
            request.setQuery(query);
            request.setStatus(status);
            request.setReply(reply);
            request.setApplicantId(applicantId);
            return request;
        }
    }

    public static class FeedbackDto {
        public Long id;
        @Size(max = 150)
        public String name;
        @Email
        @Size(max = 255)
        public String mail;
        @Size(max = 50)
        public String type;
        @Size(max = 150)
        public String subject;
        @Size(min = 2, max = 1000)
        public String message;
        @Pattern(regexp = "Pending|In Progress|Resolved|Reviewed", flags = Pattern.Flag.CASE_INSENSITIVE)
        public String status;
        @Size(max = 1000)
        public String reply;

        public static FeedbackDto from(Feedback feedback) {
            FeedbackDto dto = new FeedbackDto();
            dto.id = feedback.getId();
            dto.name = feedback.getName();
            dto.mail = feedback.getMail();
            dto.type = feedback.getType();
            dto.subject = feedback.getSubject();
            dto.message = feedback.getMessage();
            dto.status = feedback.getStatus();
            dto.reply = feedback.getReply();
            return dto;
        }

        public Feedback toEntity() {
            Feedback feedback = new Feedback();
            feedback.setName(name);
            feedback.setMail(mail);
            feedback.setType(type);
            feedback.setSubject(subject);
            feedback.setMessage(message);
            feedback.setStatus(status);
            feedback.setReply(reply);
            return feedback;
        }
    }

    public static class LogDto {
        public Long id;
        @PastOrPresent
        public LocalDate date;
        @Size(min = 2, max = 1000)
        public String description;

        public static LogDto from(AssistantLog log) {
            LogDto dto = new LogDto();
            dto.id = log.getId();
            dto.date = log.getDate();
            dto.description = log.getDescription();
            return dto;
        }

        public static LogDto from(DeliveryLog log) {
            LogDto dto = new LogDto();
            dto.id = log.getId();
            dto.date = log.getDate();
            dto.description = log.getDescription();
            return dto;
        }

        public AssistantLog toAssistantLog() {
            return new AssistantLog(date, description);
        }

        public DeliveryLog toDeliveryLog() {
            return new DeliveryLog(date, description);
        }
    }

    public static class StatusUpdateRequest {
        @NotBlank
        @Pattern(regexp = "PENDING|PROCESSING|APPROVED|REJECTED|DELIVERED|pending|completed|failed|Pending|In Progress|Resolved|Reviewed")
        public String status;
    }

    public static class AssistanceUpdateRequest {
        @Size(min = 2, max = 1000)
        public String query;
    }
}
