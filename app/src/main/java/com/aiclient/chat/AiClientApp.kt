package com.aiclient.chat

import android.app.Application
import com.aiclient.chat.data.local.AppDatabase
import com.aiclient.chat.data.prefs.SettingsRepository
import com.aiclient.chat.data.remote.ChatApiClient
import com.aiclient.chat.data.repo.ChatRepository

/** Simple hand-rolled DI container — no framework needed for an app this size. */
class AiClientApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var apiClient: ChatApiClient
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        settingsRepository = SettingsRepository(this)
        chatRepository = ChatRepository(db.conversationDao(), db.messageDao(), db.providerDao())
        apiClient = ChatApiClient()
    }
}
