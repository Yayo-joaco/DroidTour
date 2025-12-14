package com.example.droidtour.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.droidtour.R;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Notification;

import java.util.Date;

public class NotificationHelper {
    
    private static final String CHANNEL_GUIDE_OFFERS = "guide_offers";
    private static final String CHANNEL_GUIDE_TOURS = "guide_tours";
    private static final String CHANNEL_CLIENT_RESERVATIONS = "client_reservations";
    private static final String CHANNEL_CLIENT_REMINDERS = "client_reminders";

    private Context context;
    private NotificationManager notificationManager;
    private FirestoreManager firestoreManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) 
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.firestoreManager = FirestoreManager.getInstance();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal para ofertas de tours (Guía)
            NotificationChannel channelOffers = new NotificationChannel(
                    CHANNEL_GUIDE_OFFERS,
                    "Ofertas de Tours",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelOffers.setDescription("Notificaciones de nuevas ofertas de tours");
            channelOffers.enableVibration(true);
            notificationManager.createNotificationChannel(channelOffers);

            // Canal para tours del guía
            NotificationChannel channelTours = new NotificationChannel(
                    CHANNEL_GUIDE_TOURS,
                    "Mis Tours",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channelTours.setDescription("Recordatorios de tours programados");
            notificationManager.createNotificationChannel(channelTours);

            // Canal para reservas del cliente
            NotificationChannel channelReservations = new NotificationChannel(
                    CHANNEL_CLIENT_RESERVATIONS,
                    "Mis Reservas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channelReservations.setDescription("Confirmaciones y actualizaciones de reservas");
            channelReservations.enableVibration(true);
            notificationManager.createNotificationChannel(channelReservations);

            // Canal para recordatorios del cliente
            NotificationChannel channelReminders = new NotificationChannel(
                    CHANNEL_CLIENT_REMINDERS,
                    "Recordatorios",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channelReminders.setDescription("Recordatorios de tours próximos");
            notificationManager.createNotificationChannel(channelReminders);
        }
    }

    // ==================== GUIDE NOTIFICATIONS ====================
    
    public void sendNewOfferNotification(String tourName, String company, double payment) {
        String title = "🎯 Nueva Oferta de Tour";
        String message = company + " te ofrece " + tourName + " por S/. " + payment;
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GUIDE_OFFERS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    public void sendTourReminderNotification(String tourName, String time) {
        String title = "🚌 Recordatorio de Tour";
        String message = tourName + " comienza a las " + time + ". ¡No olvides prepararte!";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GUIDE_TOURS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    public void sendTourStartingSoonNotification(String tourName, int minutesRemaining) {
        String title = "⏰ Tu tour está por comenzar";
        String message = tourName + " comienza en " + minutesRemaining + " minutos";
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GUIDE_TOURS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    // ==================== CLIENT NOTIFICATIONS ====================
    
    public void sendReservationConfirmedNotification(String userId, String tourName, String date, String qrCode) {
        String title = "Reserva Confirmada";
        String message = "Tu reserva para " + tourName + " el " + date + " ha sido confirmada. Código: " + qrCode;
        
        // Guardar en Firestore
        if (userId != null) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("RESERVATION_CONFIRMED");
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setCreatedAt(new Date());
            notification.setIsRead(false);
            
            /*firestoreManager.createNotification(notification, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Notificación guardada en Firestore
                }

                @Override
                public void onFailure(Exception e) {
                    // Error al guardar, pero mostrar notificación de todos modos
                }
            });

             */
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CLIENT_RESERVATIONS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    public void sendTourReminderForClient(String userId, String tourName, String date, String time) {
        String title = "Recordatorio de Tour";
        String message = "No olvides tu tour " + tourName + " el " + date + " a las " + time;
        
        // Guardar en Firestore
        if (userId != null) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("TOUR_REMINDER");
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setCreatedAt(new Date());
            notification.setIsRead(false);
            
            /*firestoreManager.createNotification(notification, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Notificación guardada en Firestore
                }

                @Override
                public void onFailure(Exception e) {
                    // Error al guardar, pero mostrar notificación de todos modos
                }
            });

             */
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CLIENT_REMINDERS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    public void sendTourCompletedNotification(String userId, String tourName) {
        String title = "Tour Completado";
        String message = "¡Esperamos que hayas disfrutado " + tourName + "! ¿Quieres calificarlo?";
        
        // Guardar en Firestore
        if (userId != null) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("TOUR_COMPLETED");
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setCreatedAt(new Date());
            notification.setIsRead(false);
            
            /*firestoreManager.createNotification(notification, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Notificación guardada en Firestore
                }

                @Override
                public void onFailure(Exception e) {
                    // Error al guardar, pero mostrar notificación de todos modos
                }
            });

             */
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CLIENT_RESERVATIONS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    public void sendPaymentConfirmedNotification(String userId, String tourName, double amount) {
        String title = "Pago Confirmado";
        String message = "Tu pago de S/. " + amount + " para " + tourName + " ha sido procesado exitosamente";
        
        // Guardar en Firestore
        if (userId != null) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("PAYMENT_CONFIRMED");
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setCreatedAt(new Date());
            notification.setIsRead(false);
            
            /*firestoreManager.createNotification(notification, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    // Notificación guardada en Firestore
                }

                @Override
                public void onFailure(Exception e) {
                    // Error al guardar, pero mostrar notificación de todos modos
                }
            });

             */
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_CLIENT_RESERVATIONS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        
        notificationManager.notify(generateNotificationId(), builder.build());
    }

    // ==================== UTILITIES ====================
    
    private int generateNotificationId() {
        return (int) System.currentTimeMillis();
    }

    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }

    // Método getCurrentTimestamp eliminado - ya no se usa
}

