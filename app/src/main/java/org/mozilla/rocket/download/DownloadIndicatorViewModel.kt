package org.mozilla.rocket.download

import android.app.DownloadManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.rocket.download.data.DownloadInfoRepository

class DownloadIndicatorViewModel(private val repository: DownloadInfoRepository) : ViewModel() {

    enum class Status {
        DEFAULT, DOWNLOADING, UNREAD, WARNING
    }

    val downloadIndicatorObservable = MutableLiveData<Status>()

    fun updateIndicator() = viewModelScope.launch {
        val list = repository.queryIndicatorStatus()
        var hasDownloading = false
        var hasUnread = false
        var hasWarning = false
        for (item in list) {
            if (!hasDownloading && (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING)) {
                hasDownloading = true
            }
            if (!hasUnread && item.status == DownloadManager.STATUS_SUCCESSFUL && !item.isRead) {
                hasUnread = true
            }
            if (!hasWarning && (item.status == DownloadManager.STATUS_PAUSED || item.status == DownloadManager.STATUS_FAILED)) {
                hasWarning = true
            }
        }
        downloadIndicatorObservable.value = when {
            hasDownloading -> Status.DOWNLOADING
            hasUnread -> Status.UNREAD
            hasWarning -> Status.WARNING
            else -> Status.DEFAULT
        }
    }
}
