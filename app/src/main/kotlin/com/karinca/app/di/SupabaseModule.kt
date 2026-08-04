package com.karinca.app.di

import com.karinca.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Singleton

// SUPABASE_URL / SUPABASE_ANON_KEY come from local.properties via BuildConfig (see
// app/build.gradle.kts) — never hardcoded, never the service-role key. The anon key is
// safe to ship in the client; RLS is what actually protects the data.
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Postgrest)
        // Session persistence (load/save/auto-refresh) is handled by the Auth plugin's
        // own default SettingsSessionManager — no custom persistence code needed here.
        install(Auth)
    }

    @Provides
    @Singleton
    fun providePostgrest(client: SupabaseClient): Postgrest = client.postgrest

    @Provides
    @Singleton
    fun provideAuth(client: SupabaseClient): Auth = client.auth

    // Only for calling ai-service's own small REST surface directly (currently just
    // account deletion) — everything else in the app talks to Supabase via the
    // supabase-kt client above, never raw HTTP. Generous timeout because ai-service
    // runs on Render's free tier, which spins the instance down after idling and can
    // take 30-50s to wake back up on the next request — the default (short) timeout
    // would fail that first request even though the server is genuinely on its way up.
    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
        }
    }
}
