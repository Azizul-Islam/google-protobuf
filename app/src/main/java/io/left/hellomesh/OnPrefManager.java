package io.left.hellomesh;

import android.content.Context;
import android.content.SharedPreferences;

/*
*  ****************************************************************************
*  * Created by : Azizul Islam on 18-Sep-17 at 10:56 AM.
*  * Email : azizul@w3engineers.com
*  * 
*  * Last edited by : Azizul Islam on 18-Sep-17.
*  * 
*  * Last Reviewed by : <Reviewer Name> on <mm/dd/yy>  
*  ****************************************************************************
*/
public class OnPrefManager {

    private OnPrefManager(Context context) {
        mContext = context;
    }

    private static OnPrefManager mOnPrefManager;
    private static Context mContext;
    private static SharedPreferences mSharedPref;
    private static SharedPreferences.Editor mPrefEditor;


    public static OnPrefManager init(Context context) {
        if (mOnPrefManager == null) {
            mOnPrefManager = new OnPrefManager(context);
        }
        initPref(context);
        return mOnPrefManager;
    }


    private static SharedPreferences initPref(Context context) {
        if (mSharedPref == null) {
            mSharedPref = context.getSharedPreferences(context.getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
            mPrefEditor = mSharedPref.edit();
        }

        if (mPrefEditor == null) {
            mPrefEditor = mSharedPref.edit();
        }

        return mSharedPref;
    }

    public static SharedPreferences getPref(Context context) {
        if (mSharedPref == null) {
            mSharedPref = context.getApplicationContext().getSharedPreferences(context.getApplicationContext().getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        }
        return mSharedPref;
    }


    private final String MY_ID = "myid";

    public void setMyId(String myId){
        mPrefEditor.putString(MY_ID, myId);
        mPrefEditor.commit();
    }



}
