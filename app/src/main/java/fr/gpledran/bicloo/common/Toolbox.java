package fr.gpledran.bicloo.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;

public final class Toolbox {

    public static void showProgressBar(final View progressOverlay) {
        animateView(progressOverlay, View.VISIBLE, 0.4f, 200);
    }

    public static void hideProgressBar(final View progressOverlay) {
        animateView(progressOverlay, View.GONE, 0, 200);
    }

    public static boolean isNetworkAvailable(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    private static void animateView(final View view, final int toVisibility, float toAlpha, int duration) {
        boolean show = toVisibility == View.VISIBLE;
        if (show) {
            view.setAlpha(0);
        }
        view.setVisibility(View.VISIBLE);
        view.animate()
            .setDuration(duration)
            .alpha(show ? toAlpha : 0)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setVisibility(toVisibility);
                }
            });
    }
}
