package com.ft.sdk.garble.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.lang.reflect.Method;

/**
 * BY huangDianHua
 * DATE:2019-12-19 15:50
 * Description:
 */
public class AopUtils {
    public static String getViewId(View view) {
        String idString = null;
        try {
            if (view.getId() != View.NO_ID) {
                idString = view.getContext().getResources().getResourceEntryName(view.getId());
            }
        } catch (Exception e) {

        }
        return idString;
    }

    public static Activity getActivityFromContext(Context context) {
        Activity activity = null;
        try {
            if (context != null) {
                if (context instanceof Activity) {
                    activity = (Activity) context;
                } else if (context instanceof ContextWrapper) {
                    while (!(context instanceof Activity) && context instanceof ContextWrapper) {
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    if (context instanceof Activity) {
                        activity = (Activity) context;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return activity;
    }

    /**
     * 返回当前类的名称
     *
     * @param object
     * @return
     */
    public static String getClassName(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof Class) {
            return ((Class) object).getSimpleName();
        }
        return object.getClass().getSimpleName();
    }

    /**
     * 返回父类名称
     *
     * @param object
     * @return
     */
    public static String getSupperClassName(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof Class) {
            try {
                return ((Class) object).getSuperclass().getSimpleName();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return object.getClass().getSuperclass().getSimpleName();
    }

    public static String getActivityName(Object object) {
        if (object == null) {
            return "";
        }
        return object.getClass().getSimpleName();
    }

    public static String getViewTree(View view) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(view.getClass().getSimpleName() + "/");
        ViewParent viewParent = view.getParent();
        while (viewParent != null) {
            stringBuffer.insert(0, viewParent.getClass().getSimpleName() + "/");
            viewParent = viewParent.getParent();
        }
        stringBuffer.append("#" + AopUtils.getViewId(view));
        return stringBuffer.toString();
    }

    public static String getDialogClickView(Dialog dialog,int whichButton){
        Class<?> supportAlertDialogClass = null;
        Class<?> androidXAlertDialogClass = null;
        Class<?> currentAlertDialogClass;
        try {
            supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
        } catch (Exception e) {
            //ignored
        }

        try {
            androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
        } catch (Exception e) {
            //ignored
        }

        if (supportAlertDialogClass == null && androidXAlertDialogClass == null) {
            return null;
        }

        if (supportAlertDialogClass != null) {
            currentAlertDialogClass = supportAlertDialogClass;
        } else {
            currentAlertDialogClass = androidXAlertDialogClass;
        }

        if (dialog instanceof android.app.AlertDialog) {
            android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
            Button button = alertDialog.getButton(whichButton);
            if (button != null) {
                return getViewTree(button);
            } else {
                ListView listView = alertDialog.getListView();
                if (listView != null) {
                    ListAdapter listAdapter = listView.getAdapter();
                    Object object = listAdapter.getItem(whichButton);
                    if (object instanceof String) {
                        return getViewTree(listView)+"#value-"+object+"#postion-"+whichButton;
                    }else{
                        return getViewTree(listView)+"#postion-"+whichButton;
                    }
                }
            }

        } else if (currentAlertDialogClass.isInstance(dialog)) {
            Button button = null;
            try {
                Method getButtonMethod = dialog.getClass().getMethod("getButton", int.class);
                if (getButtonMethod != null) {
                    button = (Button) getButtonMethod.invoke(dialog, whichButton);
                }
            } catch (Exception e) {
                //ignored
            }

            if (button != null) {
                return getViewTree(button);
            } else {
                try {
                    Method getListViewMethod = dialog.getClass().getMethod("getListView");
                    if (getListViewMethod != null) {
                        ListView listView = (ListView) getListViewMethod.invoke(dialog);
                        if (listView != null) {
                            ListAdapter listAdapter = listView.getAdapter();
                            Object object = listAdapter.getItem(whichButton);
                            if (object instanceof String) {
                                return getViewTree(listView)+"#value-"+object+"#postion-"+whichButton;
                            }else{
                                return getViewTree(listView)+"#postion-"+whichButton;
                            }
                        }
                    }
                } catch (Exception e) {
                    //ignored
                }
            }
        }
        return null;
    }
}