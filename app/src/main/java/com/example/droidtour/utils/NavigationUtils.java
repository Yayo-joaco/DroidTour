package com.example.droidtour.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.droidtour.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Helper methods for navigation flows that need to reset back to the Login screen.
 */
public final class NavigationUtils {

    private NavigationUtils() {
        // Utility class
    }

    /**
     * Sends the user back to the login screen, clearing the current task.
     *
     * @param activity           caller activity
     * @param signOutFromFirebase whether FirebaseAuth should be signed out before navigating
     */
    public static void navigateBackToLogin(Activity activity, boolean signOutFromFirebase) {
        if (activity == null) return;

        if (signOutFromFirebase) {
            try {
                FirebaseAuth.getInstance().signOut();
            } catch (Exception ignored) {
            }
        }

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}

