package com.example.droidtour.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.droidtour.R;
import com.example.droidtour.models.User;
import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> implements Filterable {

    private List<User> userList;
    private List<User> userListFull;
    private OnUserClickListener listener;
    private String currentFilter = "ALL";

    public interface OnUserClickListener {
        void onUserClick(User user);
        void onUserEdit(User user);
        void onUserDelete(User user);
        void onUserStatusChange(User user, boolean isActive);
    }

    public UsersAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.userListFull = new ArrayList<>(userList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateList(List<User> newList) {
        userList = new ArrayList<>(newList);
        userListFull = new ArrayList<>(newList);
        applyFilter(currentFilter); // Re-aplicar filtro actual
        notifyDataSetChanged();
    }

    public void setFilter(String filter) {
        this.currentFilter = filter;
        applyFilter(filter);
    }

    private void applyFilter(String filterType) {
        if (userListFull == null) return;

        List<User> filteredList = new ArrayList<>();

        switch (filterType) {
            case "ALL":
                filteredList.addAll(userListFull);
                break;
            case "ADMIN":
                for (User user : userListFull) {
                    if ("ADMIN".equals(user.getUserType()) || "SUPERADMIN".equals(user.getUserType())) {
                        filteredList.add(user);
                    }
                }
                break;
            case "GUIDE":
                for (User user : userListFull) {
                    if ("GUIDE".equals(user.getUserType())) {
                        filteredList.add(user);
                    }
                }
                break;
            case "CLIENT":
                for (User user : userListFull) {
                    if ("CLIENT".equals(user.getUserType())) {
                        filteredList.add(user);
                    }
                }
                break;
        }

        userList = filteredList;
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<User> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(userListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (User user : userListFull) {
                    String fullName = user.getFullName() != null ? user.getFullName() : "";
                    String email = user.getEmail() != null ? user.getEmail() : "";
                    String type = user.getUserType() != null ? user.getUserType() : "";
                    if (fullName.toLowerCase().contains(filterPattern) ||
                            email.toLowerCase().contains(filterPattern) ||
                            type.toLowerCase().contains(filterPattern)) {
                        filteredList.add(user);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            userList.clear();
            userList.addAll((List) results.values);
            applyFilter(currentFilter); // Aplicar filtro de tipo después de la búsqueda
        }
    };

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserName, tvUserEmail, tvUserType, tvAvatarInitial;
        private ImageView ivUserAvatar;
        private View viewStatusIndicator;
        private com.google.android.material.chip.Chip chipUserRole, chipRegisterDate;
        private com.google.android.material.switchmaterial.SwitchMaterial switchUserStatus;
        private com.google.android.material.chip.Chip chipStatus; // <-- nuevo

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
            tvUserType = itemView.findViewById(R.id.tv_user_type);
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            viewStatusIndicator = itemView.findViewById(R.id.view_status_indicator);
            chipUserRole = itemView.findViewById(R.id.chip_user_role);
            chipRegisterDate = itemView.findViewById(R.id.chip_register_date);
            switchUserStatus = itemView.findViewById(R.id.switch_user_status);
            chipStatus = itemView.findViewById(R.id.chip_status); // inicializar
        }

        public void bind(User user, OnUserClickListener listener) {
            // Nombre y email seguros
            tvUserName.setText(user.getFullName() != null ? user.getFullName() : "Sin nombre");
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "Sin email");
            tvUserType.setText(getUserTypeDisplayName(user.getUserType()));

            // Avatar e inicial
            setupUserAvatar(user);

            // Estado en línea (placeholder)
            setupOnlineStatus(user);

            // Chips de información
            setupInfoChips(user);

            // Mostrar chip de estado solo para GUIDEs con status pending
            if ("GUIDE".equals(user.getUserType()) && "pending".equalsIgnoreCase(user.getStatus())) {
                chipStatus.setVisibility(View.VISIBLE);
                chipStatus.setText("Pendiente");
                chipStatus.setChipBackgroundColorResource(R.color.notification_orange);
                chipStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.white));
            } else {
                chipStatus.setVisibility(View.GONE);
            }

            // Switch de estado
            setupStatusSwitch(user, listener);

            // Listeners de clic
            setupClickListeners(user, listener);
        }

        private void setupUserAvatar(User user) {
            String name = user.getFullName() != null ? user.getFullName() : "";
            String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            tvAvatarInitial.setText(initial);

            // Cargar foto si existe
            String photo = user.getPhotoUrl();
            if (photo != null && !photo.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(photo)
                        .into(ivUserAvatar);
                tvAvatarInitial.setVisibility(View.GONE);
            } else {
                tvAvatarInitial.setVisibility(View.VISIBLE);
                ivUserAvatar.setImageDrawable(null);
            }
        }

        private void setupOnlineStatus(User user) {
            // Placeholder: ocultamos el indicador
            viewStatusIndicator.setVisibility(View.GONE);
        }

        private void setupInfoChips(User user) {
            // Chip de rol
            chipUserRole.setText(getUserTypeDisplayName(user.getUserType()));

            // Chip de fecha - formatear fecha si existe
            if (user.getCreatedAt() != null) {
                String formattedDate = formatDate(user.getCreatedAt());
                chipRegisterDate.setText("Desde " + formattedDate);
                chipRegisterDate.setVisibility(View.VISIBLE);
            } else {
                chipRegisterDate.setVisibility(View.GONE);
            }

            // Color del chip según el tipo de usuario
            int chipColor = getChipColorForUserType(user.getUserType());
            chipUserRole.setChipBackgroundColorResource(chipColor);
        }

        private String formatDate(java.util.Date date) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return "Fecha inválida";
            }
        }

        private void setupStatusSwitch(User user, OnUserClickListener listener) {
            boolean isActive = user.getActive() != null ? user.getActive() : true;
            switchUserStatus.setChecked(isActive);

            switchUserStatus.setOnCheckedChangeListener(null);
            switchUserStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onUserStatusChange(user, isChecked);
                }
            });
        }

        private void setupClickListeners(User user, OnUserClickListener listener) {
            itemView.findViewById(R.id.btn_view_user).setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(user);
            });

            itemView.findViewById(R.id.btn_edit_user).setOnClickListener(v -> {
                if (listener != null) listener.onUserEdit(user);
            });

            itemView.findViewById(R.id.btn_delete_user).setOnClickListener(v -> {
                if (listener != null) listener.onUserDelete(user);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(user);
            });
        }

        private String getUserTypeDisplayName(String userType) {
            if (userType == null) return "Sin tipo";

            switch (userType) {
                case "SUPERADMIN": return "Super Admin";
                case "ADMIN": return "Administrador";
                case "GUIDE": return "Guía Turístico";
                case "CLIENT": return "Cliente";
                default: return userType;
            }
        }

        private int getChipColorForUserType(String userType) {
            if (userType == null) return R.color.default_chip_color;

            switch (userType) {
                case "SUPERADMIN": return R.color.superadmin_chip_color;
                case "ADMIN": return R.color.admin_chip_color;
                case "GUIDE": return R.color.guide_chip_color;
                case "CLIENT": return R.color.client_chip_color;
                default: return R.color.default_chip_color;
            }
        }


    }
}