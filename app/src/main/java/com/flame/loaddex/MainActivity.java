package com.flame.loaddex;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //最终：调用sdcard目录下的com。example。test。testClass。testFunc
        Context appContext = this.getApplicationContext();
//            try {
        //加载第三方dex的函数
//                testLoadDexClassLoader(appContext, "/sdcard/3.dex");
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
        //方式一加载第三方activity
      //  startTestActivity(this, "/sdcard/4.dex");
        startTestActivitySecondMrthod(this, "/sdcard/5.dex");
    }
    //函数一：加载插件dex中的function
    public void testLoadDexClassLoader(Context context, String dexfilepath) throws NoSuchMethodException {
        //用于存放提取出来的dex
        File optfile = context.getDir("opt_dex", 0);
        //存放so文件
        File libfile = context.getDir("lib_path", 0);
        ClassLoader tmpClassLoader = context.getClassLoader();
        //1.加载的dex路径， 2优化的dex路径，3lib路径
        //使用dexclassloader进行第三方dex的加载，可以调用函数，但是不能赋予组件生命周期
        DexClassLoader dexClassLoader = new DexClassLoader(dexfilepath, optfile.getAbsolutePath(),  libfile.getAbsolutePath(), MainActivity.class.getClassLoader());
        //反射获取3.dex的TestClass类
        Class<?> clazz = null;
        try{
            //加载需要调用的类
           clazz =  dexClassLoader.loadClass("com.example.test.TestClass");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //clazz类加载成功
        if(clazz != null){
            //使用反射获取函数名
            try{ Method testFunMethod = clazz.getDeclaredMethod("testFunc");
                //由于testFunc不是静态函数，需要实例化一个object对它进行调用
                Object obj = clazz.newInstance();
                testFunMethod.invoke(obj);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }

    //函数二：实现方式一，使用dexClassLoader替换mClassLoader，加载插件dex中的activity，
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void startTestActivity(Context context, String dexfilepath){

        File optfile = context.getDir("opt_dex", 0);
        File libfile = context.getDir("lib_path", 0);
        ClassLoader tmpClassLoader = context.getClassLoader();
        DexClassLoader dexClassLoader = new DexClassLoader(dexfilepath, optfile.getAbsolutePath(),  libfile.getAbsolutePath(), MainActivity.class.getClassLoader());
        //在这里实现dexClassLoader对mClassloader的替换
        replaceClassLoader(dexClassLoader);
        Class<?> clazz = null;
        try{
            //加载需要调用的类
            clazz =  dexClassLoader.loadClass("com.example.test.TestActivity");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //类加载成功
       // Log.i("flame", "startTestActivity: iii");
        context.startActivity(new Intent(context, clazz));

    } @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    //函数三：实现方式二：将dexClassloader嵌入到pathclassloader和bootclassloader之间，通过双亲委派进行加载
    public void startTestActivitySecondMrthod(Context context, String dexfilepath){
        //用于存放提取出来的dex
        File optfile = context.getDir("opt_dex", 0);
        //存放so文件
        File libfile = context.getDir("lib_path", 0);
        ClassLoader pathClassLoader = MainActivity.class.getClassLoader();
        ClassLoader bootClassloader = MainActivity.class.getClassLoader().getParent();
        //1. 将dexClassloader的父节点设置为bootclassloader
        DexClassLoader dexClassLoader = new DexClassLoader(dexfilepath, optfile.getAbsolutePath(),  libfile.getAbsolutePath(), bootClassloader);
        //2.将pathclassloader的父节点设置为dexclassloader
        try {
            Field parentFeild = ClassLoader.class.getDeclaredField("parent");
            parentFeild.setAccessible(true);
            parentFeild.set(pathClassLoader, dexClassLoader);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        /*
        * I/flame: this:dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/com.flame.loaddex-IdV4PEcCY4Jw6QP1seNBQg==/base.apk"],nativeLibraryDirectories=[/data/app/com.flame.loaddex-IdV4PEcCY4Jw6QP1seNBQg==/lib/arm64, /system/lib64, /system/product/lib64]]]
        * --parent:dalvik.system.DexClassLoader[DexPathList[[dex file "/sdcard/5.dex"],nativeLibraryDirectories=[/data/user/0/com.flame.loaddex/app_lib_path, /system/lib64, /system/product/lib64]]]
I/flame: this:dalvik.system.DexClassLoader[DexPathList[[dex file "/sdcard/5.dex"],nativeLibraryDirectories=[/data/user/0/com.flame.loaddex/app_lib_path, /system/lib64, /system/product/lib64]]]
* --parent:java.lang.BootClassLoader@3fad21*/
        ClassLoader tmpClassloader = pathClassLoader;
        ClassLoader parentClassloader = pathClassLoader.getParent();
        while(parentClassloader != null){
            Log.i("flame", "this:" + tmpClassloader + "--parent:" + parentClassloader);
            tmpClassloader = parentClassloader;
            parentClassloader = parentClassloader.getParent();
        }
        Log.i("flame", "root:" + tmpClassloader);

        Class<?> clazz = null;
        try{
            //加载需要调用的类
            clazz =  dexClassLoader.loadClass("com.example.test.TestActivity");

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //类加载成功
        // Log.i("flame", "startTestActivity: iii");
        context.startActivity(new Intent(context, clazz));

    }

    //进行mClassLoader的替换
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void replaceClassLoader(ClassLoader classLoader){
        try {

            //反射调用使用dexclassloader加载当前的ActivityThread,也就是APP进程入口
            Class<?> ActivityThreadClazz = classLoader.loadClass("android.app.ActivityThread");
            //获取到当前的Activity线程
            Method currentActivityThreadMethod = ActivityThreadClazz.getDeclaredMethod("currentActivityThread");
            //静态可以直接调用
            Object activityThreadObj = currentActivityThreadMethod.invoke(null);
            //在Java反射中Field用于获取某个类的属性或该属性的属性值
            // final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap<>();
            //找到mPackage
            Field mPackagesFiled = ActivityThreadClazz.getDeclaredField("mPackages");
            mPackagesFiled.setAccessible(true);
            //ArrayMap mPackageObj = (ArrayMap) mPackagesFiled.get(activityThreadObj);
            ArrayMap mPackageObj = (ArrayMap) mPackagesFiled.get(activityThreadObj);
            //从mPackage中取出LoadedAPK，也就是当前的包名
            WeakReference wr = (WeakReference) mPackageObj.get(this.getPackageName());
            //获取到loadedapk
            Object loadedApkObj = wr.get();

            //加载到LoadedAPK类，用来寻找mClassLoader
            Class LoadedApkClazz = classLoader.loadClass("android.app.LoadedApk");

            //private ClassLoader mClassLoader
            Field mCLassLoader = LoadedApkClazz.getDeclaredField("mClassLoader");
            mCLassLoader.setAccessible(true);
            mCLassLoader.set(loadedApkObj, classLoader);


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }
}
