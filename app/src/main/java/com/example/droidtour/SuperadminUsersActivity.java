package com.example.droidtour;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.adapters.UsersAdapter;
import com.example.droidtour.models.User;
import com.example.droidtour.ui.UserProfileBottomSheet;
import com.example.droidtour.utils.PreferencesManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SuperadminUsersActivity extends AppCompatActivity implements UsersAdapter.OnUserClickListener, UserProfileBottomSheet.OnUserProfileActionListener {

    private static final String TAG = "SuperadminUsersAct";

    private PreferencesManager prefsManager;
    private RecyclerView rvUsers;
    private TabLayout tabUserTypes;
    private com.google.android.material.textfield.TextInputEditText etSearch;
    private View layoutEmptyState;

    private UsersAdapter usersAdapter;
    private List<User> userList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Validaciones de seguridad
        prefsManager = new PreferencesManager(this);
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("SUPERADMIN") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_superadmin_users);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Configurar toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Gestión de Usuarios");
        }

        // Inicializar vistas
        initViews();

        // Configurar RecyclerView
        setupRecyclerView();

        // Cargar usuarios
        loadUsersFromFirestore();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rv_users);
        tabUserTypes = findViewById(R.id.tab_user_types);
        etSearch = findViewById(R.id.et_search);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        userList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        usersAdapter = new UsersAdapter(userList, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(usersAdapter);
    }

    private void setupListeners() {
        // Listener para búsqueda
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usersAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Listener para tabs de filtro
        tabUserTypes.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String filterType = getFilterTypeForTab(tab.getPosition());
                usersAdapter.setFilter(filterType);
                checkEmptyState();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private String getFilterTypeForTab(int position) {
        switch (position) {
            case 0: return "ALL";
            case 1: return "ADMIN";
            case 2: return "GUIDE";
            case 3: return "CLIENT";
            default: return "ALL";
        }
    }

    private void loadUsersFromFirestore() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Intentamos mapear al modelo User, pero normalizamos campos legacy/alternativos
                            User user = document.toObject(User.class);

                            // Always ensure user is not null
                            if (user == null) {
                                user = new User();
                            }

                            // Document ID -> userId si falta
                            if (user.getUserId() == null || user.getUserId().isEmpty()) {
                                user.setUserId(document.getId());
                            }

                            Map<String, Object> data = document.getData();

                            // Email fallback
                            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                                Object e = data.get("email");
                                if (e != null) user.setEmail(String.valueOf(e));
                            }

                            // Full name: prefer fullName, luego displayName (legacy), luego firstName+lastName
                            if (user.getFullName() == null || user.getFullName().isEmpty()) {
                                Object dn = data.get("displayName");
                                if (dn != null) {
                                    user.setFullName(String.valueOf(dn));
                                } else if (user.getFirstName() != null || user.getLastName() != null) {
                                    String fn = (user.getFirstName() != null ? user.getFirstName() : "");
                                    String ln = (user.getLastName() != null ? user.getLastName() : "");
                                    String combined = (fn + " " + ln).trim();
                                    if (!combined.isEmpty()) user.setFullName(combined);
                                }
                            }

                            // Profile image: try profileImageUrl, photoURL (legacy), photoUrl
                            if (user.getProfileImageUrl() == null || user.getProfileImageUrl().isEmpty()) {
                                Object p1 = data.get("profileImageUrl");
                                Object p2 = data.get("photoURL");
                                Object p3 = data.get("photoUrl");
                                if (p1 != null) user.setProfileImageUrl(String.valueOf(p1));
                                else if (p2 != null) user.setProfileImageUrl(String.valueOf(p2));
                                else if (p3 != null) user.setProfileImageUrl(String.valueOf(p3));
                            }

                            // userType fallback
                            if (user.getUserType() == null || user.getUserType().isEmpty()) {
                                Object ut = data.get("userType");
                                if (ut != null) user.setUserType(String.valueOf(ut));
                            }

                            // isActive boolean: try isActive or infer from status field
                            if (user.getActive() == null) {
                                Object activeObj = data.get("isActive");
                                if (activeObj instanceof Boolean) {
                                    user.setActive((Boolean) activeObj);
                                } else {
                                    Object statusObj = data.get("status");
                                    if (statusObj != null) {
                                        String statusStr = String.valueOf(statusObj);
                                        user.setActive("active".equalsIgnoreCase(statusStr));
                                    } else {
                                        user.setActive(true); // default safe assumption
                                    }
                                }
                            }

                            // Añadir user a la lista
                            userList.add(user);
                        }
                        usersAdapter.updateList(userList);
                        checkEmptyState();
                        Toast.makeText(this, "Usuarios cargados: " + userList.size(), Toast.LENGTH_SHORT).show();

                        // Ahora, para cada GUIDE, obtener su estado desde la colección user_roles
                        List<User> guidesToCheck = new ArrayList<>();
                        for (User u : userList) {
                            if ("GUIDE".equals(u.getUserType())) {
                                guidesToCheck.add(u);
                            }
                        }

                        if (!guidesToCheck.isEmpty()) {
                            final int total = guidesToCheck.size();
                            final int[] completed = {0};

                            for (User guide : guidesToCheck) {
                                String uid = guide.getUserId();
                                if (uid == null || uid.isEmpty()) {
                                    completed[0]++;
                                    continue;
                                }

                                db.collection("user_roles").document(uid).get()
                                        .addOnSuccessListener(doc -> {
                                            if (doc != null && doc.exists()) {
                                                // Manejar varias estructuras: doc puede tener directamente 'guide' o 'guide' dentro de 'roles'
                                                if (doc.contains("guide")) {
                                                    Object guideObj = doc.get("guide");
                                                    if (guideObj instanceof Map) {
                                                        Map<String, Object> guideMap = (Map<String, Object>) guideObj;
                                                        Object statusObj = guideMap.get("status");
                                                        if (statusObj != null) {
                                                            guide.setStatus(String.valueOf(statusObj));
                                                        }
                                                    }
                                                } else if (doc.contains("roles")) {
                                                    Object rolesObj = doc.get("roles");
                                                    if (rolesObj instanceof Map) {
                                                        Map<String, Object> rolesMap = (Map<String, Object>) rolesObj;
                                                        Object guideRole = rolesMap.get("guide");
                                                        if (guideRole instanceof Map) {
                                                            Map<String, Object> guideMap = (Map<String, Object>) guideRole;
                                                            Object statusObj = guideMap.get("status");
                                                            if (statusObj != null) guide.setStatus(String.valueOf(statusObj));
                                                        }
                                                    }
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w(TAG, "No se pudo obtener user_roles para " + uid + ": " + e.getMessage());
                                        })
                                        .addOnCompleteListener(roleTask -> {
                                            completed[0]++;
                                            if (completed[0] >= total) {
                                                // Todas las peticiones completadas - actualizar adapter
                                                usersAdapter.updateList(userList);
                                            }
                                        });
                            }
                        }
                    } else {
                        Log.e(TAG, "Error cargando usuarios", task.getException());
                        Toast.makeText(this, "Error cargando usuarios: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkEmptyState() {
        if (usersAdapter.getItemCount() == 0) {
            rvUsers.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvUsers.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    // Implementación de los listeners del adapter
    @Override
    public void onUserClick(User user) {
        // Mostrar bottom sheet con los datos del usuario
        long createdAtMillis = -1;
        if (user.getCreatedAt() != null) createdAtMillis = user.getCreatedAt().getTime();

        UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                user.getUserId() != null ? user.getUserId() : "",
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfileImageUrl(),
                user.getUserType(),
                createdAtMillis,
                user.getStatus()
        );

        sheet.show(getSupportFragmentManager(), "user_profile_sheet");
    }

    @Override
    public void onEditUser(String userId) {
        // Lanzar actividad de edición o mostrar formulario
        Toast.makeText(this, "Abrir edición para " + userId, Toast.LENGTH_SHORT).show();
        // TODO: implementar navegación a la pantalla de edición
    }

    @Override
    public void onSendMessageToUser(String userId) {
        Toast.makeText(this, "Enviar mensaje a " + userId, Toast.LENGTH_SHORT).show();
        // TODO: implementar chat/mensaje
    }

    @Override
    public void onUserEdit(User user) {
        Toast.makeText(this, "Editar: " + user.getFullName(), Toast.LENGTH_SHORT).show();
        // TODO: Implementar edición de usuario
    }

    @Override
    public void onUserDelete(User user) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Usuario")
                .setMessage("¿Estás seguro de eliminar a " + user.getFullName() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteUser(user))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onUserStatusChange(User user, boolean isActive) {
        // Mostrar diálogo de confirmación antes de realizar el cambio
        boolean isGuide = "GUIDE".equals(user.getUserType());
        String title = isActive ? "Activar usuario" : "Desactivar usuario";
        String message;
        if (isActive) {
            if (isGuide) {
                message = "Activar este usuario aprobará automáticamente la solicitud del guía. ¿Deseas activar y aprobarlo?";
            } else {
                message = "¿Deseas activar este usuario?";
            }
        } else {
            message = "¿Deseas desactivar este usuario?";
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(isActive ? "Activar" : "Desactivar", (dialog, which) -> {
                    // Ejecutar la actualización
                    performUserStatusUpdate(user, isActive, isGuide && isActive);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Revertir el cambio en la UI
                    int idx = userList.indexOf(user);
                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                })
                .setOnCancelListener(dialog -> {
                    int idx = userList.indexOf(user);
                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                })
                .show();
    }

    /**
     * Realiza la actualización en Firestore del campo isActive y opcionalmente actualiza user_roles/guide.status
     * @param user usuario
     * @param isActive nuevo estado
     * @param approveGuide si true, actualizará user_roles/{userId}.guide.status a "active" y marcará aprobado
     */
    private void performUserStatusUpdate(User user, boolean isActive, boolean approveGuide) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            int idx = userList.indexOf(user);
            if (idx >= 0) usersAdapter.notifyItemChanged(idx);
            return;
        }

        db.collection("users").document(user.getUserId())
                .update("isActive", isActive)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar objeto local
                    user.setActive(isActive);

                    if (approveGuide) {
                        // Actualizar user_roles para aprobar la solicitud del guía
                        Map<String, Object> roleData = new HashMap<>();
                        roleData.put("status", "active");
                        roleData.put("updatedAt", new Date());
                        roleData.put("activatedAt", new Date());

                        Map<String, Object> roleUpdate = new HashMap<>();
                        roleUpdate.put("guide", roleData);

                        db.collection("user_roles").document(user.getUserId())
                                .set(roleUpdate, SetOptions.merge())
                                .addOnSuccessListener(aVoid2 -> {
                                    user.setStatus("active");
                                    user.setGuideApproved(true);
                                    Toast.makeText(this, "Usuario activado y guía aprobado", Toast.LENGTH_SHORT).show();
                                    int idx = userList.indexOf(user);
                                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Usuario activado pero no se pudo actualizar user_roles: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    int idx = userList.indexOf(user);
                                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                                });

                    } else {
                        // Si no se aprueba, para guías que se desactivan dejamos el estado en user_roles como inactive (opcional)
                        if (!isActive && "GUIDE".equals(user.getUserType())) {
                            Map<String, Object> roleData = new HashMap<>();
                            roleData.put("status", "inactive");
                            roleData.put("updatedAt", new Date());

                            Map<String, Object> roleUpdate = new HashMap<>();
                            roleUpdate.put("guide", roleData);

                            db.collection("user_roles").document(user.getUserId())
                                    .set(roleUpdate, SetOptions.merge())
                                    .addOnSuccessListener(aVoid2 -> {
                                        user.setStatus("inactive");
                                        user.setGuideApproved(false);
                                        Toast.makeText(this, "Usuario desactivado", Toast.LENGTH_SHORT).show();
                                        int idx = userList.indexOf(user);
                                        if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Usuario desactivado pero no se pudo actualizar user_roles: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        int idx = userList.indexOf(user);
                                        if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                                    });
                        } else {
                            String statusMsg = isActive ? "Usuario activado" : "Usuario desactivado";
                            Toast.makeText(this, statusMsg, Toast.LENGTH_SHORT).show();
                            int idx = userList.indexOf(user);
                            if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error actualizando estado: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Revertir switch en UI
                    int idx = userList.indexOf(user);
                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                });
    }

    private void deleteUser(User user) {
        if (user.getUserId() == null) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                    loadUsersFromFirestore(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error eliminando usuario: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_general, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
