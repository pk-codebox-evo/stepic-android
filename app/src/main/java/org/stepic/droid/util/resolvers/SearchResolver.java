package org.stepic.droid.util.resolvers;

import android.support.annotation.NonNull;

import org.stepic.droid.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

public class SearchResolver implements ISearchResolver {

    @NonNull
    @Override
    public long[] getCourseIdsFromSearchResults(List<SearchResult> searchResultList) {
        List<Long> courseIds = new ArrayList<>();
        if (searchResultList == null || searchResultList.size() == 0) return new long[0];

        for (SearchResult item : searchResultList) {
            long courseId = item.getCourse();
            if (courseId != 0) {
                courseIds.add(courseId);
            }
        }

        long[] result = new long[courseIds.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = courseIds.get(i);
        }
        return result;
    }
}
