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
import com.example.droidtour.firebase.FirebaseAuthManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.example.droidtour.models.Notification;
import com.google.android.material.appbar.MaterialToolbar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientNotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private FirebaseAuthManager authManager;
    private FirestoreManager firestoreManager;
    private String currentUserId;
    private NotificationsAdapter adapter;
    private List<Notification> notificationsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_notifications);

        authManager = FirebaseAuthManager.getInstance(this);
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = authManager.getCurrentUserId();
        
        // 🔥 TEMPORAL: Para testing sin login
        if (currentUserId == null) {
            currentUserId = "K35mJaSYbAT8YgFN5tq33ik6";
            android.widget.Toast.makeText(this, "⚠️ Modo testing: prueba@droidtour.com", android.widget.Toast.LENGTH_SHORT).show();
        }

        setupToolbar();
        initializeViews();
        loadNotificationsFromFirebase();
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

    private void loadNotificationsFromFirebase() {
        firestoreManager.getNotificationsByUser(currentUserId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                notificationsList.clear();
                notificationsList.addAll((List<Notification>) result);
                adapter = new NotificationsAdapter(notificationsList);
                rvNotifications.setAdapter(adapter);
            }
            
            @Override
            public void onFailure(Exception e) {
                android.widget.Toast.makeText(ClientNotificationsActivity.this, "Error cargando notificaciones", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
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
        
        private List<Notification> notifications;

        NotificationsAdapter(List<Notification> notifications) {
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
            Notification notification = notifications.get(position);
            
            holder.tvTitle.setText(notification.getTitle());
            holder.tvMessage.setText(notification.getMessage());
            holder.tvTime.setText(notification.getCreatedAt() != null ? getRelativeTime(notification.getCreatedAt().getTime()) : "Hace un momento");
            
            // Configurar icono según tipo
            String icon = getIconForType(notification.getType());
            holder.tvIcon.setText(icon);
            
            // Configurar color de fondo del icono según tipo
            int iconBgDrawable = getIconBackgroundDrawable(notification.getType());
            holder.viewIconBackground.setBackgroundResource(iconBgDrawable);
            
            // Configurar estilo según si está visto o no
            if (!notification.getRead()) {
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
            
            // Marcar como leída al hacer click
            holder.itemView.setOnClickListener(v -> {
                if (!notification.getRead()) {
                    firestoreManager.markNotificationAsRead(notification.getNotificationId(), new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            notification.setRead(true);
                            notifyItemChanged(position);
                        }
                        @Override
                        public void onFailure(Exception e) {}
                    });
                }
            });
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        private String getIconForType(String type) {
            if (type == null) return "🔔";
            switch (type) {
                case "RESERVATION_CONFIRMED":
                    return "✅";
                case "PAYMENT_CHARGED":
                case "PAYMENT_CONFIRMED":
                    return "💳";
                case "TOUR_REMINDER":
                case "QR_SENT":
                    return "🎫";
                case "TOUR_COMPLETED":
                    return "🎉";
                default:
                    return "🔔";
            }
        }

        private int getIconBackgroundDrawable(String type) {
            if (type == null) return R.drawable.icon_background_blue;
            switch (type) {
                case "RESERVATION_CONFIRMED":
                    return R.drawable.icon_background_green;
                case "PAYMENT_CHARGED":
                case "PAYMENT_CONFIRMED":
                    return R.drawable.icon_background_blue;
                case "TOUR_REMINDER":
                case "QR_SENT":
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

