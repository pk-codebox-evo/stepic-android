package org.stepic.droid.preferences;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stepic.droid.R;
import org.stepic.droid.analytic.Analytic;
import org.stepic.droid.model.EmailAddress;
import org.stepic.droid.model.Profile;
import org.stepic.droid.model.StorageOption;
import org.stepic.droid.util.StorageUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserPreferences {

    Context mContext;
    SharedPreferenceHelper mSharedPreferenceHelper;
    private Analytic analytic;

    private String kb;
    private String mb;
    private String gb;
    private String defaultStorage;
    private String secondary;
    private String free_title;


    @Inject
    public UserPreferences(Context context, SharedPreferenceHelper helper, Analytic analytic) {
        mContext = context;
        mSharedPreferenceHelper = helper;
        this.analytic = analytic;
        kb = context.getString(R.string.kb);
        mb = context.getString(R.string.mb);
        gb = context.getString(R.string.gb);
        defaultStorage = context.getString(R.string.default_storage);
        secondary = context.getString(R.string.secondary_storage);
        free_title = context.getString(R.string.free_title);


    }

    /**
     * Returns user storage directory under /Android/data/ folder for the currently logged in user.
     * This is the folder where all video downloads should be kept.
     *
     * @return folder for current user, where videos are saved.
     */
    public File getUserDownloadFolder() {

        File android = new File(Environment.getExternalStorageDirectory(), "Android");
        File downloadsDir = new File(android, "data");
        File packDir = new File(downloadsDir, mContext.getPackageName());
        if (packDir == null) return null;
        File userStepicIdDir = new File(packDir, getUserId() + "");
        userStepicIdDir.mkdirs();
        try {
            //hide from gallery our videos.
            File noMediaFile = new File(userStepicIdDir, ".nomedia");
            noMediaFile.createNewFile();
        } catch (IOException ioException) {
            // FIXME: 20.10.15 handle exception
            analytic.reportError(Analytic.Error.CANT_CREATE_NOMEDIA, ioException);
        }

        return userStepicIdDir;
    }

    @Nullable
    public File getSdCardDownloadFolder() {
        try {
            File androidDataPackage = ContextCompat.getExternalFilesDirs(mContext, null)[1];
            if (androidDataPackage == null) return null;
            File userStepicIdDir = new File(androidDataPackage, getUserId() + "");
            userStepicIdDir.mkdirs();
            try {
                //hide from gallery our videos.
                File noMediaFile = new File(userStepicIdDir, ".nomedia");
                noMediaFile.createNewFile();
            } catch (IOException ioException) {
                // FIXME: 20.10.15 handle exception
                analytic.reportError(Analytic.Error.CANT_CREATE_NOMEDIA, ioException);
            }

            return userStepicIdDir;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public long getUserId() {
        Profile userProfile = mSharedPreferenceHelper.getProfile();
        long userId = -1; // default anonymous user id
        if (userProfile != null) {
            userId = userProfile.getId();
        }
        return userId;
    }

    @Nullable
    public List<EmailAddress> getUserEmails() {
        return mSharedPreferenceHelper.getStoredEmails();
    }

    @Nullable
    public EmailAddress getPrimaryEmail() {
        List<EmailAddress> emails = getUserEmails();
        if (emails == null || emails.isEmpty()) return null;
        if (emails.size() == 1) return emails.get(0);

        //emails >1
        EmailAddress primary = null;
        for (EmailAddress item : emails) {
            if (item != null && item.is_primary()) {
                primary = item;
                break;
            }
        }
        if (primary == null) {
            primary = emails.get(0);
        }
        return primary;
    }

    public boolean isNetworkMobileAllowed() {
        return mSharedPreferenceHelper.isMobileInternetAlsoAllowed();
    }

    public String getQualityVideo() {
        return mSharedPreferenceHelper.getVideoQuality();
    }

    public void storeQualityVideo(String videoQuality) {
        mSharedPreferenceHelper.storeVideoQuality(videoQuality);
    }

    public VideoPlaybackRate getVideoPlaybackRate() {
        return mSharedPreferenceHelper.getVideoPlaybackRate();
    }

    public void setVideoPlaybackRate(VideoPlaybackRate rate) {
        mSharedPreferenceHelper.storeVideoPlaybackRate(rate);
    }

    public boolean isOpenInExternal() {
        return mSharedPreferenceHelper.isOpenInExternal();
    }

    public void setOpenInExternal(boolean isOpenInExternal) {
        mSharedPreferenceHelper.setOpenInExternal(isOpenInExternal);
    }

    public boolean isNotificationEnabled() {
        return !mSharedPreferenceHelper.isNotificationDisabled();
    }

    public void setNotificationEnabled(boolean isEnabled) {
        mSharedPreferenceHelper.setNotificationDisabled(!isEnabled);
    }


    public boolean isVibrateNotificationEnabled() {
        return !mSharedPreferenceHelper.isNotificationVibrationDisabled();
    }

    public void setVibrateNotificationEnabled(boolean isEnabled) {
        mSharedPreferenceHelper.setNotificationVibrationDisabled(!isEnabled);
    }

    public boolean isSoundNotificationEnabled() {
        return !mSharedPreferenceHelper.isNotificationSoundDisabled();
    }

    public void setNotificationSoundEnabled(boolean isEnabled) {
        mSharedPreferenceHelper.setNotificationSoundDisabled(!isEnabled);
    }

    public void setSdChosen(boolean isSdChosen) {
        mSharedPreferenceHelper.setSDChosen(isSdChosen);
    }

    public boolean isSdChosen() {
        return mSharedPreferenceHelper.isSDChosen();
    }

    /**
     * @return list of storage option: list.size()<=2, can be empty
     */
    @NotNull
    public List<StorageOption> getStorageOptionList() {
        List<StorageOption> list = new ArrayList<>();
        File[] files = StorageUtil.getRawAppDirs();
        if (files == null || files.length == 0) {
            return list;
        }

        int i = 0;
        while (i < files.length && i < 2) {
            if (files[i] != null) {
                long free = StorageUtil.getAvailableMemorySize(files[i]);
                long total = StorageUtil.getTotalMemorySize(files[i]);
                if (total <= 0) {
                    // not show fake storage
                    continue;
                }

                boolean isChosen = false;
                final boolean isSd = isSdChosen();
                if (isSd && i != 0) {
                    isChosen = true;
                } else if (!isSd && i == 0) {
                    isChosen = true;
                }
                String info = formatOptionList(i, total, free, files[i]);

                StorageOption option = new StorageOption(info, isChosen, total, free, files[i]);
                list.add(option);
            }
            i++;
        }


        return list;
    }

    //move to another class. total&free in bytes
    private String formatOptionList(int index, long total, long free, File file) {
        total /= 1024;
        free /= 1024; //now in kb
        StringBuilder sb = new StringBuilder();
        if (index == 0) {
            sb.append(defaultStorage);
        } else {
            sb.append(secondary);
        }

        sb.append(" (");

        addToBuilderSizeSpaceMeasure(sb, total);
        sb.append(")");
        sb.append(". ");
        addToBuilderSizeSpaceMeasure(sb, free);
        sb.append(" ");
        sb.append(free_title);
        return sb.toString();
    }

    private void addToBuilderSizeSpaceMeasure(StringBuilder sb, long sizeInKb) {
        if (sizeInKb < 1024) {
            sb.append(sizeInKb);
            sb.append(" ");
            sb.append(kb);
        } else {
            sizeInKb /= 1024;

            if (sizeInKb >= 1024) {
                sizeInKb /= 1024;
                sb.append(sizeInKb);
                sb.append(" ");
                sb.append(gb);
            } else {
                sb.append(sizeInKb);
                sb.append(" ");
                sb.append(mb);
            }
        }
    }

}
