/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.permission.ui.handheld;

import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_ALLOW_BUTTON;
import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_ALLOW_FOREGROUND_BUTTON;
import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_ALLOW_ONE_TIME_BUTTON;
import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_DENY_AND_DONT_ASK_AGAIN_BUTTON;
import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_DENY_BUTTON;
import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_NO_UPGRADE_AND_DONT_ASK_AGAIN_BUTTON;
import static com.android.permissioncontroller.permission.ui.GrantPermissionsActivity.VISIBILITY_NO_UPGRADE_BUTTON;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.method.LinkMovementMethod;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.permissioncontroller.R;
import com.android.permissioncontroller.permission.ui.GrantPermissionsViewHandler;
import com.android.permissioncontroller.permission.ui.ManagePermissionsActivity;

public class GrantPermissionsViewHandlerImpl implements GrantPermissionsViewHandler,
        OnClickListener {

    public static final String ARG_GROUP_NAME = "ARG_GROUP_NAME";
    public static final String ARG_GROUP_COUNT = "ARG_GROUP_COUNT";
    public static final String ARG_GROUP_INDEX = "ARG_GROUP_INDEX";
    public static final String ARG_GROUP_ICON = "ARG_GROUP_ICON";
    public static final String ARG_GROUP_MESSAGE = "ARG_GROUP_MESSAGE";
    private static final String ARG_GROUP_DETAIL_MESSAGE = "ARG_GROUP_DETAIL_MESSAGE";
    private static final String ARG_DIALOG_BUTTON_VISIBILITIES = "ARG_DIALOG_BUTTON_VISIBILITIES";

    // Animation parameters.
    private static final long SWITCH_TIME_MILLIS = 75;
    private static final long ANIMATION_DURATION_MILLIS = 200;

    private final Activity mActivity;
    private final String mAppPackageName;
    private final UserHandle mUserHandle;

    private ResultListener mResultListener;

    // Configuration of the current dialog
    private String mGroupName;
    private int mGroupCount;
    private int mGroupIndex;
    private Icon mGroupIcon;
    private CharSequence mGroupMessage;
    private CharSequence mDetailMessage;
    private boolean[] mButtonVisibilities;

    // Views
    private ImageView mIconView;
    private TextView mMessageView;
    private TextView mDetailMessageView;
    private Button mAllowButton;
    private Button mAllowForegroundButton;
    private Button mAllowOneTimeButton;
    private Button mDenyButton;
    private Button mDenyAndDontAskAgainButton;
    private Button mNoUpgradeButton;
    private Button mNoUpgradeAndDontAskAgainButton;
    private ViewGroup mRootView;

    public GrantPermissionsViewHandlerImpl(Activity activity, String appPackageName,
            @NonNull UserHandle userHandle) {
        mActivity = activity;
        mAppPackageName = appPackageName;
        mUserHandle = userHandle;
    }

    @Override
    public GrantPermissionsViewHandlerImpl setResultListener(ResultListener listener) {
        mResultListener = listener;
        return this;
    }

    @Override
    public void saveInstanceState(Bundle arguments) {
        arguments.putString(ARG_GROUP_NAME, mGroupName);
        arguments.putInt(ARG_GROUP_COUNT, mGroupCount);
        arguments.putInt(ARG_GROUP_INDEX, mGroupIndex);
        arguments.putParcelable(ARG_GROUP_ICON, mGroupIcon);
        arguments.putCharSequence(ARG_GROUP_MESSAGE, mGroupMessage);
        arguments.putCharSequence(ARG_GROUP_DETAIL_MESSAGE, mDetailMessage);
        arguments.putBooleanArray(ARG_DIALOG_BUTTON_VISIBILITIES, mButtonVisibilities);
    }

    @Override
    public void loadInstanceState(Bundle savedInstanceState) {
        mGroupName = savedInstanceState.getString(ARG_GROUP_NAME);
        mGroupMessage = savedInstanceState.getCharSequence(ARG_GROUP_MESSAGE);
        mGroupIcon = savedInstanceState.getParcelable(ARG_GROUP_ICON);
        mGroupCount = savedInstanceState.getInt(ARG_GROUP_COUNT);
        mGroupIndex = savedInstanceState.getInt(ARG_GROUP_INDEX);
        mDetailMessage = savedInstanceState.getCharSequence(ARG_GROUP_DETAIL_MESSAGE);
        mButtonVisibilities = savedInstanceState.getBooleanArray(ARG_DIALOG_BUTTON_VISIBILITIES);

        updateAll();
    }

    @Override
    public void updateUi(String groupName, int groupCount, int groupIndex, Icon icon,
            CharSequence message, CharSequence detailMessage, boolean[] buttonVisibilities) {
        boolean isNewGroup = mGroupIndex != groupIndex;

        mGroupName = groupName;
        mGroupCount = groupCount;
        mGroupIndex = groupIndex;
        mGroupIcon = icon;
        mGroupMessage = message;
        mDetailMessage = detailMessage;
        mButtonVisibilities = buttonVisibilities;

        // If this is a second (or later) permission and the views exist, then animate.
        if (mIconView != null) {
            updateAll();
        }
    }

    private void updateAll() {
        updateDescription();
        updateDetailDescription();
        updateButtons();

//      Animate change in size
//      Grow or shrink the content container to size of new content
        ChangeBounds growShrinkToNewContentSize = new ChangeBounds();
        growShrinkToNewContentSize.setDuration(ANIMATION_DURATION_MILLIS);
        growShrinkToNewContentSize.setInterpolator(AnimationUtils.loadInterpolator(mActivity,
                android.R.interpolator.fast_out_slow_in));
        TransitionManager.beginDelayedTransition(mRootView, growShrinkToNewContentSize);
    }

    @Override
    public View createView() {
        mRootView = (ViewGroup) LayoutInflater.from(mActivity)
                .inflate(R.layout.grant_permissions, null);

        int h = mActivity.getResources().getDisplayMetrics().heightPixels;
        mRootView.setMinimumHeight(h);
        mRootView.findViewById(R.id.grant_singleton).setOnClickListener(this); // Cancel dialog
        mRootView.findViewById(R.id.grant_dialog).setOnClickListener(this); // Swallow click event

        mMessageView = mRootView.findViewById(R.id.permission_message);
        mDetailMessageView = mRootView.findViewById(R.id.detail_message);
        mDetailMessageView.setMovementMethod(LinkMovementMethod.getInstance());
        mIconView = mRootView.findViewById(R.id.permission_icon);
        mAllowButton = mRootView.findViewById(R.id.permission_allow_button);
        mAllowButton.setOnClickListener(this);
        mAllowForegroundButton =
                mRootView.findViewById(R.id.permission_allow_foreground_only_button);
        mAllowForegroundButton.setOnClickListener(this);
        mAllowOneTimeButton =
                mRootView.findViewById(R.id.permission_allow_one_time_button);
        mAllowOneTimeButton.setOnClickListener(this);
        mDenyButton = mRootView.findViewById(R.id.permission_deny_button);
        mDenyButton.setOnClickListener(this);
        mDenyAndDontAskAgainButton =
                mRootView.findViewById(R.id.permission_deny_and_dont_ask_again_button);
        mDenyAndDontAskAgainButton.setOnClickListener(this);
        mNoUpgradeButton = mRootView.findViewById(R.id.permission_no_upgrade_button);
        mNoUpgradeButton.setOnClickListener(this);
        mNoUpgradeAndDontAskAgainButton =
                mRootView.findViewById(R.id.permission_no_upgrade_and_dont_ask_again_button);
        mNoUpgradeAndDontAskAgainButton.setOnClickListener(this);

        if (mGroupName != null) {
            updateAll();
        }

        return mRootView;
    }

    @Override
    public void updateWindowAttributes(LayoutParams outLayoutParams) {
        // No-op
    }

    private void updateDescription() {
        if (mGroupIcon != null) {
            mIconView.setImageDrawable(mGroupIcon.loadDrawable(mActivity));
        }
        mMessageView.setText(mGroupMessage);
    }

    private void updateDetailDescription() {
        if (mDetailMessage == null) {
            mDetailMessageView.setVisibility(View.GONE);
        } else {
            mDetailMessageView.setText(mDetailMessage);
            mDetailMessageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateButtons() {
        updateButton(mAllowButton, VISIBILITY_ALLOW_BUTTON);
        updateButton(mAllowForegroundButton, VISIBILITY_ALLOW_FOREGROUND_BUTTON);
        updateButton(mAllowOneTimeButton, VISIBILITY_ALLOW_ONE_TIME_BUTTON);
        updateButton(mDenyButton, VISIBILITY_DENY_BUTTON);
        updateButton(mDenyAndDontAskAgainButton, VISIBILITY_DENY_AND_DONT_ASK_AGAIN_BUTTON);
        updateButton(mNoUpgradeButton, VISIBILITY_NO_UPGRADE_BUTTON);
        updateButton(mNoUpgradeAndDontAskAgainButton,
                VISIBILITY_NO_UPGRADE_AND_DONT_ASK_AGAIN_BUTTON);
    }

    private void updateButton(Button button, int pos) {
        button.setVisibility(mButtonVisibilities[pos] ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.permission_allow_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName, GRANTED_ALWAYS);
                }
                break;
            case R.id.permission_allow_foreground_only_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName,
                            GRANTED_FOREGROUND_ONLY);
                }
                break;
            case R.id.permission_allow_one_time_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName, GRANTED_ONE_TIME);
                }
                break;
            case R.id.permission_deny_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName, DENIED);
                }
                break;
            case R.id.permission_deny_and_dont_ask_again_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName, DENIED_DO_NOT_ASK_AGAIN);
                }
                break;
            case R.id.permission_more_info_button:
                Intent intent = new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS);
                intent.putExtra(Intent.EXTRA_PACKAGE_NAME, mAppPackageName);
                intent.putExtra(Intent.EXTRA_USER, mUserHandle);
                intent.putExtra(ManagePermissionsActivity.EXTRA_ALL_PERMISSIONS, true);
                mActivity.startActivity(intent);
                break;
            case R.id.permission_no_upgrade_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName, DENIED);
                }
                break;
            case R.id.permission_no_upgrade_and_dont_ask_again_button:
                if (mResultListener != null) {
                    view.performAccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS, null);
                    mResultListener.onPermissionGrantResult(mGroupName, DENIED_DO_NOT_ASK_AGAIN);
                }
                break;
            case R.id.grant_singleton:
                if (mResultListener != null) {
                    mResultListener.onPermissionGrantResult(mGroupName, CANCELED);
                } else {
                    mActivity.finish();
                }
                break;
        }

    }

    @Override
    public void onBackPressed() {
        if (mResultListener != null) {
            mResultListener.onPermissionGrantResult(mGroupName, CANCELED);
        } else {
            mActivity.finish();
        }
    }

}
