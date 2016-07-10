package xyz.paphonb.snapcameratogallery.xposed;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SnapCameraToGallery implements IXposedHookLoadPackage {
    public static final String PACKAGE_OWN = "xyz.paphonb.snapcameratogallery";
    public static final String PACKAGE_SNAPCAM = "org.codeaurora.snapcam";
    public static final String PACKAGE_CM_SNAP = "org.cyanogenmod.snap";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(PACKAGE_SNAPCAM) || lpparam.packageName.equals(PACKAGE_CM_SNAP)) {
            hookSnap(lpparam);
        } else if (lpparam.packageName.equals(PACKAGE_OWN)) {
            XposedHelpers.findAndHookMethod(PACKAGE_OWN + ".MainActivity", lpparam.classLoader, "isActivated", XC_MethodReplacement.returnConstant(true));
        }
    }

    private void hookSnap(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> classRefocusActivity = XposedHelpers.findClass("com.android.camera.RefocusActivity", lpparam.classLoader);

        XposedHelpers.findAndHookMethod("com.android.camera.CameraActivity", lpparam.classLoader, "gotoGallery", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                Object adapter = XposedHelpers.callMethod(activity, "getDataAdapter");
                Object img = XposedHelpers.callMethod(adapter, "getImageData", 1);
                Object mCurrentModule = XposedHelpers.getObjectField(activity, "mCurrentModule");
                if (img == null)
                    return null;
                Uri uri = (Uri) XposedHelpers.callMethod(img, "getContentUri");
                if (mCurrentModule.getClass().getName().contains("PhotoModule")) {
                    if ((boolean) XposedHelpers.callMethod(mCurrentModule, "isRefocus")) {
                        Intent intent = new Intent();
                        intent.setClass(activity, classRefocusActivity);
                        intent.setData(uri);
                        activity.startActivity(intent);
                        return null;
                    }
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setType("image/*");
                    intent.setDataAndType(uri, "image/*");
                    activity.startActivity(intent);
                } catch (Throwable t) {
                    XposedBridge.log("SnapCameraToGallery E/Can\'t start gallery");
                    XposedBridge.log(t);
                }
                return null;
            }
        });
    }
}
