package org.stepic.droid.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.stepic.droid.R;
import org.stepic.droid.base.MainApplication;
import org.stepic.droid.configuration.IConfig;
import org.stepic.droid.model.Course;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MyCoursesAdapter extends ArrayAdapter<Course> {

    @Inject
    IConfig mConfig;

    private List<Course> mCourses;
    private Context mContext;
    private LayoutInflater mInflater;

    public MyCoursesAdapter(Context context, List<Course> courses) {
        super(context, 0, courses);
        mCourses = courses;
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        MainApplication.component(mContext).inject(this);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Course course = getItem(position);

        View view = convertView;
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolderItem viewHolderItem = null;
        if (view == null) {
            view = mInflater.inflate(R.layout.course_item, null);
            viewHolderItem = new ViewHolderItem(view);
            view.setTag(viewHolderItem);
        } else {
            viewHolderItem = (ViewHolderItem) convertView.getTag();
        }
        viewHolderItem.courseName.setText(course.getTitle());
        viewHolderItem.courseSummary.setText(course.getSummary());
        Picasso.with(mContext).load(mConfig.getBaseUrl() + course.getCover()).
                placeholder(R.drawable.stepic_logo_black_and_white).into(viewHolderItem.courseIcon);
        viewHolderItem.courseDateInterval.setText(course.getDateOfCourse());

        return view;
    }


    static class ViewHolderItem {
        @Bind(R.id.course_name)
        TextView courseName;

        @Bind(R.id.course_summary)
        TextView courseSummary;

        @Bind(R.id.course_icon)
        ImageView courseIcon;

        @Bind(R.id.course_date_interval)
        TextView courseDateInterval;

        public ViewHolderItem(View view) {
            ButterKnife.bind(this, view);
        }
    }
}