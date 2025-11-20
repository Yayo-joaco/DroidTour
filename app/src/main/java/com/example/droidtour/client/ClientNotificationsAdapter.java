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
import com.example.droidtour.R;

// Cambios: renombrada la clase a ClientNotificationsAdapter, paquete corregido a .client,
// interfaz simplificada a OnNotificationActionListener (un solo método) y añadido markAsRead(int).
public class ClientNotificationsAdapter extends RecyclerView.Adapter<ClientNotificationsAdapter.ViewHolder> {

    // Interface para manejar acciones (click, menu, etc.)
    public interface OnNotificationActionListener {
        void onNotificationAction(int position, String action);
    }

    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    // Constructor
    public ClientNotificationsAdapter(OnNotificationActionListener listener) {
        this.listener = listener;
        this.notifications = new ArrayList<>();
        loadDummyData(); // Datos de ejemplo
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

        // Establecer datos
        holder.tvTitle.setText(notification.title);
        holder.tvMessage.setText(notification.message);
        holder.tvTime.setText(notification.time);
        holder.tvCategory.setText(notification.category);

        // Mostrar/ocultar indicador de no leído
        holder.unreadIndicator.setVisibility(
                notification.isUnread ? View.VISIBLE : View.INVISIBLE
        );

        // Establecer ícono
        holder.ivIcon.setImageResource(notification.iconRes);

        // Click en la tarjeta
        holder.card.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationAction(position, "click");
                // Marcar como leída localmente
                notification.isUnread = false;
                notifyItemChanged(position);
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

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    // Método para marcar todas como leídas
    public void markAllAsRead() {
        for (Notification notification : notifications) {
            notification.isUnread = false;
        }
        notifyDataSetChanged();
    }

    // Método para marcar una notificación como leída
    public void markAsRead(int position) {
        if (position >= 0 && position < notifications.size()) {
            Notification n = notifications.get(position);
            n.isUnread = false;
            notifyItemChanged(position);
        }
    }

    // Método para eliminar una notificación
    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Datos de ejemplo
    private void loadDummyData() {
        notifications.add(new Notification(
                "Reserva confirmada",
                "Tu reserva para el tour 'Machu Picchu' ha sido confirmada",
                "2h",
                "Reservas",
                R.drawable.ic_book_24,
                true
        ));

        notifications.add(new Notification(
                "Nuevo mensaje",
                "Juan Pérez te ha enviado un mensaje",
                "5h",
                "Mensajes",
                R.drawable.ic_chat,
                true
        ));

        notifications.add(new Notification(
                "Recordatorio de pago",
                "Tu pago pendiente vence en 2 días",
                "1d",
                "Pagos",
                R.drawable.ic_payment,
                false
        ));

        notifications.add(new Notification(
                "Nueva oferta",
                "Descuento del 20% en tours a Cusco",
                "2d",
                "Promociones",
                R.drawable.ic_mountain,
                false
        ));

        notifications.add(new Notification(
                "Califica tu experiencia",
                "¿Cómo estuvo tu tour a Lima?",
                "3d",
                "Valoraciones",
                R.drawable.ic_star,
                false
        ));
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

    // Clase modelo para las notificaciones
    static class Notification {
        String title;
        String message;
        String time;
        String category;
        int iconRes;
        boolean isUnread;

        Notification(String title, String message, String time,
                     String category, int iconRes, boolean isUnread) {
            this.title = title;
            this.message = message;
            this.time = time;
            this.category = category;
            this.iconRes = iconRes;
            this.isUnread = isUnread;
        }
    }
}