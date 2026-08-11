package com.example.releaf.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseModule {
    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() {
            if (_client == null) {
                _client = createSupabaseClient(
                    supabaseUrl = Config.SUPABASE_URL,
                    supabaseKey = Config.SUPABASE_ANON_KEY
                ) {
                    install(Auth)
                    install(Postgrest)
                    install(Storage)
                }
            }
            return _client!!
        }
}

object Config {
    const val SUPABASE_URL = "https://kdbwgsxnzqmngkrvoctf.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtkYndnc3huenFtbmdrcnZvY3RmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY0Mjc5NTMsImV4cCI6MjEwMjAwMzk1M30._AZgLEhp-JuHG4J2Lgha8LlwnQXXCp10ScGhbbEiw9g"
}
