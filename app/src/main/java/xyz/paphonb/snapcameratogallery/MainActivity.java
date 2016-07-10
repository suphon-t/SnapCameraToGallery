package xyz.paphonb.snapcameratogallery;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static xyz.paphonb.snapcameratogallery.xposed.SnapCameraToGallery.PACKAGE_CM_SNAP;
import static xyz.paphonb.snapcameratogallery.xposed.SnapCameraToGallery.PACKAGE_OWN;
import static xyz.paphonb.snapcameratogallery.xposed.SnapCameraToGallery.PACKAGE_SNAPCAM;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int BUFFER_LEN = 4096;
    private static final String HIDE_APP_ICON = "hide_app_icon";
    private SharedPreferences mPreferences;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.gallery).setOnClickListener(this);

        if (!isActivated())
            getSupportActionBar().setSubtitle(R.string.module_not_enabled);
        if (!hasXposedInstaller())
            findViewById(R.id.xposed_warning).setVisibility(View.VISIBLE);
        if (!hasSnap())
            findViewById(R.id.snap_warning).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        openFile();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void openFile() {
        String fileName = "/test.png";
        AssetManager assetManager = getAssets();
        try {
            InputStream is = assetManager.open("screenshot.png");
            File out = new File(getFilesDir(), fileName);
            out.createNewFile();
            byte[] buffer = new byte[BUFFER_LEN];
            FileOutputStream fos = new FileOutputStream(out);
            int read;

            while ((read = is.read(buffer, 0, BUFFER_LEN)) >= 0) {
                fos.write(buffer, 0, read);
            }

            fos.flush();
            fos.close();
            is.close();

            makeFileWorldReadable(out);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("image/*");
            intent.setDataAndType(Uri.parse("file://storage" + out.getAbsolutePath()), "image/*");
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("SetWorldReadable")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void makeFileWorldReadable(File f) {
        f.setReadable(true, false);
    }

    @SuppressWarnings("ConstantConditions")
    public boolean hasXposedInstaller() {
        return isActivated() || getXposedInstallerLaunchIntent() != null;
    }

    public Intent getXposedInstallerLaunchIntent() {
        return getPackageManager().getLaunchIntentForPackage("de.robv.android.xposed.installer");
    }

    public boolean hasSnap() {
        return getSnapCamLaunchIntent() != null || getCmSnapLaunchIntent() != null;
    }

    public Intent getSnapCamLaunchIntent() {
        return getPackageManager().getLaunchIntentForPackage(PACKAGE_SNAPCAM);
    }

    public Intent getCmSnapLaunchIntent() {
        return getPackageManager().getLaunchIntentForPackage(PACKAGE_CM_SNAP);
    }

    private boolean isActivated() {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.hide_app_icon).setChecked(getPreferences().getBoolean(HIDE_APP_ICON, false));
        return true;
    }

    @SuppressLint("WorldReadableFiles")
    private SharedPreferences getPreferences() {
        if (mPreferences != null)
            return mPreferences;
        mPreferences = getSharedPreferences(PACKAGE_OWN + "_preferences", MODE_WORLD_READABLE);
        return mPreferences;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean checked = !item.isChecked();
        item.setChecked(checked);
        getPreferences().edit().putBoolean(HIDE_APP_ICON, checked).apply();
        int mode = checked ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        getPackageManager().setComponentEnabledSetting(new ComponentName(this, PACKAGE_OWN + ".MainShortcut" ), mode, PackageManager.DONT_KILL_APP);
        return super.onOptionsItemSelected(item);
    }
}
