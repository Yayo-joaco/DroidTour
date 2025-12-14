package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class User {
    @DocumentId
    private String userId;

    private String email;
    private String userType; // CLIENT, GUIDE, COMPANY_ADMIN, SUPERADMIN
    private String status; // active, inactive, pending_approval
    @ServerTimestamp private Date createdAt;

    // Solo para CLIENT y GUIDE
    private PersonalData personalData;

    // Solo para COMPANY_ADMIN
    private String companyId; // Referencia a companies/{companyId}

    // Clase interna para datos personales
    public static class PersonalData {
        private String firstName;
        private String lastName;
        private String fullName;
        private String documentType; // DNI, Pasaporte
        private String documentNumber;
        private String dateOfBirth;
        private String phoneNumber;
        private String profileImageUrl;

        // Constructor, getters y setters
        public PersonalData() {}

        public PersonalData(String firstName, String lastName, String documentType,
                            String documentNumber, String dateOfBirth, String phoneNumber) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.fullName = firstName + " " + lastName;
            this.documentType = documentType;
            this.documentNumber = documentNumber;
            this.dateOfBirth = dateOfBirth;
            this.phoneNumber = phoneNumber;
        }

        // Getters y setters...

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
        }

        public String getDocumentNumber() {
            return documentNumber;
        }

        public void setDocumentNumber(String documentNumber) {
            this.documentNumber = documentNumber;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getProfileImageUrl() {
            return profileImageUrl;
        }

        public void setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
        }
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public PersonalData getPersonalData() {
        return personalData;
    }

    public void setPersonalData(PersonalData personalData) {
        this.personalData = personalData;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    // Constructor vacío
    public User() {}

    // Factory methods por tipo de usuario
    public static User createClient(String email, String firstName, String lastName,
                                    String documentType, String documentNumber,
                                    String dateOfBirth, String phoneNumber) {
        User user = new User();
        user.setEmail(email);
        user.setUserType("CLIENT");
        user.setStatus("active");
        user.setPersonalData(new PersonalData(firstName, lastName, documentType,
                documentNumber, dateOfBirth, phoneNumber));
        return user;
    }

    public static User createGuide(String email, String firstName, String lastName,
                                   String documentType, String documentNumber,
                                   String dateOfBirth, String phoneNumber) {
        User user = new User();
        user.setEmail(email);
        user.setUserType("GUIDE");
        user.setStatus("pending_approval");
        user.setPersonalData(new PersonalData(firstName, lastName, documentType,
                documentNumber, dateOfBirth, phoneNumber));
        return user;
    }

    public static User createCompanyAdmin(String email, String companyId) {
        User user = new User();
        user.setEmail(email);
        user.setUserType("COMPANY_ADMIN");
        user.setStatus("active");
        user.setCompanyId(companyId);
        return user;
    }

    //realmente no lo usaremos
    public static User createSuperAdmin(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setUserType("SUPERADMIN");
        user.setStatus("active");
        user.setPersonalData(new PersonalData(firstName, lastName, null, null, null, null));
        return user;
    }

    // Métodos helper
    public boolean isClient() { return "CLIENT".equals(userType); }
    public boolean isGuide() { return "GUIDE".equals(userType); }
    public boolean isCompanyAdmin() { return "COMPANY_ADMIN".equals(userType); }
    public boolean isSuperAdmin() { return "SUPERADMIN".equals(userType); }







    // ====== Compatibilidad (legacy getters/setters) ======
    public String getFirstName() {
        return personalData != null ? personalData.getFirstName() : null;
    }
    public void setFirstName(String firstName) {
        if (personalData == null) personalData = new PersonalData();
        personalData.setFirstName(firstName);
    }

    public String getLastName() {
        return personalData != null ? personalData.getLastName() : null;
    }
    public void setLastName(String lastName) {
        if (personalData == null) personalData = new PersonalData();
        personalData.setLastName(lastName);
    }

    public String getFullName() {
        return personalData != null ? personalData.getFullName() : null;
    }
    public void setFullName(String fullName) {
        if (personalData == null) personalData = new PersonalData();
        personalData.setFullName(fullName);
    }

    // Si tu adapter/activity usa photoUrl:
    public String getPhotoUrl() {
        return personalData != null ? personalData.getProfileImageUrl() : null;
    }




}
