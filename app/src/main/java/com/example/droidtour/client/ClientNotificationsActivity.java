package com.example.droidtour.client;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.R;
import com.example.droidtour.database.DatabaseHelper;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientNotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private DatabaseHelper dbHelper;
    private NotificationsAdapter adapter;
    private List<DatabaseHelper.Notification> notificationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_notifications);

        dbHelper = new DatabaseHelper(this);

        setupToolbar();
        initializeViews();
        loadNotifications();
        // NO marcar todas como vistas automáticamente
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar badge cuando se regresa
        updateBadgeInParent();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notificaciones");
        }
    }

    private void initializeViews() {
        rvNotifications = findViewById(R.id.rv_notifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadNotifications() {
        // Limpiar notificaciones existentes y crear nuevas sin emojis
        clearAllNotifications();
        seedNotifications();
        
        notificationsList = dbHelper.getAllNotifications();

        adapter = new NotificationsAdapter(notificationsList);
        rvNotifications.setAdapter(adapter);
    }
    
    private void clearAllNotifications() {
        // Eliminar todas las notificaciones existentes para recrearlas sin emojis
        dbHelper.deleteAllNotifications();
    }

    private void seedNotifications() {
        Calendar cal = Calendar.getInstance();

        // Crear las notificaciones en orden inverso (más antiguas primero)
        // para que al ordenar por ID DESC, las más recientes aparezcan primero
        
        // Notificación 6: Pago Confirmado (más antigua) - VISTA
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, -3);
        cal.add(Calendar.HOUR, -2);
        long notifId = dbHelper.addNotification(
            "PAYMENT_CONFIRMED",
            "Pago Confirmado",
            "Tu pago de S/. 65.0 para Islas Ballestas y Paracas ha sido procesado exitosamente",
            getRelativeTime(cal.getTimeInMillis())
        );
        dbHelper.markNotificationAsRead((int) notifId);

        // Notificación 5: Reserva Confirmada (otra) - VISTA
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, -3);
        notifId = dbHelper.addNotification(
            "RESERVATION_CONFIRMED",
            "Reserva Confirmada",
            "Tu reserva para Islas Ballestas y Paracas el 1 Nov ha sido confirmada. Código: QR-2024-002",
            getRelativeTime(cal.getTimeInMillis())
        );
        dbHelper.markNotificationAsRead((int) notifId);

        // Notificación 4: Tour Completado - VISTA
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, -2);
        notifId = dbHelper.addNotification(
            "TOUR_COMPLETED",
            "Tour Completado",
            "¡Esperamos que hayas disfrutado City Tour Lima Centro Histórico! ¿Quieres calificarlo?",
            getRelativeTime(cal.getTimeInMillis())
        );
        dbHelper.markNotificationAsRead((int) notifId);

        // Notificación 3: Recordatorio de Tour - VISTA
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_MONTH, -1);
        notifId = dbHelper.addNotification(
            "TOUR_REMINDER",
            "Recordatorio de Tour",
            "No olvides tu tour Machu Picchu Full Day el 30 Oct a las 09:00 AM",
            getRelativeTime(cal.getTimeInMillis())
        );
        dbHelper.markNotificationAsRead((int) notifId);

        // Notificación 2: Pago Confirmado (más reciente) - NO VISTA
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR, -1);
        dbHelper.addNotification(
            "PAYMENT_CONFIRMED",
            "Pago Confirmado",
            "Tu pago de S/. 85.0 para City Tour Lima Centro Histórico ha sido procesado exitosamente",
            getRelativeTime(cal.getTimeInMillis())
        );

        // Notificación 1: Reserva Confirmada (más reciente) - NO VISTA
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.HOUR, -2);
        dbHelper.addNotification(
            "RESERVATION_CONFIRMED",
            "Reserva Confirmada",
            "Tu reserva para City Tour Lima Centro Histórico el 28 Oct ha sido confirmada. Código: QR-2024-001",
            getRelativeTime(cal.getTimeInMillis())
        );
    }

    private String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long minutes = diff / (1000 * 60);
        long hours = diff / (1000 * 60 * 60);
        long days = diff / (1000 * 60 * 60 * 24);
        
        if (minutes < 60) {
            return "Hace " + minutes + (minutes == 1 ? " minuto" : " minutos");
        } else if (hours < 24) {
            return "Hace " + hours + (hours == 1 ? " hora" : " horas");
        } else {
            return "Hace " + days + (days == 1 ? " día" : " días");
        }
    }

    private void markAllAsRead() {
        // NO marcar todas como vistas automáticamente
        // Solo se marcarán cuando el usuario las vea
        // dbHelper.markAllNotificationsAsRead();
        // Actualizar badge en la actividad padre
        updateBadgeInParent();
    }

    private void updateBadgeInParent() {
        // Enviar broadcast o usar resultado para actualizar badge
        Intent resultIntent = new Intent();
        setResult(RESULT_OK, resultIntent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Adapter para notificaciones
    private class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
        
        private List<DatabaseHelper.Notification> notifications;

        NotificationsAdapter(List<DatabaseHelper.Notification> notifications) {
            this.notifications = notifications;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DatabaseHelper.Notification notification = notifications.get(position);
            
            holder.tvTitle.setText(notification.getTitle());
            holder.tvMessage.setText(notification.getMessage());
            holder.tvTime.setText(notification.getTimestamp());
            
            // Configurar icono según tipo
            String icon = getIconForType(notification.getType());
            holder.tvIcon.setText(icon);
            
            // Configurar color de fondo del icono según tipo
            int iconBgDrawable = getIconBackgroundDrawable(notification.getType());
            holder.viewIconBackground.setBackgroundResource(iconBgDrawable);
            
            // Configurar estilo según si está visto o no
            if (!notification.isRead()) {
                // No vista: colores más fuertes
                holder.tvTitle.setTextColor(getColor(R.color.primary));
                holder.tvTitle.setAlpha(1.0f);
                holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                holder.tvMessage.setTextColor(getColor(R.color.black));
                holder.tvMessage.setAlpha(1.0f);
                holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(1.0f);
            } else {
                // Vista: colores más suaves
                holder.tvTitle.setTextColor(getColor(R.color.gray));
                holder.tvTitle.setAlpha(0.7f);
                holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
                holder.tvMessage.setTextColor(getColor(R.color.gray));
                holder.tvMessage.setAlpha(0.6f);
                holder.viewUnreadIndicator.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.85f);
            }
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        private String getIconForType(String type) {
            switch (type) {
                case "RESERVATION_CONFIRMED":
                    return "✅";
                case "PAYMENT_CONFIRMED":
                    return "💳";
                case "TOUR_REMINDER":
                    return "🎫";
                case "TOUR_COMPLETED":
                    return "🎉";
                default:
                    return "🔔";
            }
        }

        private int getIconBackgroundDrawable(String type) {
            switch (type) {
                case "RESERVATION_CONFIRMED":
                    return R.drawable.icon_background_green;
                case "PAYMENT_CONFIRMED":
                    return R.drawable.icon_background_blue;
                case "TOUR_REMINDER":
                    return R.drawable.icon_background_purple;
                case "TOUR_COMPLETED":
                    return R.drawable.icon_background_red;
                default:
                    return R.drawable.icon_background_blue;
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime, tvIcon;
            View viewIconBackground, viewUnreadIndicator;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_notification_title);
                tvMessage = itemView.findViewById(R.id.tv_notification_message);
                tvTime = itemView.findViewById(R.id.tv_notification_time);
                tvIcon = itemView.findViewById(R.id.tv_notification_icon);
                viewIconBackground = itemView.findViewById(R.id.view_icon_background);
                viewUnreadIndicator = itemView.findViewById(R.id.view_unread_indicator);
            }
        }
    }
}

