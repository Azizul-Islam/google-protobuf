package io.left.hellomesh;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/*
*  ****************************************************************************
*  * Created by : Azizul Islam on 18-Sep-17 at 1:10 PM.
*  * Email : azizul@w3engineers.com
*  * 
*  * Last edited by : Azizul Islam on 18-Sep-17.
*  * 
*  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>  
*  ****************************************************************************
*/
public class InvokPermission {

    private static InvokPermission invokePermission;
    public static final int PERMISSIONS_REQUEST= 1;

    public static synchronized InvokPermission getInstance() {
        if (invokePermission == null) {
            invokePermission = new InvokPermission();
        }
        return invokePermission;
    }

    public boolean isPermitted(Context context, String[] args) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        List<String> finalArgs = new ArrayList<>();
        for (String arg : args) {
            if(context.checkSelfPermission(arg) != PackageManager.PERMISSION_GRANTED) {
                finalArgs.add(arg);
            }
        }

        if (finalArgs.isEmpty()) {
            return true;
        }

        ((Activity) context).requestPermissions(finalArgs.toArray(new String[finalArgs.size()]),
                PERMISSIONS_REQUEST);

        return false;
    }

    public boolean requestPermission(Context context, String... str) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        List<String> finalArgs = new ArrayList<>();
        for (int i = 0; i < str.length; i++) {
            if(context.checkSelfPermission(str[i]) != PackageManager.PERMISSION_GRANTED) {
                finalArgs.add(str[i]);
            }
        }

        if (finalArgs.isEmpty()) {
            return true;
        }

        ((Activity) context).requestPermissions(finalArgs.toArray(new String[finalArgs.size()]), PERMISSIONS_REQUEST);

        return false;
    }

    public void buildToast(Context context, String string){
        Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }

    public boolean isAllowed(Context context, String str){

        if(context == null) return false;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if(context.checkSelfPermission(str) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }
}