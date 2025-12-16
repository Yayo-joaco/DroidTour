package com.example.droidtour.client;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.example.droidtour.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.droidtour.utils.ChatManagerRealtime;
import com.example.droidtour.utils.PresenceManager;
import com.example.droidtour.models.Message;
import com.example.droidtour.models.Company;
import com.example.droidtour.utils.PreferencesManager;
import com.example.droidtour.firebase.FirestoreManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompanyChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private TextInputEditText etMessage;
    private ImageView ivSendMessage;
    private ImageView ivAttach;
    private TextView tvClientName;
    private TextView tvClientStatus;
    private ImageView ivClientAvatar;
    private CompanyChatAdapter chatAdapter;
    private PreferencesManager prefsManager;
    private ChatManagerRealtime chatManager;
    private PresenceManager presenceManager;
    private FirestoreManager firestoreManager;
    private String conversationId;
    private String companyId;
    private String companyName;
    private String companyLogoUrl;
    private String currentUserId;
    private String currentUserName;
    
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
        prefsManager = new PreferencesManager(this);

        // Validar sesión PRIMERO
        if (!prefsManager.isLoggedIn()) {
            redirectToLogin();
            finish();
            return;
        }

        // Validar que el usuario sea CLIENT o ADMIN
        String userType = prefsManager.getUserType();
        if (userType == null || (!userType.equals("CLIENT") && !userType.equals("ADMIN"))) {
            redirectToLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_company_chat);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        setupToolbar();
        initializeViews();
        setupClickListeners();
        loadCompanyInfo();

        // Inicializar managers y obtener userId PRIMERO
        chatManager = new ChatManagerRealtime();
        presenceManager = new PresenceManager();
        firestoreManager = FirestoreManager.getInstance();
        currentUserId = prefsManager.getUserId();
        currentUserName = prefsManager.getUserName();
        
        // Configurar RecyclerView DESPUÉS de tener currentUserId
        setupRecyclerView();

        // Obtener companyId del intent
        companyId = getIntent().getStringExtra("company_id");
        
        // Si no hay companyId, intentar obtenerlo del nombre de la empresa
        if (companyId == null || companyId.isEmpty()) {
            // Por ahora usar el nombre como fallback, pero idealmente debería venir del intent
            initializeConversation();
        } else {
            initializeConversation();
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chat con Empresa");
        }
    }

    private void initializeViews() {
        rvChatMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        ivSendMessage = findViewById(R.id.iv_send_message);
        ivAttach = findViewById(R.id.iv_attach);
        tvClientName = findViewById(R.id.tv_client_name);
        tvClientStatus = findViewById(R.id.tv_client_status);
        ivClientAvatar = findViewById(R.id.iv_client_avatar);
        // Mostrar estado por defecto
        if (tvClientStatus != null) tvClientStatus.setText("En línea");
    }

    private void setupRecyclerView() {
        rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new CompanyChatAdapter(currentUserId);
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        ivSendMessage.setOnClickListener(v -> sendMessage());
        if (ivAttach != null) {
            ivAttach.setOnClickListener(v -> showAttachmentOptions());
        }
    }

    private void loadCompanyInfo() {
        // Get company info from intent
        companyName = getIntent().getStringExtra("company_name");
        if (companyName != null) {
            tvClientName.setText(companyName);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Chat - " + companyName);
        }
        
        // El logo se cargará en initializeConversation() cuando tengamos el companyId
    }
    
    private void loadCompanyLogo() {
        firestoreManager.getCompanyById(companyId, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                if (result instanceof Company) {
                    Company company = (Company) result;
                    companyLogoUrl = company.getLogoUrl();
                    
                    // Actualizar el logo en el header
                    if (ivClientAvatar != null) {
                        if (companyLogoUrl != null && !companyLogoUrl.isEmpty()) {
                            Glide.with(CompanyChatActivity.this)
                                    .load(companyLogoUrl)
                                    .placeholder(R.drawable.ic_avatar_24)
                                    .error(R.drawable.ic_avatar_24)
                                    .circleCrop()
                                    .into(ivClientAvatar);
                        } else {
                            ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                        }
                    }
                    
                    // Actualizar el adaptador con el nuevo logoUrl
                    if (chatAdapter != null) {
                        chatAdapter.setCompanyLogoUrl(companyLogoUrl);
                    }
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                // Si falla, usar logo por defecto (ya está en el layout)
                companyLogoUrl = null;
                if (ivClientAvatar != null) {
                    ivClientAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        });
    }
    
    private void initializeConversation() {
        // Si no tenemos companyId, usar un valor por defecto (esto debería mejorarse)
        if (companyId == null || companyId.isEmpty()) {
            companyId = "unknown_company";
        }
        
        // Cargar logo de la empresa si tenemos companyId válido
        if (companyId != null && !companyId.isEmpty() && !companyId.equals("unknown_company")) {
            loadCompanyLogo();
        }
        
        // Crear o obtener conversación
        chatManager.createOrGetConversation(
            currentUserId,
            companyId,
            currentUserName != null ? currentUserName : "Cliente",
            companyName != null ? companyName : "Empresa",
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
                        Toast.makeText(CompanyChatActivity.this, "Error al inicializar chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    chatAdapter.addMessage(message);
                    rvChatMessages.scrollToPosition(chatAdapter.getItemCount() - 1);
                    
                    // Marcar como entregado si el receptor está en línea
                    if (!message.getSenderId().equals(currentUserId)) {
                        chatManager.markMessageAsDelivered(conversationId, message.getMessageId());
                        // Marcar como leído si estamos viendo la conversación
                        chatManager.markMessageAsRead(conversationId, message.getMessageId());
                    }
                });
            }
            
            @Override
            public void onMessageUpdated(Message message) {
                runOnUiThread(() -> {
                    // Actualizar mensaje existente en el adaptador
                    chatAdapter.updateMessage(message);
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(CompanyChatActivity.this, "Error al recibir mensajes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
        
        // Escuchar estado en línea de la empresa
        presenceManager.listenToPresence(companyId, new PresenceManager.PresenceCallback() {
            @Override
            public void onPresenceChanged(String userId, boolean isOnline, long lastSeen) {
                runOnUiThread(() -> {
                    if (isOnline) {
                        tvClientStatus.setText("En línea");
                    } else {
                        if (lastSeen > 0) {
                            long timeDiff = System.currentTimeMillis() / 1000 - lastSeen;
                            if (timeDiff < 300) { // Menos de 5 minutos
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

    private void sendMessage() {
        String messageText = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        
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
                    msg.setSenderName(currentUserName != null ? currentUserName : "Cliente");
                    msg.setReceiverId(companyId);
                    msg.setReceiverName(companyName != null ? companyName : "Empresa");
                    msg.setSenderType(prefsManager.getUserType());
                    msg.setMessageText(finalMessageText);
                    msg.setTimestamp(Timestamp.now());
                    msg.setIsRead(false);
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
                                CompanyChatActivity.this.selectedFileUri = null;
                                CompanyChatActivity.this.selectedFileType = null;
                                CompanyChatActivity.this.selectedFileName = null;
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
                                Toast.makeText(CompanyChatActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(CompanyChatActivity.this, "Error al subir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        // Construir Message usando el modelo existente
        Message msg = new Message();
        msg.setSenderId(currentUserId);
        msg.setSenderName(currentUserName != null ? currentUserName : "Cliente");
        msg.setReceiverId(companyId);
        msg.setReceiverName(companyName != null ? companyName : "Empresa");
        msg.setSenderType(prefsManager.getUserType());
        msg.setMessageText(messageText);
        msg.setTimestamp(Timestamp.now());
        msg.setIsRead(false);
        msg.setConversationId(conversationId);
        msg.setCompanyId(companyId);

        // Enviar mediante ChatManagerRealtime
        chatManager.sendMessage(conversationId, msg, new ChatManagerRealtime.SendCallback() {
            @Override
            public void onSuccess(String messageId) {
                runOnUiThread(() -> {
                    etMessage.setText("");
                    msg.setMessageId(messageId);
                    // El mensaje se agregará automáticamente cuando llegue por el listener
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(CompanyChatActivity.this, "Error al enviar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
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
        if (presenceManager != null) {
            presenceManager.setOffline(currentUserId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
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

// Adaptador para los mensajes del chat de la empresa (dinámico)
class CompanyChatAdapter extends RecyclerView.Adapter<CompanyChatAdapter.ViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private final String currentUserId;
    private String companyLogoUrl;

    CompanyChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }
    
    public void setCompanyLogoUrl(String logoUrl) {
        this.companyLogoUrl = logoUrl;
        // Notificar cambios para actualizar avatares
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Message message = messages.get(position);

        View layoutUserMessage = holder.itemView.findViewById(R.id.layout_user_message);
        View layoutCompanyMessage = holder.itemView.findViewById(R.id.layout_company_message);
        TextView tvUserMessage = holder.itemView.findViewById(R.id.tv_user_message);
        TextView tvCompanyMessage = holder.itemView.findViewById(R.id.tv_company_message);
        TextView tvCompanyTime = holder.itemView.findViewById(R.id.tv_company_message_time);
        TextView tvUserTime = holder.itemView.findViewById(R.id.tv_user_message_time);

        // Verificar si el mensaje es del usuario actual
        // Comparar tanto por senderId como por senderType para mayor seguridad
        String senderId = message.getSenderId();
        String senderType = message.getSenderType();
        boolean isCurrentUser = (currentUserId != null && senderId != null && currentUserId.equals(senderId)) ||
                                (senderType != null && senderType.equals("CLIENT"));

        // Obtener referencias a elementos de adjuntos
        ImageView ivUserMessageImage = holder.itemView.findViewById(R.id.iv_user_message_image);
        LinearLayout layoutUserPdf = holder.itemView.findViewById(R.id.layout_user_pdf);
        TextView tvUserPdfName = holder.itemView.findViewById(R.id.tv_user_pdf_name);
        TextView tvUserPdfSize = holder.itemView.findViewById(R.id.tv_user_pdf_size);
        ImageView ivCompanyMessageImage = holder.itemView.findViewById(R.id.iv_company_message_image);
        LinearLayout layoutCompanyPdf = holder.itemView.findViewById(R.id.layout_company_pdf);
        TextView tvCompanyPdfName = holder.itemView.findViewById(R.id.tv_company_pdf_name);
        TextView tvCompanyPdfSize = holder.itemView.findViewById(R.id.tv_company_pdf_size);
        
        if (isCurrentUser) {
            // Mensaje del cliente (usuario actual) - mostrar a la DERECHA en azul
            if (layoutUserMessage != null) layoutUserMessage.setVisibility(View.VISIBLE);
            if (layoutCompanyMessage != null) layoutCompanyMessage.setVisibility(View.GONE);
            
            // Manejar adjuntos
            if (message.hasAttachment() && message.getAttachmentType() != null) {
                if (message.getAttachmentType().equals("IMAGE")) {
                    // Mostrar imagen
                    if (ivUserMessageImage != null) {
                        ivUserMessageImage.setVisibility(View.VISIBLE);
                        Glide.with(holder.itemView.getContext())
                                .load(message.getAttachmentUrl())
                                .centerCrop()
                                .into(ivUserMessageImage);
                        // Click listener para descargar/abrir imagen
                        ivUserMessageImage.setOnClickListener(v -> {
                            if (message.getAttachmentUrl() != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenImage(
                                    holder.itemView.getContext(),
                                    message.getAttachmentUrl(),
                                    message.getAttachmentName() != null ? message.getAttachmentName() : "imagen.jpg"
                                );
                            }
                        });
                    }
                    if (layoutUserPdf != null) layoutUserPdf.setVisibility(View.GONE);
                } else if (message.getAttachmentType().equals("PDF")) {
                    // Mostrar PDF
                    if (ivUserMessageImage != null) ivUserMessageImage.setVisibility(View.GONE);
                    if (layoutUserPdf != null) {
                        layoutUserPdf.setVisibility(View.VISIBLE);
                        if (tvUserPdfName != null) {
                            tvUserPdfName.setText(message.getAttachmentName() != null ? message.getAttachmentName() : "documento.pdf");
                        }
                        if (tvUserPdfSize != null && message.getAttachmentSize() != null) {
                            tvUserPdfSize.setText(formatFileSize(message.getAttachmentSize()));
                        }
                        // Click listener para descargar PDF
                        layoutUserPdf.setOnClickListener(v -> {
                            if (message.getAttachmentUrl() != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenPdf(
                                    holder.itemView.getContext(),
                                    message.getAttachmentUrl(),
                                    message.getAttachmentName() != null ? message.getAttachmentName() : "documento.pdf"
                                );
                            }
                        });
                    }
                }
                // Mostrar texto si existe
                if (tvUserMessage != null) {
                    if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
                        tvUserMessage.setVisibility(View.VISIBLE);
                        tvUserMessage.setText(message.getMessageText());
                    } else {
                        tvUserMessage.setVisibility(View.GONE);
                    }
                }
            } else {
                // Sin adjunto, mostrar solo texto
                if (ivUserMessageImage != null) ivUserMessageImage.setVisibility(View.GONE);
                if (layoutUserPdf != null) layoutUserPdf.setVisibility(View.GONE);
                if (tvUserMessage != null) {
                    tvUserMessage.setVisibility(View.VISIBLE);
                    tvUserMessage.setText(message.getMessageText());
                }
            }
            
            if (tvUserTime != null && message.getTimestamp() != null) {
                tvUserTime.setText(android.text.format.DateFormat.format("hh:mm a", message.getTimestamp().toDate()));
            }
        } else {
            // Mensaje de la empresa - mostrar a la IZQUIERDA en blanco/gris
            if (layoutUserMessage != null) layoutUserMessage.setVisibility(View.GONE);
            if (layoutCompanyMessage != null) layoutCompanyMessage.setVisibility(View.VISIBLE);
            
            // Manejar adjuntos
            if (message.hasAttachment() && message.getAttachmentType() != null) {
                if (message.getAttachmentType().equals("IMAGE")) {
                    // Mostrar imagen
                    if (ivCompanyMessageImage != null) {
                        ivCompanyMessageImage.setVisibility(View.VISIBLE);
                        Glide.with(holder.itemView.getContext())
                                .load(message.getAttachmentUrl())
                                .centerCrop()
                                .into(ivCompanyMessageImage);
                        // Click listener para descargar/abrir imagen
                        ivCompanyMessageImage.setOnClickListener(v -> {
                            if (message.getAttachmentUrl() != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenImage(
                                    holder.itemView.getContext(),
                                    message.getAttachmentUrl(),
                                    message.getAttachmentName() != null ? message.getAttachmentName() : "imagen.jpg"
                                );
                            }
                        });
                    }
                    if (layoutCompanyPdf != null) layoutCompanyPdf.setVisibility(View.GONE);
                } else if (message.getAttachmentType().equals("PDF")) {
                    // Mostrar PDF
                    if (ivCompanyMessageImage != null) ivCompanyMessageImage.setVisibility(View.GONE);
                    if (layoutCompanyPdf != null) {
                        layoutCompanyPdf.setVisibility(View.VISIBLE);
                        if (tvCompanyPdfName != null) {
                            tvCompanyPdfName.setText(message.getAttachmentName() != null ? message.getAttachmentName() : "documento.pdf");
                        }
                        if (tvCompanyPdfSize != null && message.getAttachmentSize() != null) {
                            tvCompanyPdfSize.setText(formatFileSize(message.getAttachmentSize()));
                        }
                        // Click listener para descargar PDF
                        layoutCompanyPdf.setOnClickListener(v -> {
                            if (message.getAttachmentUrl() != null) {
                                com.example.droidtour.utils.AttachmentDownloadHelper.downloadAndOpenPdf(
                                    holder.itemView.getContext(),
                                    message.getAttachmentUrl(),
                                    message.getAttachmentName() != null ? message.getAttachmentName() : "documento.pdf"
                                );
                            }
                        });
                    }
                }
                // Mostrar texto si existe
                if (tvCompanyMessage != null) {
                    if (message.getMessageText() != null && !message.getMessageText().isEmpty()) {
                        tvCompanyMessage.setVisibility(View.VISIBLE);
                        tvCompanyMessage.setText(message.getMessageText());
                    } else {
                        tvCompanyMessage.setVisibility(View.GONE);
                    }
                }
            } else {
                // Sin adjunto, mostrar solo texto
                if (ivCompanyMessageImage != null) ivCompanyMessageImage.setVisibility(View.GONE);
                if (layoutCompanyPdf != null) layoutCompanyPdf.setVisibility(View.GONE);
                if (tvCompanyMessage != null) {
                    tvCompanyMessage.setVisibility(View.VISIBLE);
                    tvCompanyMessage.setText(message.getMessageText());
                }
            }
            
            if (tvCompanyTime != null && message.getTimestamp() != null) {
                tvCompanyTime.setText(android.text.format.DateFormat.format("hh:mm a", message.getTimestamp().toDate()));
            }
            
            // Cargar avatar de la empresa (logoUrl)
            ImageView ivCompanyAvatar = holder.itemView.findViewById(R.id.iv_company_avatar);
            if (ivCompanyAvatar != null) {
                if (companyLogoUrl != null && !companyLogoUrl.isEmpty()) {
                    Glide.with(ivCompanyAvatar.getContext())
                            .load(companyLogoUrl)
                            .placeholder(R.drawable.ic_avatar_24)
                            .error(R.drawable.ic_avatar_24)
                            .circleCrop()
                            .into(ivCompanyAvatar);
                } else {
                    // Usar placeholder por defecto si no hay logoUrl
                    ivCompanyAvatar.setImageResource(R.drawable.ic_avatar_24);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message m) {
        messages.add(m);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessages(List<Message> ms) {
        int start = messages.size();
        messages.addAll(ms);
        notifyItemRangeInserted(start, ms.size());
    }
    
    public void updateMessage(Message updatedMessage) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getMessageId() != null && 
                messages.get(i).getMessageId().equals(updatedMessage.getMessageId())) {
                messages.set(i, updatedMessage);
                notifyItemChanged(i);
                return;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View v) {
            super(v);
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