package org.stepic.droid.core

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.support.annotation.WorkerThread
import android.support.v4.content.ContextCompat
import android.util.Log
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.stepic.droid.concurrency.IMainHandler
import org.stepic.droid.configuration.IConfig
import org.stepic.droid.model.Section
import org.stepic.droid.preferences.UserPreferences
import org.stepic.droid.util.AppConstants
import org.stepic.droid.util.StringUtil
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Singleton

@Singleton
class CalendarPresenterImpl(val config: IConfig,
                            val mainHandler: IMainHandler,
                            val context: Context,
                            val threadPool: ThreadPoolExecutor,
                            val userPreferences: UserPreferences) : CalendarPresenter {

    private var view: CalendarExportableView? = null

    override fun shouldBeShownAsWidget(sectionList: List<Section>?): Boolean {
        //TODO: check preferences
        return shouldBeShown(sectionList)
    }

    override fun shouldBeShown(sectionList: List<Section>?): Boolean {
        if (sectionList == null || sectionList.isEmpty()) return false

        val now: Long = DateTime.now(DateTimeZone.getDefault()).millis
        val nowMinus1Hour = now - AppConstants.MILLIS_IN_1HOUR

        sectionList.forEach {
            if (isDateGreaterNowMinus1Hour(it.soft_deadline, nowMinus1Hour)
                    || isDateGreaterNowMinus1Hour(it.hard_deadline, nowMinus1Hour)) {
                return true;
            }
        }
        return false
    }

    private fun isDateGreaterNowMinus1Hour(deadline: String?, nowMinus1Hour: Long): Boolean {
        if (deadline != null) {
            val deadlineDateTime = DateTime(deadline)
            val deadlineMillis = deadlineDateTime.millis
            if (deadlineMillis - nowMinus1Hour > 0) {
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }

    override fun addDeadlinesToCalendar(sectionList: List<Section>) {
        val permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CALENDAR)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            view?.permissionNotGranted()
            return
        }


        val now: Long = DateTime.now(DateTimeZone.getDefault()).millis
        val nowMinus1Hour = now - AppConstants.MILLIS_IN_1HOUR
        threadPool.execute {
            sectionList.filterNotNull().forEach {
                // We can choose soft_deadline or last_deadline of the Course, if section doesn't have it, but it will pollute calendar.

                // Add upcoming deadlines of sections:
                if (isDateGreaterNowMinus1Hour(it.soft_deadline, nowMinus1Hour)) {
                    val deadline = it.soft_deadline
                    if (deadline != null) {
                        addDeadlineEvent(it, deadline, DeadlineType.softDeadline)
                    }
                }

                if (isDateGreaterNowMinus1Hour(it.hard_deadline, nowMinus1Hour)) {
                    val deadline = it.hard_deadline
                    if (deadline != null) {
                        addDeadlineEvent(it, deadline, DeadlineType.hardDeadline)
                    }
                }
            }

            mainHandler.post {
                view?.successExported()
            }
        }
    }

    @WorkerThread
    private fun addDeadlineEvent(section: Section, deadline: String, deadlineType: DeadlineType) {

        val dateEndInMillis = DateTime(deadline).millis
        val dateStartInMillis = dateEndInMillis - AppConstants.MILLIS_IN_1HOUR

        val contentValues = ContentValues()
        contentValues.put(CalendarContract.Events.DTSTART, dateStartInMillis)
        contentValues.put(CalendarContract.Events.DTEND, dateEndInMillis)

        val calendarTitle = section.title + " — " + context.getString(deadlineType.deadlineTitle)
        contentValues.put(CalendarContract.Events.TITLE, calendarTitle);
        contentValues.put(CalendarContract.Events.DESCRIPTION, StringUtil.getAbsoluteUriForSection(config, section));
        contentValues.put(CalendarContract.Events.CALENDAR_ID, 1)
        contentValues.put(CalendarContract.Events.EVENT_TIMEZONE, DateTimeZone.getDefault().id)

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, contentValues)

        val eventId: Long = (uri.lastPathSegment).toLong()
        Log.d("eee", "eventId = " + eventId)
    }

    override fun onStart(view: CalendarExportableView) {
        this.view = view
    }

    override fun onStop() {
        view = null
    }
}