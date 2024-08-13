package com.hotbox.terminal.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class UserInteractionInterceptor {

    public static void wrapWindowCallback(Window window, FragmentActivity activity) {
        Window.Callback originalCallback = window.getCallback();

        window.setCallback(new Window.Callback() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                return originalCallback.dispatchKeyEvent(event);
            }

            @Override
            public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                return originalCallback.dispatchKeyShortcutEvent(event);
            }

            //This is the important override
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (activity != null)
                        activity.onUserInteraction();
                }
                return originalCallback.dispatchTouchEvent(event);
            }

            @Override
            public boolean dispatchTrackballEvent(MotionEvent event) {
                return originalCallback.dispatchTrackballEvent(event);
            }

            @Override
            public boolean dispatchGenericMotionEvent(MotionEvent event) {
                return originalCallback.dispatchGenericMotionEvent(event);
            }

            @Override
            public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                return originalCallback.dispatchPopulateAccessibilityEvent(event);
            }

            @Nullable
            @Override
            public View onCreatePanelView(int featureId) {
                return originalCallback.onCreatePanelView(featureId);
            }

            @Override
            public boolean onCreatePanelMenu(int featureId, Menu menu) {
                return originalCallback.onCreatePanelMenu(featureId, menu);
            }

            @Override
            public boolean onPreparePanel(int featureId, View view, Menu menu) {
                return originalCallback.onPreparePanel(featureId, view, menu);
            }

            @Override
            public boolean onMenuOpened(int featureId, Menu menu) {
                return originalCallback.onMenuOpened(featureId, menu);
            }

            @Override
            public boolean onMenuItemSelected(int featureId, MenuItem item) {
                return originalCallback.onMenuItemSelected(featureId, item);
            }

            @Override
            public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
                originalCallback.onWindowAttributesChanged(attrs);
            }

            @Override
            public void onContentChanged() {
                originalCallback.onContentChanged();
            }

            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                originalCallback.onWindowFocusChanged(hasFocus);
            }

            @Override
            public void onAttachedToWindow() {
                originalCallback.onAttachedToWindow();
            }

            @Override
            public void onDetachedFromWindow() {
                originalCallback.onDetachedFromWindow();
            }

            @Override
            public void onPanelClosed(int featureId, Menu menu) {
                originalCallback.onPanelClosed(featureId, menu);
            }

            @Override
            public boolean onSearchRequested() {
                return originalCallback.onSearchRequested();
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public boolean onSearchRequested(SearchEvent searchEvent) {
                return originalCallback.onSearchRequested(searchEvent);
            }

            @Nullable
            @Override
            public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
                return originalCallback.onWindowStartingActionMode(callback);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Nullable
            @Override
            public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
                return originalCallback.onWindowStartingActionMode(callback, type);
            }

            @Override
            public void onActionModeStarted(ActionMode mode) {
                originalCallback.onActionModeStarted(mode);
            }

            @Override
            public void onActionModeFinished(ActionMode mode) {
                originalCallback.onActionModeFinished(mode);
            }
        });
    }
}