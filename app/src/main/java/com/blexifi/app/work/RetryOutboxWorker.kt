package com.blexifi.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blexifi.app.data.local.AppDatabase

class RetryOutboxWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.get(applicationContext)
        val pending = db.messageDao().pendingOutbox()

        // Transport integration pending:
        // for each pending message: attempt BLE/Wi‑Fi Direct send, then update state/attempt count.
        if (pending.isEmpty()) return Result.success()

        pending.forEach {
            db.messageDao().updateState(
                envelopeId = it.envelopeId,
                newState = "RELAYED",
                attempts = it.attempts + 1,
            )
        }

        return Result.success()
    }
}
