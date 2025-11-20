package com.example.droidtour.client;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.card.MaterialCardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import com.example.droidtour.R;
import com.example.droidtour.models.Notification;

// Cambios: renombrada la clase a ClientNotificationsAdapter, paquete corregido a .client,
// interfaz simplificada a OnNotificationActionListener (un solo método) y añadido markAsRead(int).
public class ClientNotificationsAdapter extends RecyclerView.Adapter<ClientNotificationsAdapter.ViewHolder> {

    // Interface para manejar acciones (click, menu, etc.)
    public interface OnNotificationActionListener {
        void onNotificationAction(int position, String action);
    }

    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    // Constructor que recibe la lista de notificaciones
    public ClientNotificationsAdapter(List<Notification> notifications, OnNotificationActionListener listener) {
        this.listener = listener;
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }
    
    // Método para actualizar los datos
    public void updateData(List<Notification> newNotifications) {
        this.notifications = newNotifications != null ? newNotifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // 🔥 Usar datos reales de Firebase
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(formatTimeAgo(notification.getCreatedAt()));
        holder.tvCategory.setText(getCategoryName(notification.getType()));

        // Mostrar/ocultar indicador de no leído
        Boolean isRead = notification.getIsRead();
        holder.unreadIndicator.setVisibility(
                (isRead == null || !isRead) ? View.VISIBLE : View.INVISIBLE
        );

        // Establecer ícono según tipo de notificación
        holder.ivIcon.setImageResource(getIconForType(notification.getType()));

        // Click en la tarjeta
        holder.card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationAction(position, "click");
            }
        });

        // Click en el menú (mostrar PopupMenu)
        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), holder.menuButton);
            popup.getMenuInflater().inflate(R.menu.menu_notification_item, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_action_mark_read) {
                    if (listener != null) listener.onNotificationAction(position, "mark_read");
                    return true;
                } else if (id == R.id.menu_action_delete) {
                    if (listener != null) listener.onNotificationAction(position, "delete");
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }
    
    /**
     * Obtener ícono según el tipo de notificación
     */
    private int getIconForType(String type) {
        if (type == null) return R.drawable.ic_book_24;
        
        switch (type) {
            case "RESERVATION_CONFIRMED":
                return R.drawable.ic_book_24;
            case "QR_SENT":
                return R.drawable.ic_qr_code;
            case "PAYMENT_CHARGED":
            case "PAYMENT_FAILED":
                return R.drawable.ic_payment;
            case "TOUR_REMINDER":
                return R.drawable.ic_mountain;
            case "TOUR_COMPLETED":
                return R.drawable.ic_check_circle;
            case "REVIEW_REQUEST":
                return R.drawable.ic_star;
            default:
                return R.drawable.ic_book_24;
        }
    }
    
    /**
     * Obtener nombre de categoría según tipo
     */
    private String getCategoryName(String type) {
        if (type == null) return "General";
        
        switch (type) {
            case "RESERVATION_CONFIRMED":
                return "Reservas";
            case "QR_SENT":
                return "QR Codes";
            case "PAYMENT_CHARGED":
            case "PAYMENT_FAILED":
                return "Pagos";
            case "TOUR_REMINDER":
            case "TOUR_COMPLETED":
                return "Tours";
            case "REVIEW_REQUEST":
                return "Valoraciones";
            default:
                return "General";
        }
    }
    
    /**
     * Formatear tiempo relativo (hace 2h, hace 1d, etc.)
     */
    private String formatTimeAgo(Date date) {
        if (date == null) return "Ahora";
        
        long timeInMillis = date.getTime();
        long now = System.currentTimeMillis();
        long diff = now - timeInMillis;
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Ahora";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "m";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + "h";
        } else if (diff < TimeUnit.DAYS.toMillis(7)) {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + "d";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", new Locale("es", "ES"));
            return sdf.format(date);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    // Método para eliminar una notificación
    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    // ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        View unreadIndicator;
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime, tvCategory;
        ImageButton menuButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_notification);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvMessage = itemView.findViewById(R.id.tv_notification_message);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            tvCategory = itemView.findViewById(R.id.tv_notification_category);
            menuButton = itemView.findViewById(R.id.menu_button);
        }
    }
}