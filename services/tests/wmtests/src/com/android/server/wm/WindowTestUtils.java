/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.server.wm;

import static android.app.AppOpsManager.OP_NONE;
import static android.content.pm.ActivityInfo.RESIZE_MODE_UNRESIZEABLE;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mock;
import static com.android.server.wm.WindowContainer.POSITION_TOP;

import android.app.ActivityManager;
import android.os.IBinder;
import android.view.IWindow;
import android.view.WindowManager;

import com.android.server.wm.ActivityTestsBase.ActivityBuilder;

/**
 * A collection of static functions that provide access to WindowManager related test functionality.
 */
class WindowTestUtils {
    private static int sNextTaskId = 0;

    /** Creates a {@link Task} and adds it to the specified {@link TaskStack}. */
    static Task createTaskInStack(WindowManagerService service, TaskStack stack,
            int userId) {
        synchronized (service.mGlobalLock) {
            final Task newTask = new Task(sNextTaskId++, stack, userId, service, 0, false,
                    new ActivityManager.TaskDescription(), null);
            stack.addTask(newTask, POSITION_TOP);
            return newTask;
        }
    }

    /** Creates an {@link ActivityRecord} and adds it to the specified {@link Task}. */
    static ActivityRecord createActivityRecordInTask(DisplayContent dc, Task task) {
        final ActivityRecord activity = createTestActivityRecord(dc);
        task.addChild(activity, POSITION_TOP);
        return activity;
    }

    static ActivityRecord createTestActivityRecord(DisplayContent dc) {
        synchronized (dc.mWmService.mGlobalLock) {
            final ActivityRecord activity = new ActivityBuilder(dc.mWmService.mAtmService).build();
            activity.onDisplayChanged(dc);
            activity.setOccludesParent(true);
            activity.setHidden(false);
            activity.hiddenRequested = false;
            return activity;
        }
    }

    static TestWindowToken createTestWindowToken(int type, DisplayContent dc) {
        return createTestWindowToken(type, dc, false /* persistOnEmpty */);
    }

    static TestWindowToken createTestWindowToken(int type, DisplayContent dc,
            boolean persistOnEmpty) {
        synchronized (dc.mWmService.mGlobalLock) {
            return new TestWindowToken(type, dc, persistOnEmpty);
        }
    }

    /* Used so we can gain access to some protected members of the {@link WindowToken} class */
    static class TestWindowToken extends WindowToken {

        private TestWindowToken(int type, DisplayContent dc, boolean persistOnEmpty) {
            super(dc.mWmService, mock(IBinder.class), type, persistOnEmpty, dc,
                    false /* ownerCanManageAppTokens */);
        }

        int getWindowsCount() {
            return mChildren.size();
        }

        boolean hasWindow(WindowState w) {
            return mChildren.contains(w);
        }
    }

    /* Used so we can gain access to some protected members of the {@link Task} class */
    static class TestTask extends Task {
        boolean mShouldDeferRemoval = false;
        boolean mOnDisplayChangedCalled = false;
        private boolean mIsAnimating = false;

        TestTask(int taskId, TaskStack stack, int userId, WindowManagerService service,
                int resizeMode, boolean supportsPictureInPicture,
                TaskRecord taskRecord) {
            super(taskId, stack, userId, service, resizeMode, supportsPictureInPicture,
                    new ActivityManager.TaskDescription(), taskRecord);
            stack.addTask(this, POSITION_TOP);
        }

        boolean shouldDeferRemoval() {
            return mShouldDeferRemoval;
        }

        int positionInParent() {
            return getParent().mChildren.indexOf(this);
        }

        @Override
        void onDisplayChanged(DisplayContent dc) {
            super.onDisplayChanged(dc);
            mOnDisplayChangedCalled = true;
        }

        @Override
        boolean isSelfAnimating() {
            return mIsAnimating;
        }

        void setLocalIsAnimating(boolean isAnimating) {
            mIsAnimating = isAnimating;
        }
    }

    static TestTask createTestTask(TaskStack stack) {
        return new TestTask(sNextTaskId++, stack, 0, stack.mWmService, RESIZE_MODE_UNRESIZEABLE,
                false, mock(TaskRecord.class));
    }

    /** Used to track resize reports. */
    static class TestWindowState extends WindowState {
        boolean mResizeReported;

        TestWindowState(WindowManagerService service, Session session, IWindow window,
                WindowManager.LayoutParams attrs, WindowToken token) {
            super(service, session, window, token, null, OP_NONE, 0, attrs, 0, 0,
                    false /* ownerCanAddInternalSystemWindow */);
        }

        @Override
        void reportResized() {
            super.reportResized();
            mResizeReported = true;
        }

        @Override
        public boolean isGoneForLayoutLw() {
            return false;
        }

        @Override
        void updateResizingWindowIfNeeded() {
            // Used in AppWindowTokenTests#testLandscapeSeascapeRotationRelayout to deceive
            // the system that it can actually update the window.
            boolean hadSurface = mHasSurface;
            mHasSurface = true;

            super.updateResizingWindowIfNeeded();

            mHasSurface = hadSurface;
        }
    }
}
