package com.example.droidtour.firebase;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Manager para Firebase Storage
 * Proporciona métodos para subir y descargar archivos (imágenes, documentos, etc.)
 */
public class FirebaseStorageManager {
    private static final String TAG = "FirebaseStorageManager";
    private static FirebaseStorageManager instance;
    
    private final FirebaseStorage storage;
    private final StorageReference storageRef;
    
    // Rutas de carpetas en Storage
    private static final String FOLDER_PROFILE_IMAGES = "profile_images";
    private static final String FOLDER_TOUR_IMAGES = "tour_images";
    private static final String FOLDER_COMPANY_LOGOS = "company_logos";
    private static final String FOLDER_COMPANY_COVERS = "company_covers";
    private static final String FOLDER_SERVICE_IMAGES = "service_images";
    private static final String FOLDER_DOCUMENTS = "documents";

    private FirebaseStorageManager() {
        this.storage = FirebaseStorage.getInstance();
        this.storageRef = storage.getReference();
    }

    public static synchronized FirebaseStorageManager getInstance() {
        if (instance == null) {
            instance = new FirebaseStorageManager();
        }
        return instance;
    }

    // ==================== SUBIR IMÁGENES DE PERFIL ====================

    /**
     * Subir imagen de perfil de usuario
     * @param userId ID del usuario
     * @param imageUri URI de la imagen local
     * @param callback Callback con la URL de descarga
     */
    public void uploadProfileImage(String userId, Uri imageUri, StorageCallback callback) {
        if (userId == null || imageUri == null) {
            callback.onFailure(new Exception("User ID and image URI are required"));
            return;
        }

        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_PROFILE_IMAGES).child(fileName);

        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        })
        .addOnSuccessListener(taskSnapshot -> {
            // Obtener URL de descarga
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Profile image uploaded: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL", e);
                callback.onFailure(e);
            });
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading profile image", e);
            callback.onFailure(e);
        });
    }

    /**
     * Subir imagen de perfil desde Bitmap
     */
    public void uploadProfileImageFromBitmap(String userId, Bitmap bitmap, StorageCallback callback) {
        if (userId == null || bitmap == null) {
            callback.onFailure(new Exception("User ID and bitmap are required"));
            return;
        }

        // Comprimir bitmap a JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_PROFILE_IMAGES).child(fileName);

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        })
        .addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Profile image uploaded: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL", e);
                callback.onFailure(e);
            });
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading profile image", e);
            callback.onFailure(e);
        });
    }

    // ==================== SUBIR IMÁGENES DE TOURS ====================

    /**
     * Subir imagen de tour
     * @param tourId ID del tour (o generar uno único si es nuevo)
     * @param imageUri URI de la imagen local
     * @param callback Callback con la URL de descarga
     */
    public void uploadTourImage(String tourId, Uri imageUri, StorageCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new Exception("Image URI is required"));
            return;
        }

        String fileName = (tourId != null ? tourId : UUID.randomUUID().toString()) 
                        + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_TOUR_IMAGES).child(fileName);

        uploadImageFile(imageRef, imageUri, callback);
    }

    /**
     * Subir múltiples imágenes de tour
     */
    public void uploadTourImages(String tourId, Uri[] imageUris, MultipleStorageCallback callback) {
        if (imageUris == null || imageUris.length == 0) {
            callback.onFailure(new Exception("Image URIs are required"));
            return;
        }

        final int totalImages = imageUris.length;
        final String[] downloadUrls = new String[totalImages];
        final int[] uploadedCount = {0};
        final boolean[] hasError = {false};

        for (int i = 0; i < imageUris.length; i++) {
            final int index = i;
            uploadTourImage(tourId, imageUris[i], new StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    downloadUrls[index] = downloadUrl;
                    uploadedCount[0]++;
                    
                    if (uploadedCount[0] == totalImages) {
                        callback.onAllSuccess(downloadUrls);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (!hasError[0]) {
                        hasError[0] = true;
                        callback.onFailure(e);
                    }
                }

                @Override
                public void onProgress(int progress) {
                    // Progress individual de cada imagen
                }
            });
        }
    }

    // ==================== SUBIR LOGOS Y COVERS DE EMPRESAS ====================

    /**
     * Subir logo de empresa
     */
    public void uploadCompanyLogo(String companyId, Uri imageUri, StorageCallback callback) {
        if (companyId == null || imageUri == null) {
            callback.onFailure(new Exception("Company ID and image URI are required"));
            return;
        }

        String fileName = companyId + "_logo_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_COMPANY_LOGOS).child(fileName);

        uploadImageFile(imageRef, imageUri, callback);
    }

    /**
     * Subir imagen de portada de empresa
     */
    public void uploadCompanyCover(String companyId, Uri imageUri, StorageCallback callback) {
        if (companyId == null || imageUri == null) {
            callback.onFailure(new Exception("Company ID and image URI are required"));
            return;
        }

        String fileName = companyId + "_cover_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_COMPANY_COVERS).child(fileName);

        uploadImageFile(imageRef, imageUri, callback);
    }

    // ==================== SUBIR IMÁGENES DE SERVICIOS ====================

    /**
     * Subir imagen de servicio
     */
    public void uploadServiceImage(String serviceId, Uri imageUri, StorageCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new Exception("Image URI is required"));
            return;
        }

        String fileName = (serviceId != null ? serviceId : "service") + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_SERVICE_IMAGES).child(fileName);

        uploadImageFile(imageRef, imageUri, callback);
    }

    // ==================== SUBIR IMÁGENES DE TOURS ====================

    /**
     * Subir imagen de tour
     * @param tourName Nombre del tour (se usa para crear subcarpeta)
     * @param imageUri URI de la imagen local
     * @param imageIndex Índice de la imagen (1, 2, 3, etc.)
     * @param callback Callback con la URL de descarga
     */
    public void uploadTourImage(String tourName, Uri imageUri, int imageIndex, StorageCallback callback) {
        if (imageUri == null) {
            callback.onFailure(new Exception("Image URI is required"));
            return;
        }

        // Limpiar nombre del tour para usarlo como nombre de carpeta
        String cleanTourName = tourName != null ? 
            tourName.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s]", "").replaceAll("\\s+", "_").toLowerCase() : 
            "tour_" + System.currentTimeMillis();
        
        String fileName = cleanTourName + "_img" + imageIndex + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(FOLDER_TOUR_IMAGES).child(cleanTourName).child(fileName);

        Log.d(TAG, "Subiendo imagen de tour: " + FOLDER_TOUR_IMAGES + "/" + cleanTourName + "/" + fileName);
        uploadImageFile(imageRef, imageUri, callback);
    }

    /**
     * Subir múltiples imágenes de tour
     * @param tourName Nombre del tour
     * @param imageUris Lista de URIs de imágenes
     * @param callback Callback con lista de URLs de descarga
     */
    public void uploadTourImages(String tourName, java.util.List<Uri> imageUris, MultipleUploadCallback callback) {
        if (imageUris == null || imageUris.isEmpty()) {
            callback.onComplete(new java.util.ArrayList<>());
            return;
        }

        java.util.List<String> uploadedUrls = new java.util.ArrayList<>();
        final int[] completedCount = {0};
        final int totalImages = imageUris.size();

        for (int i = 0; i < totalImages; i++) {
            final int index = i;
            uploadTourImage(tourName, imageUris.get(i), index + 1, new StorageCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    synchronized (uploadedUrls) {
                        uploadedUrls.add(downloadUrl);
                        completedCount[0]++;
                        
                        if (completedCount[0] >= totalImages) {
                            callback.onComplete(uploadedUrls);
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error subiendo imagen " + (index + 1), e);
                    synchronized (uploadedUrls) {
                        completedCount[0]++;
                        
                        if (completedCount[0] >= totalImages) {
                            callback.onComplete(uploadedUrls);
                        }
                    }
                }

                @Override
                public void onProgress(int progress) {
                    // Progreso individual
                }
            });
        }
    }

    public interface MultipleUploadCallback {
        void onComplete(java.util.List<String> downloadUrls);
    }

    // ==================== SUBIR DOCUMENTOS ====================

    /**
     * Subir documento (PDF, etc.)
     */
    public void uploadDocument(String userId, Uri documentUri, String fileName, StorageCallback callback) {
        if (userId == null || documentUri == null) {
            callback.onFailure(new Exception("User ID and document URI are required"));
            return;
        }

        String uniqueFileName = userId + "_" + System.currentTimeMillis() + "_" + fileName;
        StorageReference docRef = storageRef.child(FOLDER_DOCUMENTS).child(uniqueFileName);

        UploadTask uploadTask = docRef.putFile(documentUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        })
        .addOnSuccessListener(taskSnapshot -> {
            docRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Document uploaded: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL", e);
                callback.onFailure(e);
            });
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading document", e);
            callback.onFailure(e);
        });
    }

    // ==================== ELIMINAR ARCHIVOS ====================

    /**
     * Eliminar archivo por URL
     */
    public void deleteFile(String fileUrl, SimpleStorageCallback callback) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            callback.onFailure(new Exception("File URL is required"));
            return;
        }

        try {
            StorageReference fileRef = storage.getReferenceFromUrl(fileUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "File deleted successfully");
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting file", e);
                        callback.onFailure(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Invalid file URL", e);
            callback.onFailure(e);
        }
    }

    /**
     * Eliminar imagen de perfil anterior al subir una nueva
     */
    public void deleteOldProfileImage(String oldImageUrl, SimpleStorageCallback callback) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            deleteFile(oldImageUrl, callback);
        } else {
            callback.onSuccess();
        }
    }

    // ==================== MÉTODO AUXILIAR ====================

    /**
     * Método auxiliar para subir imágenes
     */
    private void uploadImageFile(StorageReference imageRef, Uri imageUri, StorageCallback callback) {
        UploadTask uploadTask = imageRef.putFile(imageUri);
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress((int) progress);
        })
        .addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Image uploaded: " + downloadUrl);
                callback.onSuccess(downloadUrl);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting download URL", e);
                callback.onFailure(e);
            });
        })
        .addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading image", e);
            callback.onFailure(e);
        });
    }

    // ==================== CALLBACKS ====================

    public interface StorageCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
        void onProgress(int progress);
    }

    public interface MultipleStorageCallback {
        void onAllSuccess(String[] downloadUrls);
        void onFailure(Exception e);
    }

    public interface SimpleStorageCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}

