package com.example.releaf.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.asSharedFlow

object SupabaseModule {
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() {
            if (_client == null) {
                require(com.example.releaf.BuildConfig.SUPABASE_URL.isNotBlank() && com.example.releaf.BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
                    "Supabase credentials are missing. Add supabase.url and supabase.anon.key to local.properties."
                }
                _client = createSupabaseClient(
                    supabaseUrl = com.example.releaf.BuildConfig.SUPABASE_URL,
                    supabaseKey = com.example.releaf.BuildConfig.SUPABASE_ANON_KEY
                ) {
                    install(Auth)
                    install(Postgrest)
                    install(Storage)
                    install(Realtime)
                }
            }
            return _client!!
        }

    private val _refreshEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 0)
    val refreshEvent = _refreshEvent.asSharedFlow()

    suspend fun triggerRefresh() {
        _refreshEvent.emit(Unit)
    }
}
