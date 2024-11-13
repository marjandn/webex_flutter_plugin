package ae.altkamul.webex_flutter_plugin.calling.calendarMeeting.details

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ciscowebex.androidsdk.calendarMeeting.CalendarMeeting
import ae.altkamul.webex_flutter_plugin.BaseViewModel
import ae.altkamul.webex_flutter_plugin.calling.calendarMeeting.CalendarMeetingsRepository
import io.reactivex.android.schedulers.AndroidSchedulers

class CalendarMeetingDetailsViewModel(private val repo: CalendarMeetingsRepository) : BaseViewModel() {
    private val tag = CalendarMeetingDetailsViewModel::class.java.name
    private val _meeting = MutableLiveData<CalendarMeeting>()
    val meeting: LiveData<CalendarMeeting> = _meeting

    fun getCalendarMeetingById(id: String) {
        repo.getCalendarMeetingById(id).observeOn(AndroidSchedulers.mainThread())
            .subscribe({ meeting ->
                _meeting.postValue(meeting)
            }, {
                Log.d(tag, it.message ?: "Error in getCalendarMeetingById api")
            }).autoDispose()
    }
}