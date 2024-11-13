package ae.altkamul.webex_flutter_plugin.calling.calendarMeeting

import ae.altkamul.webex_flutter_plugin.calling.calendarMeeting.details.CalendarMeetingDetailsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val calendarMeetingsModule = module {
    viewModel { CalendarMeetingsViewModel(get(), get()) }
    viewModel { CalendarMeetingDetailsViewModel(get()) }

    single { CalendarMeetingsRepository(get()) }
}