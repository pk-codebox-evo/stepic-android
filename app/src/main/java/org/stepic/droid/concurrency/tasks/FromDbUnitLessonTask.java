package org.stepic.droid.concurrency.tasks;

import com.squareup.otto.Bus;

import org.stepic.droid.base.MainApplication;
import org.stepic.droid.events.units.LoadedFromDbUnitsLessonsEvent;
import org.stepic.droid.exceptions.UnitStoredButLessonNotException;
import org.stepic.droid.model.Lesson;
import org.stepic.droid.model.Progress;
import org.stepic.droid.model.Section;
import org.stepic.droid.model.Unit;
import org.stepic.droid.model.containers.UnitLessonProgressContainer;
import org.stepic.droid.store.operations.DatabaseFacade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class FromDbUnitLessonTask extends StepicTask<Void, Void, UnitLessonProgressContainer> {
    @Inject
    DatabaseFacade mDatabaseFacade;

    @Inject
    Bus mBus;


    private Section mSection;

    public FromDbUnitLessonTask(Section section) {
        super(MainApplication.getAppContext());
        MainApplication.component().inject(this);
        mSection = section;
    }

    @Override
    protected UnitLessonProgressContainer doInBackgroundBody(Void... params) throws Exception {

        List<Unit> fromCacheUnits = mDatabaseFacade.getAllUnitsOfSection(mSection.getId());
        List<Lesson> fromCacheLessons = new ArrayList<>();
        Map<Long, Progress> unitProgressMap = new HashMap<>();

        for (Unit unit : fromCacheUnits) {
            String progressId = unit.getProgressId();
            unit.set_viewed_custom(mDatabaseFacade.isProgressViewed(progressId));

            //new api:
            Progress progress = mDatabaseFacade.getProgressById(progressId);
            if (progress != null) {
                unitProgressMap.put(unit.getId(), progress);
            }
        }

        Collections.sort(fromCacheUnits, new Comparator<Unit>() {
            @Override
            public int compare(Unit lhs, Unit rhs) {
                if (lhs == null || rhs == null) return 0;

                int lhsPos = lhs.getPosition();
                int rhsPos = rhs.getPosition();
                return lhsPos - rhsPos;
            }
        });


//lessons will sort automatically
        for (Unit unitItem : fromCacheUnits) {
            Lesson lesson = mDatabaseFacade.getLessonOfUnit(unitItem);
            if (lesson == null) {
                throw new UnitStoredButLessonNotException();
            }

            fromCacheLessons.add(lesson);
        }


        return new UnitLessonProgressContainer(fromCacheUnits, fromCacheLessons, unitProgressMap);
    }

    @Override
    protected void onSuccess(UnitLessonProgressContainer container) {
        super.onSuccess(container);
        mBus.post(new LoadedFromDbUnitsLessonsEvent(container, mSection));
    }
}
