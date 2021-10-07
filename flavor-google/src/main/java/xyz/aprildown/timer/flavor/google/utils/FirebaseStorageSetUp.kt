package xyz.aprildown.timer.flavor.google.utils

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

internal fun setUpFirebaseStorage() {
    Firebase.storage.run {
        // We don't retry because our upload, download and query operations are synchronized.
        maxUploadRetryTimeMillis = 0
        maxDownloadRetryTimeMillis = 0
        maxOperationRetryTimeMillis = 0
    }
}
