package com.kabouzeid.gramophone.ui.activities.base;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.view.View;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.interfaces.KabViewsDisableAble;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public abstract class AbsBaseActivity extends AbsThemeActivity implements KabViewsDisableAble {
    public static final int PERMISSION_REQUEST = 100;

    private boolean areViewsEnabled;
    private boolean recreating;

    private boolean createdWithPermissionsGranted;
    private String[] permissions;
    private String permissionDeniedMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        permissions = getPermissionsToRequest();
        createdWithPermissionsGranted = hasPermissions();

        setPermissionDeniedMessage(null);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!hasPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableViews();

        if (hasPermissions() != createdWithPermissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // the handler is necessary to avoid "java.lang.RuntimeException: Performing pause of activity that is not resumed"
                onPermissionsChanged();
            }
        }
    }

    protected void onPermissionsChanged() {
        if (!recreating) {
            recreating = true;
            postRecreate();
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_UP) {
            showOverflowMenu();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    protected void showOverflowMenu() {

    }

    /**
     * Should be overwritten and re enable all {@link android.view.View} to ensure they are accessible again
     * <p/>
     * This is necessary because of a bug with the shared element transition
     */
    @Override
    public void enableViews() {
        areViewsEnabled = true;
    }

    /**
     * Should be overwritten and disable all views that start a new activity on click to prevent opening an activity multiple times
     * <p/>
     * This is necessary because of a bug with the shared element transition
     */
    @Override
    public void disableViews() {
        areViewsEnabled = false;
    }

    @Override
    public boolean areViewsEnabled() {
        return areViewsEnabled;
    }

    @Nullable
    protected String[] getPermissionsToRequest() {
        return null;
    }

    protected void setPermissionDeniedMessage(String message) {
        if (message == null) {
            permissionDeniedMessage = getString(R.string.permissions_denied);
        } else {
            permissionDeniedMessage = message;
        }
    }

    protected void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            requestPermissions(permissions, PERMISSION_REQUEST);
        }
    }

    protected boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(AbsBaseActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        //User has deny from permission dialog
                        Snackbar.make(getWindow().getDecorView(), permissionDeniedMessage,
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.action_grant, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        requestPermissions();
                                    }
                                })
                                .setActionTextColor(ThemeStore.accentColor(this))
                                .show();
                    } else {
                        // User has deny permission and checked never show permission dialog so you can redirect to Application settings page
                        Snackbar.make(getWindow().getDecorView(), permissionDeniedMessage,
                                Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.action_settings, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", AbsBaseActivity.this.getPackageName(), null);
                                        intent.setData(uri);
                                        startActivity(intent);
                                    }
                                })
                                .setActionTextColor(ThemeStore.accentColor(this))
                                .show();
                    }
                    return;
                }
            }
            onPermissionsChanged();
        }
    }
}
