package com.example.droidtour.superadmin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.droidtour.LoginActivity;
import com.example.droidtour.R;
import com.example.droidtour.adapters.UsersAdapter;
import com.example.droidtour.models.User;
import com.example.droidtour.ui.UserProfileBottomSheet;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
    private SwipeRefreshLayout swipeRefresh;
    private CircularProgressIndicator loadingIndicator;

    private UsersAdapter usersAdapter;
    private List<User> userList;
    private FirebaseFirestore db;

    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddAdmin;

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
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

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
        showLoading();
        loadUsersFromFirestore();

        // Configurar listeners
        setupListeners();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rv_users);
        tabUserTypes = findViewById(R.id.tab_user_types);
        etSearch = findViewById(R.id.et_search);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // fab
        fabAddAdmin = findViewById(R.id.fab_add_admin);
        fabAddAdmin.setVisibility(View.GONE);

        userList = new ArrayList<>();

        // Configurar comportamiento del SwipeRefreshLayout
        swipeRefresh.setOnRefreshListener(() -> {
            loadUsersFromFirestore();
        });
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

                // Mostrar FAB solo en la pestaña de administradores
                if (tab.getPosition() == 1) {
                    fabAddAdmin.setVisibility(View.VISIBLE);
                } else {
                    fabAddAdmin.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Listener para el FAB
        fabAddAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(SuperadminUsersActivity.this, AdminRegistrationActivity.class);
            startActivityForResult(intent, 1001);
        });
    }

    private void showLoading() {
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.VISIBLE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
    }

    private void hideLoading() {
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
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
        if (!swipeRefresh.isRefreshing()) showLoading();

        db.collection(FirestoreManager.COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = null;
                            try {
                                user = document.toObject(User.class);
                            } catch (RuntimeException e) {
                                Log.w(TAG, "Error mapeando documento " + document.getId() + ": " + e.getMessage());
                                user = new User();
                            }

                            if (user == null) {
                                user = new User();
                            }

                            // Establecer userId desde el ID del documento
                            user.setUserId(document.getId());

                            Map<String, Object> data = document.getData();

                            // Mapear campos básicos
                            if (data.get("email") != null) {
                                user.setEmail(String.valueOf(data.get("email")));
                            }

                            if (data.get("userType") != null) {
                                user.setUserType(String.valueOf(data.get("userType")));
                            }

                            if (data.get("status") != null) {
                                user.setStatus(String.valueOf(data.get("status")));
                            }

                            // Mapear personalData si existe
                            if (data.get("personalData") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> pdMap = (Map<String, Object>) data.get("personalData");
                                User.PersonalData pd = new User.PersonalData();

                                if (pdMap.get("firstName") != null)
                                    pd.setFirstName(String.valueOf(pdMap.get("firstName")));
                                if (pdMap.get("lastName") != null)
                                    pd.setLastName(String.valueOf(pdMap.get("lastName")));
                                if (pdMap.get("fullName") != null)
                                    pd.setFullName(String.valueOf(pdMap.get("fullName")));
                                if (pdMap.get("documentType") != null)
                                    pd.setDocumentType(String.valueOf(pdMap.get("documentType")));
                                if (pdMap.get("documentNumber") != null)
                                    pd.setDocumentNumber(String.valueOf(pdMap.get("documentNumber")));
                                if (pdMap.get("dateOfBirth") != null)
                                    pd.setDateOfBirth(String.valueOf(pdMap.get("dateOfBirth")));
                                if (pdMap.get("phoneNumber") != null)
                                    pd.setPhoneNumber(String.valueOf(pdMap.get("phoneNumber")));
                                if (pdMap.get("profileImageUrl") != null)
                                    pd.setProfileImageUrl(String.valueOf(pdMap.get("profileImageUrl")));

                                user.setPersonalData(pd);
                            }

                            // Fallback para campos legacy (por si no existe personalData)
                            if (user.getFullName() == null || user.getFullName().isEmpty()) {
                                Object displayName = data.get("displayName");
                                if (displayName != null) {
                                    user.setFullName(String.valueOf(displayName));
                                } else {
                                    // Construir desde firstName y lastName si existen
                                    Object fn = data.get("firstName");
                                    Object ln = data.get("lastName");
                                    if (fn != null || ln != null) {
                                        String firstName = fn != null ? String.valueOf(fn) : "";
                                        String lastName = ln != null ? String.valueOf(ln) : "";
                                        String combined = (firstName + " " + lastName).trim();
                                        if (!combined.isEmpty()) {
                                            user.setFullName(combined);
                                        }
                                    }
                                }
                            }

                            // Mapear companyId si es ADMIN o COMPANY_ADMIN
                            if (data.get("companyId") != null) {
                                user.setCompanyId(String.valueOf(data.get("companyId")));
                            }

                            // Timestamps
                            if (data.get("createdAt") instanceof Date) {
                                user.setCreatedAt((Date) data.get("createdAt"));
                            }

                            // Añadir usuario a la lista
                            userList.add(user);
                        }

                        usersAdapter.updateList(userList);
                        checkEmptyState();
                        Toast.makeText(this, "Usuarios cargados: " + userList.size(), Toast.LENGTH_SHORT).show();

                        hideLoading();

                        // Cargar estado de los guías desde user_roles
                        loadGuideStatusFromUserRoles();

                    } else {
                        Log.e(TAG, "Error cargando usuarios", task.getException());
                        Toast.makeText(this, "Error cargando usuarios: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        hideLoading();
                    }
                });
    }

    /**
     * Carga el estado de los guías desde la colección user_roles
     */
    private void loadGuideStatusFromUserRoles() {
        List<User> guidesToCheck = new ArrayList<>();
        for (User u : userList) {
            if ("GUIDE".equals(u.getUserType())) {
                guidesToCheck.add(u);
            }
        }

        if (guidesToCheck.isEmpty()) return;

        final int total = guidesToCheck.size();
        final int[] completed = {0};

        for (User guide : guidesToCheck) {
            String uid = guide.getUserId();
            if (uid == null || uid.isEmpty()) {
                completed[0]++;
                continue;
            }

            db.collection(FirestoreManager.COLLECTION_USER_ROLES).document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            // Manejar diferentes estructuras de user_roles
                            if (doc.contains("guide")) {
                                Object guideObj = doc.get("guide");
                                if (guideObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> guideMap = (Map<String, Object>) guideObj;
                                    Object statusObj = guideMap.get("status");
                                    if (statusObj != null) {
                                        guide.setStatus(String.valueOf(statusObj));
                                    }
                                }
                            } else if (doc.contains("status")) {
                                // Estructura directa con status
                                Object statusObj = doc.get("status");
                                if (statusObj != null) {
                                    guide.setStatus(String.valueOf(statusObj));
                                }
                            } else if (doc.contains("roles")) {
                                Object rolesObj = doc.get("roles");
                                if (rolesObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> rolesMap = (Map<String, Object>) rolesObj;
                                    Object guideRole = rolesMap.get("guide");
                                    if (guideRole instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> guideMap = (Map<String, Object>) guideRole;
                                        Object statusObj = guideMap.get("status");
                                        if (statusObj != null) {
                                            guide.setStatus(String.valueOf(statusObj));
                                        }
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
                            // Todas las peticiones completadas
                            usersAdapter.updateList(userList);
                        }
                    });
        }
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
        // Obtener phoneNumber desde personalData
        String phoneNumber = null;
        if (user.getPersonalData() != null) {
            phoneNumber = user.getPersonalData().getPhoneNumber();
        }

        // Obtener profileImageUrl desde personalData
        String profileImageUrl = null;
        if (user.getPersonalData() != null) {
            profileImageUrl = user.getPersonalData().getProfileImageUrl();
        }

        long createdAtMillis = -1;
        if (user.getCreatedAt() != null) {
            createdAtMillis = user.getCreatedAt().getTime();
        }

        UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
                user.getUserId() != null ? user.getUserId() : "",
                user.getFullName(),
                user.getEmail(),
                phoneNumber,
                profileImageUrl,
                user.getUserType(),
                createdAtMillis,
                user.getStatus()
        );

        sheet.show(getSupportFragmentManager(), "user_profile_sheet");
    }

    @Override
    public void onEditUser(String userId) {
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
                    performUserStatusUpdate(user, isActive, isGuide && isActive);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
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
     * Actualiza el estado del usuario en Firestore
     */
    private void performUserStatusUpdate(User user, boolean isActive, boolean approveGuide) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            int idx = userList.indexOf(user);
            if (idx >= 0) usersAdapter.notifyItemChanged(idx);
            return;
        }

        // Actualizar el campo status en la colección users
        String newStatus = isActive ? "active" : "inactive";
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        db.collection(FirestoreManager.COLLECTION_USERS).document(user.getUserId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    user.setStatus(newStatus);

                    if (approveGuide) {
                        // Actualizar user_roles para aprobar guía
                        FirestoreManager firestoreManager = FirestoreManager.getInstance();
                        firestoreManager.saveUserRole(user.getUserId(), "GUIDE", "active", null, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                user.setStatus("active");
                                Toast.makeText(SuperadminUsersActivity.this, "Usuario activado y guía aprobado", Toast.LENGTH_SHORT).show();
                                int idx = userList.indexOf(user);
                                if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(SuperadminUsersActivity.this, "Usuario activado pero no se pudo actualizar user_roles: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                int idx = userList.indexOf(user);
                                if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                            }
                        });
                    } else {
                        if (!isActive && "GUIDE".equals(user.getUserType())) {
                            // Desactivar guía en user_roles
                            FirestoreManager firestoreManager = FirestoreManager.getInstance();
                            firestoreManager.saveUserRole(user.getUserId(), "GUIDE", "inactive", null, new FirestoreManager.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    user.setStatus("inactive");
                                    Toast.makeText(SuperadminUsersActivity.this, "Usuario desactivado", Toast.LENGTH_SHORT).show();
                                    int idx = userList.indexOf(user);
                                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(SuperadminUsersActivity.this, "Usuario desactivado pero no se pudo actualizar user_roles: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    int idx = userList.indexOf(user);
                                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                                }
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
                    int idx = userList.indexOf(user);
                    if (idx >= 0) usersAdapter.notifyItemChanged(idx);
                });
    }

    private void deleteUser(User user) {
        if (user.getUserId() == null) {
            Toast.makeText(this, "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection(FirestoreManager.COLLECTION_USERS).document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                    loadUsersFromFirestore();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            loadUsersFromFirestore();
            Toast.makeText(this, "Nuevo administrador registrado", Toast.LENGTH_SHORT).show();
        }
    }
}