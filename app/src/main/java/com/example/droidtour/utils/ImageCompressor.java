// java
package com.example.droidtour.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageCompressor {

    /**
     * Comprimir imagen manteniendo calidad aceptable
     */
    public static Bitmap compressImage(String imagePath, int maxWidth, int maxHeight) {
        try {
            // Obtener dimensiones originales
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);

            // Calcular escala
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight);

            // Decodificar con escala
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

            // Corregir orientación
            bitmap = fixOrientation(bitmap, imagePath);

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Comprimir imagen desde URI: copia el contenido del Uri a un archivo temporal y usa su ruta
     */
    public static Bitmap compressImageFromUri(Context context, Uri uri, int maxSize) {
        File tempFile = null;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;

            tempFile = File.createTempFile("img_", ".tmp", context.getCacheDir());
            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            return compressImage(tempFile.getAbsolutePath(), maxSize, maxSize);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                // opcional: borrar temporal inmediatamente
                // tempFile.delete();
            }
        }
    }

    /**
     * Convertir Bitmap a bytes comprimidos
     */
    public static byte[] bitmapToBytes(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    /**
     * Redimensionar imagen
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width > maxWidth || height > maxHeight) {
            float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);

            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }

        return bitmap;
    }

    /**
     * Corregir orientación de la imagen
     */
    private static Bitmap fixOrientation(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (IOException e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * Calcular factor de escala
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
