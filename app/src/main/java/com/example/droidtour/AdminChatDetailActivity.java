package com.example.droidtour;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.droidtour.utils.ChatManagerRealtime;
import com.example.droidtour.utils.PresenceManager;
import com.example.droidtour.models.Message;
import com.example.droidtour.firebase.FirestoreManager;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminChatDetailActivity extends AppCompatActivity {
    
    private TextView tvClientName, tvClientStatus;
    private ImageView ivClientAvatar, ivCallClient, ivSendMessage, ivAttach;
    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private View rootLayout;
    private com.example.droidtour.utils.PreferencesManager prefsManager;
    private ChatManagerRealtime chatManager;
    private PresenceManager presenceManager;
    private FirestoreManager firestoreManager;
    
    private String clientId;
    private String clientName;
    private String companyId;
    private String conversationId;
    private String currentUserId;
    private String currentUserName;
    private AdminChatMessagesAdapter messagesAdapter;
    private List<AdminChatMessage> messagesList;
    
    // Constantes para selección de archivos
    private static final int REQUEST_CODE_CAMERA = 1001;
    private static final int REQUEST_CODE_GALLERY = 1002;
    private static final int REQUEST_CODE_DOCUMENT = 1003;
    private static final int REQUEST_CODE_PERMISSION_CAMERA = 2001;
    private static final int REQUEST_CODE_PERMISSION_STORAGE = 2002;
    
    // Variables para archivos adjuntos
    private Uri selectedFileUri;
    private String selectedFileType; // "IMAGE" o "PDF"
    private String selectedFileName;
    private File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar PreferencesManager PRIMERO
        prefsManager = new com.example.droidtour.utils.PreferencesManager(this);
        
        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }
        
        // Validar que el usuario sea ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("ADMIN") && !userType.equals("COMPANY_ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }
        
        setContentView(R.layout.activity_admin_chat_detail);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        
        getIntentData();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        setupInputBehavior();
        setupRecyclerView();
        
        // Inicializar managers
        chatManager = new ChatManagerRealtime();
        presenceManager = new PresenceManager();
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();
        currentUserName = prefsManager.getUserName();
        
        loadChatData();
        loadClientAvatar();
        initializeConversation();
    }
    
    private void getIntentData() {
        clientId = getIntent().getStringExtra("CLIENT_ID");
        clientName = getIntent().getStringExtra("CLIENT_NAME");
        companyId = getIntent().getStringExtra("COMPANY_ID");
        
        if (clientName == null) {
            clientName = "Cliente";
        }
        if (companyId == null) {
            // Intentar obtener companyId del usuario actual
            // Esto debería mejorarse obteniéndolo de Firestore
            companyId = "unknown_company";
        }
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
    }
    
    private void initializeViews() {
        tvClientName = findViewById(R.id.tv_client_name);
        tvClientStatus = findViewById(R.id.tv_client_status);
        
        ivClientAvatar = findViewById(R.id.iv_client_avatar);
        ivCallClient = findViewById(R.id.iv_call_client);
        ivSendMessage = findViewById(R.id.iv_send_message);
        ivAttach = findViewById(R.id.iv_attach);
        
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        rootLayout = findViewById(R.id.root_layout);
    }
    
    private void setupClickListeners() {
        if (ivCallClient != null) {
            ivCallClient.setOnClickListener(v -> {
                Toast.makeText(this, "Llamar a " + clientName, Toast.LENGTH_SHORT).show();
                // TODO: Implementar llamada telefónica
            });
        }

        if (ivSendMessage != null) {
            ivSendMessage.setOnClickListener(v -> {
                sendMessage();
            });
        }

        if (ivAttach != null) {
            ivAttach.setOnClickListener(v -> showAttachmentOptions());
        }
    }
    
    // Solo manejamos clicks en el root para quitar foco y ocultar el teclado
    private void setupInputBehavior() {
        if (rootLayout == null) return;
        rootLayout.setOnClickListener(v -> {
            if (etMessage != null) {
                etMessage.clearFocus();
                // Ocultar teclado
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
            }
        });
    }
    
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        
        messagesList = new ArrayList<>();
        messagesAdapter = new AdminChatMessagesAdapter(messagesList);
        messagesAdapter.setClientId(clientId);
        rvMessages.setAdapter(messagesAdapter);
    }
    
    private void loadChatData() {
        tvClientName.setText(clientName);
        tvClientStatus.setText("En línea");
    }
    
    private void loadClientAvatar() {
        if (clientId == null || clientId.isEmpty()) {
            return;
        }
        
        firestoreManager.getUserById(clientId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.example.droidtour.models.User && ivClientAvatar != null) {
                    com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                    String photoUrl = null;
                    
                    // Intentar obtener desde personalData primero
                    if (user.getPersonalData() != null) {
                        photoUrl = user.getPersonalData().getProfileImageUrl();
                    }
                    // Fallback al método legacy
                    if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl();
                    }
                    
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(AdminChatDetailActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .circleCrop()
                                .into(ivClientAvatar);
                    } else {
                        ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar placeholder por defecto si falla
                if (ivClientAvatar != null) {
                    ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }
    
    private void initializeConversation() {
        if (clientId == null || companyId == null) {
            Toast.makeText(this, "Error: falta información del cliente o empresa", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Crear o obtener conversación
        chatManager.createOrGetConversation(
            clientId,
            companyId,
            clientName,
            currentUserName != null ? currentUserName : "Administrador",
            new ChatManagerRealtime.ConversationCallback() {
                @Override
                public void onConversationCreated(String convId) {
                    conversationId = convId;
                    setupChat();
                }
                
                @Override
                public void onConversationFound(String convId) {
                    conversationId = convId;
                    setupChat();
                }
                
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminChatDetailActivity.this, "Error al inicializar chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        );
    }
    
    private void setupChat() {
        // Marcar todos los mensajes como leídos al abrir
        chatManager.markAllMessagesAsRead(conversationId, currentUserId);
        
        // Escuchar mensajes en tiempo real
        chatManager.listenForMessages(conversationId, new ChatManagerRealtime.MessagesListener() {
            @Override
            public void onNewMessage(Message message) {
                runOnUiThread(() -> {
                    AdminChatMessage chatMsg = convertToAdminChatMessage(message);
                    messagesList.add(chatMsg);
                    messagesAdapter.notifyItemInserted(messagesList.size() - 1);
                    rvMessages.scrollToPosition(messagesList.size() - 1);
                    
                    // Marcar como entregado y leído si es del cliente
                    if (message.getSenderId().equals(clientId)) {
                        chatManager.markMessageAsDelivered(conversationId, message.getMessageId());
                        chatManager.markMessageAsRead(conversationId, message.getMessageId());
                    }
                });
            }
            
            @Override
            public void onMessageUpdated(Message message) {
                runOnUiThread(() -> {
                    // Actualizar mensaje existente
                    for (int i = 0; i < messagesList.size(); i++) {
                        AdminChatMessage msg = messagesList.get(i);
                        if (msg.message.equals(message.getMessageText())) {
                            // Actualizar si es necesario
                            break;
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(AdminChatDetailActivity.this, "Error al recibir mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
        
        // Escuchar estado en línea del cliente
        if (clientId != null) {
            presenceManager.listenToPresence(clientId, new PresenceManager.PresenceCallback() {
                @Override
                public void onPresenceChanged(String userId, boolean isOnline, long lastSeen) {
                    runOnUiThread(() -> {
                        if (isOnline) {
                            tvClientStatus.setText("En línea");
                        } else {
                            if (lastSeen > 0) {
                                long timeDiff = System.currentTimeMillis() / 1000 - lastSeen;
                                if (timeDiff < 300) {
                                    tvClientStatus.setText("Recientemente");
                                } else {
                                    tvClientStatus.setText("Desconectado");
                                }
                            } else {
                                tvClientStatus.setText("Desconectado");
                            }
                        }
                    });
                }
                
                @Override
                public void onError(Exception e) {
                    // Ignorar errores de presencia
                }
            });
        }
    }
    
    private AdminChatMessage convertToAdminChatMessage(Message message) {
        boolean isFromClient = message.getSenderId().equals(clientId);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeStr = "";
        if (message.getTimestamp() != null) {
            timeStr = sdf.format(message.getTimestamp().toDate());
        }
        
        if (message.hasAttachment()) {
            return new AdminChatMessage(
                message.getMessageText() != null ? message.getMessageText() : "",
                isFromClient,
                timeStr,
                true,
                message.getAttachmentUrl(),
                message.getAttachmentType(),
                message.getAttachmentName(),
                message.getAttachmentSize()
            );
        } else {
            return new AdminChatMessage(message.getMessageText(), isFromClient, timeStr);
        }
    }
    
    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();
        
        // Validar que haya mensaje o archivo adjunto
        if ((messageText.isEmpty() && selectedFileUri == null) || conversationId == null) {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Escriba un mensaje", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        // Si hay archivo adjunto, subirlo primero
        if (selectedFileUri != null && selectedFileType != null) {
            uploadAndSendMessage(messageText);
        } else {
            // Enviar mensaje sin adjunto
            sendTextMessage(messageText);
        }
    }
    
    private void uploadAndSendMessage(String messageText) {
        // Mostrar indicador de progreso
        final View previewLayout = findViewById(R.id.layout_attachment_preview);
        final ProgressBar progressBar = previewLayout != null ? previewLayout.findViewById(R.id.progress_upload) : null;
        final TextView tvProgress = previewLayout != null ? previewLayout.findViewById(R.id.tv_upload_progress) : null;
        
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        }
        if (tvProgress != null) {
            tvProgress.setVisibility(View.VISIBLE);
            tvProgress.setText("Subiendo... 0%");
        }
        
        // Deshabilitar botón de enviar durante la subida
        if (ivSendMessage != null) {
            ivSendMessage.setEnabled(false);
        }
        
        // Generar messageId temporal para la subida
        final String tempMessageId = java.util.UUID.randomUUID().toString();
        
        // Hacer finales las variables que se usarán en callbacks
        final String finalMessageText = messageText;
        final Uri finalSelectedFileUri = selectedFileUri;
        final String finalSelectedFileType = selectedFileType;
        final String finalSelectedFileName = selectedFileName;
        
        // Subir archivo a Firebase Storage
        com.example.droidtour.firebase.FirebaseStorageManager storageManager = 
            com.example.droidtour.firebase.FirebaseStorageManager.getInstance();
        
        storageManager.uploadChatAttachment(
            conversationId,
            tempMessageId,
            finalSelectedFileUri,
            finalSelectedFileType,
            finalSelectedFileName,
            new com.example.droidtour.firebase.FirebaseStorageManager.StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // Ocultar indicador de progreso
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (tvProgress != null) {
                        tvProgress.setVisibility(View.GONE);
                    }
                    
                    // Obtener tamaño del archivo
                    Long fileSize = null;
                    try {
                        android.content.ContentResolver resolver = getContentResolver();
                        android.database.Cursor cursor = resolver.query(finalSelectedFileUri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                            if (sizeIndex >= 0) {
                                fileSize = cursor.getLong(sizeIndex);
                            }
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Exception e) {
                        // Ignorar error
                    }
                    
                    // Crear mensaje con adjunto
                    Message msg = new Message();
                    msg.setSenderId(currentUserId);
                    msg.setSenderName(currentUserName != null ? currentUserName : "Administrador");
                    msg.setReceiverId(clientId);
                    msg.setReceiverName(clientName);
                    msg.setSenderType(prefsManager.getUserType());
                    msg.setMessageText(finalMessageText);
                    msg.setConversationId(conversationId);
                    msg.setCompanyId(companyId);
                    msg.setHasAttachment(true);
                    msg.setAttachmentUrl(downloadUrl);
                    msg.setAttachmentType(finalSelectedFileType);
                    msg.setAttachmentName(finalSelectedFileName);
                    msg.setAttachmentSize(fileSize);
                    
                    // Enviar mensaje
                    chatManager.sendMessage(conversationId, msg, new ChatManagerRealtime.SendCallback() {
                        @Override
                        public void onSuccess(String messageId) {
                            runOnUiThread(() -> {
                                etMessage.setText("");
                                // Limpiar adjunto
                                AdminChatDetailActivity.this.selectedFileUri = null;
                                AdminChatDetailActivity.this.selectedFileType = null;
                                AdminChatDetailActivity.this.selectedFileName = null;
                                if (previewLayout != null) {
                                    previewLayout.setVisibility(View.GONE);
                                }
                                // Habilitar botón de enviar
                                if (ivSendMessage != null) {
                                    ivSendMessage.setEnabled(true);
                                }
                            });
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(AdminChatDetailActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // Habilitar botón de enviar
                                if (ivSendMessage != null) {
                                    ivSendMessage.setEnabled(true);
                                }
                                // Ocultar indicador de progreso
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                if (tvProgress != null) {
                                    tvProgress.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdminChatDetailActivity.this, "Error al subir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Habilitar botón de enviar
                        if (ivSendMessage != null) {
                            ivSendMessage.setEnabled(true);
                        }
                        // Ocultar indicador de progreso
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (tvProgress != null) {
                            tvProgress.setVisibility(View.GONE);
                        }
                    });
                }
                
                @Override
                public void onProgress(int progress) {
                    runOnUiThread(() -> {
                        if (progressBar != null) {
                            progressBar.setProgress(progress);
                        }
                        if (tvProgress != null) {
                            tvProgress.setText("Subiendo... " + progress + "%");
                        }
                    });
                }
            }
        );
    }
    
    private void sendTextMessage(String messageText) {
        // Construir Message
        Message msg = new Message();
        msg.setSenderId(currentUserId);
        msg.setSenderName(currentUserName != null ? currentUserName : "Administrador");
        msg.setReceiverId(clientId);
        msg.setReceiverName(clientName);
        msg.setSenderType(prefsManager.getUserType());
        msg.setMessageText(messageText);
        msg.setConversationId(conversationId);
        msg.setCompanyId(companyId);
        
        // Enviar mediante ChatManagerRealtime
        chatManager.sendMessage(conversationId, msg, new ChatManagerRealtime.SendCallback() {
            @Override
            public void onSuccess(String messageId) {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    // El mensaje se agregará automáticamente cuando llegue por el listener
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(AdminChatDetailActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Establecer usuario como en línea
        if (currentUserId != null) {
            presenceManager.setOnline(currentUserId);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // El heartbeat seguirá actualizando lastSeen
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatManager != null) {
            chatManager.stopListening();
        }
        if (presenceManager != null && currentUserId != null) {
            presenceManager.setOffline(currentUserId);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void redirectToLogin() {
        android.content.Intent intent = new android.content.Intent(this, com.example.droidtour.LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
    
    // ==================== MÉTODOS PARA ARCHIVOS ADJUNTOS ====================
    
    private void showAttachmentOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar archivo");
        String[] options = {"Cámara", "Galería", "Documento PDF"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Cámara
                    openCamera();
                    break;
                case 1: // Galería
                    openGallery();
                    break;
                case 2: // PDF
                    openDocumentPicker();
                    break;
            }
        });
        builder.show();
    }
    
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
            return;
        }
        
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() + ".fileprovider",
                            photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, REQUEST_CODE_CAMERA);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openGallery() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION_STORAGE);
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }
    
    private void openDocumentPicker() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION_STORAGE);
            return;
        }
        
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar PDF"), REQUEST_CODE_DOCUMENT);
    }
    
    private File createImageFile() throws Exception {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK) {
            return;
        }
        
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                if (photoFile != null && photoFile.exists()) {
                    selectedFileUri = Uri.fromFile(photoFile);
                    selectedFileType = "IMAGE";
                    selectedFileName = photoFile.getName();
                    showAttachmentPreview();
                }
                break;
                
            case REQUEST_CODE_GALLERY:
                if (data != null && data.getData() != null) {
                    selectedFileUri = data.getData();
                    selectedFileType = "IMAGE";
                    selectedFileName = getFileName(selectedFileUri);
                    showAttachmentPreview();
                }
                break;
                
            case REQUEST_CODE_DOCUMENT:
                if (data != null && data.getData() != null) {
                    selectedFileUri = data.getData();
                    selectedFileType = "PDF";
                    selectedFileName = getFileName(selectedFileUri);
                    showAttachmentPreview();
                }
                break;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_CODE_PERMISSION_CAMERA:
                    openCamera();
                    break;
                case REQUEST_CODE_PERMISSION_STORAGE:
                    // El usuario debe seleccionar nuevamente la opción
                    break;
            }
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Ignorar error
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "archivo";
    }
    
    private void showAttachmentPreview() {
        View previewLayout = findViewById(R.id.layout_attachment_preview);
        if (previewLayout == null) {
            Toast.makeText(this, "Archivo seleccionado: " + selectedFileName, Toast.LENGTH_SHORT).show();
            return;
        }
        
        previewLayout.setVisibility(View.VISIBLE);
        
        ImageView ivImagePreview = previewLayout.findViewById(R.id.iv_image_preview);
        LinearLayout layoutPdfPreview = previewLayout.findViewById(R.id.layout_pdf_preview);
        TextView tvPdfName = previewLayout.findViewById(R.id.tv_pdf_name);
        TextView tvPdfSize = previewLayout.findViewById(R.id.tv_pdf_size);
        ImageView ivRemoveAttachment = previewLayout.findViewById(R.id.iv_remove_attachment);
        
        if (selectedFileType != null && selectedFileType.equals("IMAGE")) {
            // Mostrar preview de imagen
            if (ivImagePreview != null) {
                ivImagePreview.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(selectedFileUri)
                        .centerCrop()
                        .into(ivImagePreview);
            }
            if (layoutPdfPreview != null) {
                layoutPdfPreview.setVisibility(View.GONE);
            }
        } else if (selectedFileType != null && selectedFileType.equals("PDF")) {
            // Mostrar preview de PDF
            if (ivImagePreview != null) {
                ivImagePreview.setVisibility(View.GONE);
            }
            if (layoutPdfPreview != null) {
                layoutPdfPreview.setVisibility(View.VISIBLE);
            }
            if (tvPdfName != null && selectedFileName != null) {
                tvPdfName.setText(selectedFileName);
            }
            // Obtener tamaño del archivo
            if (tvPdfSize != null && selectedFileUri != null) {
                try {
                    android.content.ContentResolver resolver = getContentResolver();
                    android.database.Cursor cursor = resolver.query(selectedFileUri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex >= 0) {
                            long size = cursor.getLong(sizeIndex);
                            tvPdfSize.setText(formatFileSize(size));
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                } catch (Exception e) {
                    tvPdfSize.setText("");
                }
            }
        }
        
        // Botón para eliminar adjunto
        if (ivRemoveAttachment != null) {
            ivRemoveAttachment.setOnClickListener(v -> {
                selectedFileUri = null;
                selectedFileType = null;
                selectedFileName = null;
                previewLayout.setVisibility(View.GONE);
            });
        }
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}

// Clase para representar un mensaje de chat del administrador
class AdminChatMessage {
    public String message;
    public boolean isFromClient; // true = cliente, false = administrador
    public String timestamp;
    public boolean hasAttachment;
    public String attachmentUrl;
    public String attachmentType; // "IMAGE" o "PDF"
    public String attachmentName;
    public Long attachmentSize;

    public AdminChatMessage(String message, boolean isFromClient, String timestamp) {
        this.message = message;
        this.isFromClient = isFromClient;
        this.timestamp = timestamp;
        this.hasAttachment = false;
    }
    
    public AdminChatMessage(String message, boolean isFromClient, String timestamp, 
                           boolean hasAttachment, String attachmentUrl, String attachmentType,
                           String attachmentName, Long attachmentSize) {
        this.message = message;
        this.isFromClient = isFromClient;
        this.timestamp = timestamp;
        this.hasAttachment = hasAttachment;
        this.attachmentUrl = attachmentUrl;
        this.attachmentType = attachmentType;
        this.attachmentName = attachmentName;
        this.attachmentSize = attachmentSize;
    }
}

// Adaptador para los mensajes del chat del administrador
class AdminChatMessagesAdapter extends RecyclerView.Adapter<AdminChatMessagesAdapter.MessageViewHolder> {
    private List<AdminChatMessage> messages;
    private String clientId;
    private FirestoreManager firestoreManager;

    public AdminChatMessagesAdapter(List<AdminChatMessage> messages) {
        this.messages = messages;
        this.firestoreManager = FirestoreManager.getInstance();
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        AdminChatMessage message = messages.get(position);

        if (message.isFromClient) {
            // Mensaje del cliente (mostrar como mensaje de empresa en el layout)
            holder.layoutIncoming.setVisibility(View.VISIBLE);
            holder.layoutOutgoing.setVisibility(View.GONE);

            // Manejar adjuntos
            if (message.hasAttachment && message.attachmentType != null) {
                if (message.attachmentType.equals("IMAGE")) {
                    // Mostrar imagen
                    if (holder.ivCompanyMessageImage != null) {
                        holder.ivCompanyMessageImage.setVisibility(View.VISIBLE);
                        Glide.with(holder.itemView.getContext())
                                .load(message.attachmentUrl)
                                .centerCrop()
                                .into(holder.ivCompanyMessageImage);
                        // Click listener para descargar/abrir imagen
                        holder.ivCompanyMessageImage.setOnClickListener(v -> {
                            if (message.attachmentUrl != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenImage(
                                    holder.itemView.getContext(),
                                    message.attachmentUrl,
                                    message.attachmentName != null ? message.attachmentName : "imagen.jpg"
                                );
                            }
                        });
                    }
                    if (holder.layoutCompanyPdf != null) {
                        holder.layoutCompanyPdf.setVisibility(View.GONE);
                    }
                } else if (message.attachmentType.equals("PDF")) {
                    // Mostrar PDF
                    if (holder.ivCompanyMessageImage != null) {
                        holder.ivCompanyMessageImage.setVisibility(View.GONE);
                    }
                    if (holder.layoutCompanyPdf != null) {
                        holder.layoutCompanyPdf.setVisibility(View.VISIBLE);
                        if (holder.tvCompanyPdfName != null) {
                            holder.tvCompanyPdfName.setText(message.attachmentName != null ? message.attachmentName : "documento.pdf");
                        }
                        if (holder.tvCompanyPdfSize != null && message.attachmentSize != null) {
                            holder.tvCompanyPdfSize.setText(formatFileSize(message.attachmentSize));
                        }
                        // Click listener para descargar PDF
                        holder.layoutCompanyPdf.setOnClickListener(v -> {
                            if (message.attachmentUrl != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenPdf(
                                    holder.itemView.getContext(),
                                    message.attachmentUrl,
                                    message.attachmentName != null ? message.attachmentName : "documento.pdf"
                                );
                            }
                        });
                    }
                }
                // Ocultar texto si no hay mensaje de texto
                if (holder.tvIncomingMessage != null) {
                    if (message.message != null && !message.message.isEmpty()) {
                        holder.tvIncomingMessage.setVisibility(View.VISIBLE);
                        holder.tvIncomingMessage.setText(message.message);
                    } else {
                        holder.tvIncomingMessage.setVisibility(View.GONE);
                    }
                }
            } else {
                // Sin adjunto, mostrar solo texto
                if (holder.ivCompanyMessageImage != null) {
                    holder.ivCompanyMessageImage.setVisibility(View.GONE);
                }
                if (holder.layoutCompanyPdf != null) {
                    holder.layoutCompanyPdf.setVisibility(View.GONE);
                }
                if (holder.tvIncomingMessage != null) {
                    holder.tvIncomingMessage.setVisibility(View.VISIBLE);
                    holder.tvIncomingMessage.setText(message.message);
                }
            }
            
            if (holder.tvIncomingTime != null) {
                holder.tvIncomingTime.setText(message.timestamp);
            }
            
            // Cargar avatar del cliente en el mensaje
            if (holder.ivCompanyAvatar != null && clientId != null && !clientId.isEmpty()) {
                loadClientAvatarInMessage(holder, clientId);
            }
        } else {
            // Mensaje del administrador (mostrar como mensaje de usuario en el layout)
            holder.layoutIncoming.setVisibility(View.GONE);
            holder.layoutOutgoing.setVisibility(View.VISIBLE);

            // Manejar adjuntos
            if (message.hasAttachment && message.attachmentType != null) {
                if (message.attachmentType.equals("IMAGE")) {
                    // Mostrar imagen
                    if (holder.ivUserMessageImage != null) {
                        holder.ivUserMessageImage.setVisibility(View.VISIBLE);
                        Glide.with(holder.itemView.getContext())
                                .load(message.attachmentUrl)
                                .centerCrop()
                                .into(holder.ivUserMessageImage);
                        // Click listener para descargar/abrir imagen
                        holder.ivUserMessageImage.setOnClickListener(v -> {
                            if (message.attachmentUrl != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenImage(
                                    holder.itemView.getContext(),
                                    message.attachmentUrl,
                                    message.attachmentName != null ? message.attachmentName : "imagen.jpg"
                                );
                            }
                        });
                    }
                    if (holder.layoutUserPdf != null) {
                        holder.layoutUserPdf.setVisibility(View.GONE);
                    }
                } else if (message.attachmentType.equals("PDF")) {
                    // Mostrar PDF
                    if (holder.ivUserMessageImage != null) {
                        holder.ivUserMessageImage.setVisibility(View.GONE);
                    }
                    if (holder.layoutUserPdf != null) {
                        holder.layoutUserPdf.setVisibility(View.VISIBLE);
                        if (holder.tvUserPdfName != null) {
                            holder.tvUserPdfName.setText(message.attachmentName != null ? message.attachmentName : "documento.pdf");
                        }
                        if (holder.tvUserPdfSize != null && message.attachmentSize != null) {
                            holder.tvUserPdfSize.setText(formatFileSize(message.attachmentSize));
                        }
                        // Click listener para descargar PDF
                        holder.layoutUserPdf.setOnClickListener(v -> {
                            if (message.attachmentUrl != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenPdf(
                                    holder.itemView.getContext(),
                                    message.attachmentUrl,
                                    message.attachmentName != null ? message.attachmentName : "documento.pdf"
                                );
                            }
                        });
                    }
                }
                // Ocultar texto si no hay mensaje de texto
                if (holder.tvOutgoingMessage != null) {
                    if (message.message != null && !message.message.isEmpty()) {
                        holder.tvOutgoingMessage.setVisibility(View.VISIBLE);
                        holder.tvOutgoingMessage.setText(message.message);
                    } else {
                        holder.tvOutgoingMessage.setVisibility(View.GONE);
                    }
                }
            } else {
                // Sin adjunto, mostrar solo texto
                if (holder.ivUserMessageImage != null) {
                    holder.ivUserMessageImage.setVisibility(View.GONE);
                }
                if (holder.layoutUserPdf != null) {
                    holder.layoutUserPdf.setVisibility(View.GONE);
                }
                if (holder.tvOutgoingMessage != null) {
                    holder.tvOutgoingMessage.setVisibility(View.VISIBLE);
                    holder.tvOutgoingMessage.setText(message.message);
                }
            }
            
            if (holder.tvOutgoingTime != null) {
                holder.tvOutgoingTime.setText(message.timestamp);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutIncoming, layoutOutgoing, layoutSystem;
        TextView tvIncomingMessage, tvIncomingTime;
        TextView tvOutgoingMessage, tvOutgoingTime;
        TextView tvSystemMessage;
        ImageView ivCompanyAvatar;
        // Adjuntos para mensajes entrantes (cliente)
        ImageView ivCompanyMessageImage;
        LinearLayout layoutCompanyPdf;
        TextView tvCompanyPdfName, tvCompanyPdfSize;
        // Adjuntos para mensajes salientes (admin)
        ImageView ivUserMessageImage;
        LinearLayout layoutUserPdf;
        TextView tvUserPdfName, tvUserPdfSize;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutIncoming = itemView.findViewById(R.id.layout_company_message);
            layoutOutgoing = itemView.findViewById(R.id.layout_user_message);
            layoutSystem = null;

            tvIncomingMessage = itemView.findViewById(R.id.tv_company_message);
            tvIncomingTime = itemView.findViewById(R.id.tv_company_message_time);
            ivCompanyAvatar = itemView.findViewById(R.id.iv_company_avatar);
            
            // Adjuntos entrantes
            ivCompanyMessageImage = itemView.findViewById(R.id.iv_company_message_image);
            layoutCompanyPdf = itemView.findViewById(R.id.layout_company_pdf);
            tvCompanyPdfName = itemView.findViewById(R.id.tv_company_pdf_name);
            tvCompanyPdfSize = itemView.findViewById(R.id.tv_company_pdf_size);

            tvOutgoingMessage = itemView.findViewById(R.id.tv_user_message);
            tvOutgoingTime = itemView.findViewById(R.id.tv_user_message_time);
            
            // Adjuntos salientes
            ivUserMessageImage = itemView.findViewById(R.id.iv_user_message_image);
            layoutUserPdf = itemView.findViewById(R.id.layout_user_pdf);
            tvUserPdfName = itemView.findViewById(R.id.tv_user_pdf_name);
            tvUserPdfSize = itemView.findViewById(R.id.tv_user_pdf_size);

            tvSystemMessage = null;
        }
    }
    
    private void loadClientAvatarInMessage(MessageViewHolder holder, String clientId) {
        if (firestoreManager == null || holder.ivCompanyAvatar == null) {
            return;
        }
        
        firestoreManager.getUserById(clientId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof com.example.droidtour.models.User && holder.ivCompanyAvatar != null) {
                    com.example.droidtour.models.User user = (com.example.droidtour.models.User) result;
                    String photoUrl = null;
                    
                    // Intentar obtener desde personalData primero
                    if (user.getPersonalData() != null) {
                        photoUrl = user.getPersonalData().getProfileImageUrl();
                    }
                    // Fallback al método legacy
                    if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl();
                    }
                    
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(holder.ivCompanyAvatar.getContext())
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_24)
                                .error(R.drawable.ic_avatar_24)
                                .circleCrop()
                                .into(holder.ivCompanyAvatar);
                    } else {
                        holder.ivCompanyAvatar.setImageResource(R.drawable.ic_avatar_24);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Usar placeholder por defecto si falla
                if (holder.ivCompanyAvatar != null) {
                    holder.ivCompanyAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}
