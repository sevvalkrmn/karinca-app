package com.karinca.app.ui

import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException

// Turns a caught exception into something safe to show the user. Never falls back to
// e.message directly — that's the raw GoTrue/Postgrest/network exception text (often in
// English, sometimes exposing implementation detail), which is what was reaching users
// verbatim (e.g. a wrong password surfacing Supabase's own "Invalid login credentials"
// string instead of a plain Turkish message). Add a branch here whenever a new
// user-facing action can throw something specific enough to explain plainly.
fun Exception.toUserMessage(fallback: String = "Bir şeyler ters gitti, tekrar dener misin?"): String = when (this) {
    is AuthRestException -> when (errorCode) {
        AuthErrorCode.InvalidCredentials -> "E-posta veya şifre hatalı."
        AuthErrorCode.EmailNotConfirmed -> "E-postana gönderilen onay bağlantısına tıklaman gerekiyor."
        AuthErrorCode.EmailExists, AuthErrorCode.UserAlreadyExists -> "Bu e-posta adresiyle zaten bir hesap var."
        AuthErrorCode.PhoneExists -> "Bu telefon numarasıyla zaten bir hesap var."
        AuthErrorCode.WeakPassword -> "Şifre yeterince güçlü değil."
        AuthErrorCode.UserNotFound -> "Bu bilgilerle bir hesap bulunamadı."
        AuthErrorCode.UserBanned -> "Bu hesaba erişim engellenmiş."
        AuthErrorCode.OtpExpired -> "Doğrulama kodunun süresi doldu, yeni bir kod iste."
        AuthErrorCode.SessionExpired, AuthErrorCode.SessionNotFound, AuthErrorCode.RefreshTokenNotFound ->
            "Oturumun sona ermiş, tekrar giriş yapmayı dener misin?"
        AuthErrorCode.OverEmailSendRateLimit, AuthErrorCode.OverSmsSendRateLimit, AuthErrorCode.OverRequestRateLimit ->
            "Çok fazla deneme yaptın, biraz sonra tekrar dener misin?"
        else -> fallback
    }
    is RestException -> when (statusCode) {
        401, 403 -> "Bu işlem için yetkin yok."
        404 -> "Aradığın kayıt bulunamadı."
        in 500..599 -> "Sunucuda geçici bir sorun var, birazdan tekrar dener misin?"
        else -> fallback
    }
    is IOException -> "İnternet bağlantını kontrol edip tekrar dener misin?"
    else -> fallback
}
