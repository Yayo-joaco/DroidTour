package com.example.droidtour.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "DroidTour.db";
    private static final int DATABASE_VERSION = 1;

    // ==================== TABLES ====================
    
    // Table: Tours (for Guides)
    private static final String TABLE_TOURS = "tours";
    private static final String TOUR_ID = "id";
    private static final String TOUR_NAME = "name";
    private static final String TOUR_COMPANY = "company";
    private static final String TOUR_DATE = "date";
    private static final String TOUR_TIME = "time";
    private static final String TOUR_STATUS = "status"; // EN_PROGRESO, PROGRAMADO, COMPLETADO
    private static final String TOUR_PAYMENT = "payment";
    private static final String TOUR_PARTICIPANTS = "participants";

    // Table: Offers (for Guides)
    private static final String TABLE_OFFERS = "offers";
    private static final String OFFER_ID = "id";
    private static final String OFFER_TOUR_NAME = "tour_name";
    private static final String OFFER_COMPANY = "company";
    private static final String OFFER_DATE = "date";
    private static final String OFFER_TIME = "time";
    private static final String OFFER_PAYMENT = "payment";
    private static final String OFFER_STATUS = "status"; // PENDIENTE, RECHAZADA
    private static final String OFFER_PARTICIPANTS = "participants";

    // Table: Reservations (for Clients)
    private static final String TABLE_RESERVATIONS = "reservations";
    private static final String RES_ID = "id";
    private static final String RES_TOUR_NAME = "tour_name";
    private static final String RES_COMPANY = "company";
    private static final String RES_DATE = "date";
    private static final String RES_TIME = "time";
    private static final String RES_STATUS = "status"; // CONFIRMADA, COMPLETADA, PENDIENTE
    private static final String RES_PRICE = "price";
    private static final String RES_PEOPLE = "people";
    private static final String RES_QR_CODE = "qr_code";

    // Table: Notifications (for Clients)
    private static final String TABLE_NOTIFICATIONS = "notifications";
    private static final String NOTIF_ID = "id";
    private static final String NOTIF_TYPE = "type"; // RESERVATION_CONFIRMED, PAYMENT_CONFIRMED, TOUR_REMINDER, TOUR_COMPLETED
    private static final String NOTIF_TITLE = "title";
    private static final String NOTIF_MESSAGE = "message";
    private static final String NOTIF_TIMESTAMP = "timestamp";
    private static final String NOTIF_IS_READ = "is_read"; // 0 = no visto, 1 = visto

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Tours table
        String CREATE_TOURS_TABLE = "CREATE TABLE " + TABLE_TOURS + "("
                + TOUR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TOUR_NAME + " TEXT,"
                + TOUR_COMPANY + " TEXT,"
                + TOUR_DATE + " TEXT,"
                + TOUR_TIME + " TEXT,"
                + TOUR_STATUS + " TEXT,"
                + TOUR_PAYMENT + " REAL,"
                + TOUR_PARTICIPANTS + " INTEGER"
                + ")";
        db.execSQL(CREATE_TOURS_TABLE);

        // Create Offers table
        String CREATE_OFFERS_TABLE = "CREATE TABLE " + TABLE_OFFERS + "("
                + OFFER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + OFFER_TOUR_NAME + " TEXT,"
                + OFFER_COMPANY + " TEXT,"
                + OFFER_DATE + " TEXT,"
                + OFFER_TIME + " TEXT,"
                + OFFER_PAYMENT + " REAL,"
                + OFFER_STATUS + " TEXT,"
                + OFFER_PARTICIPANTS + " INTEGER"
                + ")";
        db.execSQL(CREATE_OFFERS_TABLE);

        // Create Reservations table
        String CREATE_RESERVATIONS_TABLE = "CREATE TABLE " + TABLE_RESERVATIONS + "("
                + RES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + RES_TOUR_NAME + " TEXT,"
                + RES_COMPANY + " TEXT,"
                + RES_DATE + " TEXT,"
                + RES_TIME + " TEXT,"
                + RES_STATUS + " TEXT,"
                + RES_PRICE + " REAL,"
                + RES_PEOPLE + " INTEGER,"
                + RES_QR_CODE + " TEXT"
                + ")";
        db.execSQL(CREATE_RESERVATIONS_TABLE);

        // Create Notifications table
        String CREATE_NOTIFICATIONS_TABLE = "CREATE TABLE " + TABLE_NOTIFICATIONS + "("
                + NOTIF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NOTIF_TYPE + " TEXT,"
                + NOTIF_TITLE + " TEXT,"
                + NOTIF_MESSAGE + " TEXT,"
                + NOTIF_TIMESTAMP + " TEXT,"
                + NOTIF_IS_READ + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_NOTIFICATIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Crear tabla de notificaciones si no existe (para bases de datos antiguas)
        String CREATE_NOTIFICATIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + "("
                + NOTIF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + NOTIF_TYPE + " TEXT,"
                + NOTIF_TITLE + " TEXT,"
                + NOTIF_MESSAGE + " TEXT,"
                + NOTIF_TIMESTAMP + " TEXT,"
                + NOTIF_IS_READ + " INTEGER DEFAULT 0"
                + ")";
        db.execSQL(CREATE_NOTIFICATIONS_TABLE);
        
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOURS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESERVATIONS);
        onCreate(db);
    }

    // ==================== GUIDE: TOURS ====================
    
    public long addTour(String name, String company, String date, String time, 
                        String status, double payment, int participants) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TOUR_NAME, name);
        values.put(TOUR_COMPANY, company);
        values.put(TOUR_DATE, date);
        values.put(TOUR_TIME, time);
        values.put(TOUR_STATUS, status);
        values.put(TOUR_PAYMENT, payment);
        values.put(TOUR_PARTICIPANTS, participants);
        
        long id = db.insert(TABLE_TOURS, null, values);
        db.close();
        return id;
    }

    public List<Tour> getAllTours() {
        List<Tour> tourList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_TOURS + " ORDER BY " + TOUR_ID + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                Tour tour = new Tour();
                tour.setId(cursor.getInt(0));
                tour.setName(cursor.getString(1));
                tour.setCompany(cursor.getString(2));
                tour.setDate(cursor.getString(3));
                tour.setTime(cursor.getString(4));
                tour.setStatus(cursor.getString(5));
                tour.setPayment(cursor.getDouble(6));
                tour.setParticipants(cursor.getInt(7));
                tourList.add(tour);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return tourList;
    }

    public int updateTourStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TOUR_STATUS, status);
        
        int rowsAffected = db.update(TABLE_TOURS, values, TOUR_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    // ==================== GUIDE: OFFERS ====================
    
    public long addOffer(String tourName, String company, String date, String time, 
                         double payment, String status, int participants) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(OFFER_TOUR_NAME, tourName);
        values.put(OFFER_COMPANY, company);
        values.put(OFFER_DATE, date);
        values.put(OFFER_TIME, time);
        values.put(OFFER_PAYMENT, payment);
        values.put(OFFER_STATUS, status);
        values.put(OFFER_PARTICIPANTS, participants);
        
        long id = db.insert(TABLE_OFFERS, null, values);
        db.close();
        return id;
    }

    public List<Offer> getAllOffers() {
        List<Offer> offerList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_OFFERS + " ORDER BY " + OFFER_ID + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                Offer offer = new Offer();
                offer.setId(cursor.getInt(0));
                offer.setTourName(cursor.getString(1));
                offer.setCompany(cursor.getString(2));
                offer.setDate(cursor.getString(3));
                offer.setTime(cursor.getString(4));
                offer.setPayment(cursor.getDouble(5));
                offer.setStatus(cursor.getString(6));
                offer.setParticipants(cursor.getInt(7));
                offerList.add(offer);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return offerList;
    }

    public int updateOfferStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(OFFER_STATUS, status);
        
        int rowsAffected = db.update(TABLE_OFFERS, values, OFFER_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    // ==================== CLIENT: RESERVATIONS ====================
    
    public long addReservation(String tourName, String company, String date, String time,
                               String status, double price, int people, String qrCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RES_TOUR_NAME, tourName);
        values.put(RES_COMPANY, company);
        values.put(RES_DATE, date);
        values.put(RES_TIME, time);
        values.put(RES_STATUS, status);
        values.put(RES_PRICE, price);
        values.put(RES_PEOPLE, people);
        values.put(RES_QR_CODE, qrCode);
        
        long id = db.insert(TABLE_RESERVATIONS, null, values);
        db.close();
        return id;
    }

    public List<Reservation> getAllReservations() {
        List<Reservation> resList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_RESERVATIONS + " ORDER BY " + RES_ID + " DESC";
        
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        
        if (cursor.moveToFirst()) {
            do {
                Reservation res = new Reservation();
                res.setId(cursor.getInt(0));
                res.setTourName(cursor.getString(1));
                res.setCompany(cursor.getString(2));
                res.setDate(cursor.getString(3));
                res.setTime(cursor.getString(4));
                res.setStatus(cursor.getString(5));
                res.setPrice(cursor.getDouble(6));
                res.setPeople(cursor.getInt(7));
                res.setQrCode(cursor.getString(8));
                resList.add(res);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return resList;
    }

    public int updateReservationStatus(int id, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RES_STATUS, status);
        
        int rowsAffected = db.update(TABLE_RESERVATIONS, values, RES_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    public void deleteReservation(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RESERVATIONS, RES_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // ==================== CLIENT: NOTIFICATIONS ====================
    
    public long addNotification(String type, String title, String message, String timestamp) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Verificar si la tabla existe, si no, crearla
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NOTIFICATIONS + "'", null);
            boolean tableExists = cursor.getCount() > 0;
            cursor.close();
            
            if (!tableExists) {
                String CREATE_NOTIFICATIONS_TABLE = "CREATE TABLE " + TABLE_NOTIFICATIONS + "("
                        + NOTIF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + NOTIF_TYPE + " TEXT,"
                        + NOTIF_TITLE + " TEXT,"
                        + NOTIF_MESSAGE + " TEXT,"
                        + NOTIF_TIMESTAMP + " TEXT,"
                        + NOTIF_IS_READ + " INTEGER DEFAULT 0"
                        + ")";
                db.execSQL(CREATE_NOTIFICATIONS_TABLE);
            }
            
            ContentValues values = new ContentValues();
            values.put(NOTIF_TYPE, type);
            values.put(NOTIF_TITLE, title);
            values.put(NOTIF_MESSAGE, message);
            values.put(NOTIF_TIMESTAMP, timestamp);
            values.put(NOTIF_IS_READ, 0); // Por defecto no vista
            
            long id = db.insert(TABLE_NOTIFICATIONS, null, values);
            db.close();
            return id;
        } catch (Exception e) {
            return -1;
        }
    }

    public List<Notification> getAllNotifications() {
        List<Notification> notifList = new ArrayList<>();
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            // Verificar si la tabla existe
            Cursor checkCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NOTIFICATIONS + "'", null);
            boolean tableExists = checkCursor.getCount() > 0;
            checkCursor.close();
            
            if (!tableExists) {
                db.close();
                return notifList;
            }
            
            String selectQuery = "SELECT * FROM " + TABLE_NOTIFICATIONS + " ORDER BY " + NOTIF_ID + " DESC";
            Cursor cursor = db.rawQuery(selectQuery, null);
            
            if (cursor.moveToFirst()) {
                do {
                    Notification notif = new Notification();
                    notif.setId(cursor.getInt(0));
                    notif.setType(cursor.getString(1));
                    notif.setTitle(cursor.getString(2));
                    notif.setMessage(cursor.getString(3));
                    notif.setTimestamp(cursor.getString(4));
                    notif.setRead(cursor.getInt(5) == 1);
                    notifList.add(notif);
                } while (cursor.moveToNext());
            }
            
            cursor.close();
            db.close();
        } catch (Exception e) {
            // Retornar lista vacía en caso de error
        }
        return notifList;
    }

    public int getUnreadNotificationsCount() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            // Verificar si la tabla existe
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NOTIFICATIONS + "'", null);
            boolean tableExists = cursor.getCount() > 0;
            cursor.close();
            
            if (!tableExists) {
                db.close();
                return 0;
            }
            
            String selectQuery = "SELECT COUNT(*) FROM " + TABLE_NOTIFICATIONS + " WHERE " + NOTIF_IS_READ + " = 0";
            cursor = db.rawQuery(selectQuery, null);
            
            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            
            cursor.close();
            db.close();
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    public void markAllNotificationsAsRead() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Verificar si la tabla existe
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NOTIFICATIONS + "'", null);
            boolean tableExists = cursor.getCount() > 0;
            cursor.close();
            
            if (!tableExists) {
                db.close();
                return;
            }
            
            ContentValues values = new ContentValues();
            values.put(NOTIF_IS_READ, 1);
            
            db.update(TABLE_NOTIFICATIONS, values, null, null);
            db.close();
        } catch (Exception e) {
            // Ignorar errores
        }
    }
    
    public void deleteAllNotifications() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Verificar si la tabla existe
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NOTIFICATIONS + "'", null);
            boolean tableExists = cursor.getCount() > 0;
            cursor.close();
            
            if (tableExists) {
                db.delete(TABLE_NOTIFICATIONS, null, null);
            }
            db.close();
        } catch (Exception e) {
            // Ignorar errores
        }
    }

    public void markNotificationAsRead(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NOTIF_IS_READ, 1);
        
        db.update(TABLE_NOTIFICATIONS, values, NOTIF_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }

    // ==================== MODEL CLASSES ====================
    
    // Modelo para reseñas (para clientes)
    public static class Review {
        private String userName;
        private String userInitial;
        private double rating;
        private String reviewText;
        private String reviewDate;
        private String tourName;

        public Review() {}

        public Review(String userName, String userInitial, double rating, String reviewText, String reviewDate, String tourName) {
            this.userName = userName;
            this.userInitial = userInitial;
            this.rating = rating;
            this.reviewText = reviewText;
            this.reviewDate = reviewDate;
            this.tourName = tourName;
        }

        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserInitial() { return userInitial; }
        public void setUserInitial(String userInitial) { this.userInitial = userInitial; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
        public String getReviewDate() { return reviewDate; }
        public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
        public String getTourName() { return tourName; }
        public void setTourName(String tourName) { this.tourName = tourName; }
    }

    // Modelo para empresas (para catálogo de empresas)
    public static class Company {
        private String name;
        private String location;
        private double rating;
        private int reviewsCount;
        private int toursCount;
        private int clientsCount;
        private double priceFrom;
        private String description;
        private boolean favorite;

        public Company() {}

        public Company(String name, String location, double rating, int reviewsCount,
                        int toursCount, int clientsCount, double priceFrom, String description) {
            this.name = name;
            this.location = location;
            this.rating = rating;
            this.reviewsCount = reviewsCount;
            this.toursCount = toursCount;
            this.clientsCount = clientsCount;
            this.priceFrom = priceFrom;
            this.description = description;
            this.favorite = false;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public int getReviewsCount() { return reviewsCount; }
        public void setReviewsCount(int reviewsCount) { this.reviewsCount = reviewsCount; }
        public int getToursCount() { return toursCount; }
        public void setToursCount(int toursCount) { this.toursCount = toursCount; }
        public int getClientsCount() { return clientsCount; }
        public void setClientsCount(int clientsCount) { this.clientsCount = clientsCount; }
        public double getPriceFrom() { return priceFrom; }
        public void setPriceFrom(double priceFrom) { this.priceFrom = priceFrom; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isFavorite() { return favorite; }
        public void setFavorite(boolean favorite) { this.favorite = favorite; }
    }

    public static class Tour {
        private int id;
        private String name, company, date, time, status;
        private double payment;
        private int participants;
        
        // Campos adicionales para catálogo de tours
        private String description;
        private String durationLabel; // e.g., "4 horas", "Full Day", "2D/1N"
        private double rating;
        private String languages;
        private String groupSizeLabel;
        private String imageUrl; // URL o URI de imagen para catálogo

        // Constructor para tours de guías (existente)
        public Tour() {}

        // Constructor para catálogo de tours
        public Tour(String name, String description, double price, String durationLabel, 
                   double rating, String languages, String groupSizeLabel) {
            this.name = name;
            this.description = description;
            this.payment = price;
            this.durationLabel = durationLabel;
            this.rating = rating;
            this.languages = languages;
            this.groupSizeLabel = groupSizeLabel;
        }

        // Getters y setters existentes
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getPayment() { return payment; }
        public void setPayment(double payment) { this.payment = payment; }
        public int getParticipants() { return participants; }
        public void setParticipants(int participants) { this.participants = participants; }
        
        // Getters y setters para campos de catálogo
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDurationLabel() { return durationLabel; }
        public void setDurationLabel(String durationLabel) { this.durationLabel = durationLabel; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public String getLanguages() { return languages; }
        public void setLanguages(String languages) { this.languages = languages; }
        public String getGroupSizeLabel() { return groupSizeLabel; }
        public void setGroupSizeLabel(String groupSizeLabel) { this.groupSizeLabel = groupSizeLabel; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public static class Offer {
        private int id;
        private String tourName, company, date, time, status;
        private double payment;
        private int participants;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTourName() { return tourName; }
        public void setTourName(String tourName) { this.tourName = tourName; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getPayment() { return payment; }
        public void setPayment(double payment) { this.payment = payment; }
        public int getParticipants() { return participants; }
        public void setParticipants(int participants) { this.participants = participants; }
    }

    public static class Reservation {
        private int id;
        private String tourName, company, date, time, status, qrCode;
        private double price;
        private int people;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getTourName() { return tourName; }
        public void setTourName(String tourName) { this.tourName = tourName; }
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public int getPeople() { return people; }
        public void setPeople(int people) { this.people = people; }
        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    }

    // Modelo para notificaciones (para clientes)
    public static class Notification {
        private int id;
        private String type;
        private String title;
        private String message;
        private String timestamp;
        private boolean isRead;

        public Notification() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public boolean isRead() { return isRead; }
        public void setRead(boolean read) { isRead = read; }
    }
}

