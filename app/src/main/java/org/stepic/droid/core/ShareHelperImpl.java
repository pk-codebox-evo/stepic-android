package org.stepic.droid.core;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.jetbrains.annotations.NotNull;
import org.stepic.droid.R;
import org.stepic.droid.base.MainApplication;
import org.stepic.droid.configuration.IConfig;
import org.stepic.droid.model.Course;
import org.stepic.droid.util.HtmlHelper;
import org.stepic.droid.util.StringUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ShareHelperImpl implements ShareHelper {

    @Inject
    IConfig config;

    @Inject
    Context context;


    @Inject
    public ShareHelperImpl() {
        MainApplication.component().inject(this);
    }


    @Override
    public Intent getIntentForCourseSharing(@NotNull Course mCourse) {
        Intent shareIntent = new Intent();
        StringBuilder sb = new StringBuilder();

        if (mCourse.getTitle() != null) {
            sb.append(mCourse.getTitle());
            sb.append("\r\n");
            sb.append("\r\n");
        }

        if (mCourse.getSummary() != null && !mCourse.getSummary().isEmpty()) {
            sb.append(HtmlHelper.fromHtml(mCourse.getSummary()).toString());
            sb.append("\r\n");
            sb.append("\r\n");
        }

        String uriForSharing = Uri.parse(StringUtil.getDynamicLinkForCourse(config, mCourse.getSlug())).toString();
        sb.append(uriForSharing);

        String textForSharing = HtmlHelper.fromHtml(sb.toString()).toString();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, textForSharing);
        shareIntent.setType("text/plain");
        return Intent.createChooser(shareIntent, context.getString(R.string.share_title));
    }
}
