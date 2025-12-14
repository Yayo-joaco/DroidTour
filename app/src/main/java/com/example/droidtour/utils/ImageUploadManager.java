package com.example.droidtour.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class ImageUploadManager {
    private static final String TAG = "ImageUploadManager";
    private static FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Subir imagen desde Bitmap
     */
    public static UploadTask uploadBitmap(Bitmap bitmap, String folder, String fileName) {
        try {
            // Convertir Bitmap a bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();

            // Crear referencia en Storage
            String path = folder + "/" + fileName + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storage.getReference().child(path);

            // Subir imagen
            return imageRef.putBytes(imageData);

        } catch (Exception e) {
            Log.e(TAG, "Error al subir bitmap", e);
            return null;
        }
    }

    /**
     * Subir imagen desde Uri (desde galería)
     */
    public static UploadTask uploadFromUri(Uri uri, String folder, String fileName) {
        try {
            String extension = getFileExtension(uri);
            String path = folder + "/" + fileName + "_" + System.currentTimeMillis() + extension;
            StorageReference imageRef = storage.getReference().child(path);

            return imageRef.putFile(uri);

        } catch (Exception e) {
            Log.e(TAG, "Error al subir desde URI", e);
            return null;
        }
    }

    /**
     * Subir imagen de perfil de usuario
     */
    public static UploadTask uploadUserProfileImage(String userId, Bitmap bitmap) {
        return uploadBitmap(bitmap, "profile_images", userId);
    }

    /**
     * Subir logo de empresa
     */
    public static UploadTask uploadCompanyLogo(String companyId, Bitmap bitmap) {
        return uploadBitmap(bitmap, "company_logos", companyId);
    }

    /**
     * Eliminar imagen de Storage
     */
    public static void deleteImage(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.contains("firebasestorage.googleapis.com")) {
                // Extraer la ruta de la URL
                StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                photoRef.delete().addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Imagen eliminada: " + imageUrl);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar imagen", e);
        }
    }

    /**
     * Obtener URL de descarga después de subir
     */
    public static void getDownloadUrl(UploadTask uploadTask, ImageUploadCallback callback) {
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return uploadTask.getResult().getStorage().getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                callback.onSuccess(downloadUri.toString());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    /**
     * Generar nombre único para imagen
     */
    public static String generateUniqueFileName() {
        return UUID.randomUUID().toString();
    }

    private static String getFileExtension(Uri uri) {
        String path = uri.getPath();
        if (path != null && path.lastIndexOf(".") != -1) {
            return path.substring(path.lastIndexOf("."));
        }
        return ".jpg";
    }

    public interface ImageUploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception exception);
    }
}