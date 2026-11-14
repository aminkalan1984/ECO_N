package com.example.domain

import java.security.MessageDigest
import java.security.SecureRandom

data class OtpChallenge(
    val phone: String,
    val codeHash: String,
    val salt: String,
    val createdAt: Long,
    val expiresAt: Long,
    val attempts: Int,
    val consumedAt: Long?
)

sealed interface OtpVerifyResult {
    data class Success(val phone: String) : OtpVerifyResult
    data class Failure(
        val reason: String,
        val remainingAttempts: Int = 0,
        val retryAfterSeconds: Int = 0
    ) : OtpVerifyResult
}

object AuthOtpEngine {
    const val OTP_LENGTH = 6
    const val OTP_TTL_SECONDS = 120
    const val RESEND_COOLDOWN_SECONDS = 60
    const val MAX_VERIFY_ATTEMPTS = 5
    const val LOCK_SECONDS = 15 * 60

    private val random = SecureRandom()

    fun normalizePhone(input: String): String {
        val latin = PersianFormat.toLatinDigits(input).filter { it.isDigit() || it == '+' }
        var normalized = latin.replace(Regex("^\\+?98"), "0")
        if (normalized.startsWith("9")) normalized = "0$normalized"
        return normalized.take(11)
    }

    fun isValidIranMobile(phone: String): Boolean =
        Regex("^09\\d{9}$").matches(normalizePhone(phone))

    fun generateOtp(): String {
        val sb = StringBuilder(OTP_LENGTH)
        repeat(OTP_LENGTH) { sb.append(random.nextInt(10)) }
        return sb.toString()
    }

    fun randomSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashCode(code: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$code".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun safeEqual(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    fun resendWaitSeconds(challenge: OtpChallenge?, now: Long): Int {
        if (challenge == null) return 0
        val remaining = RESEND_COOLDOWN_SECONDS * 1000L - (now - challenge.createdAt)
        return if (remaining > 0) ((remaining + 999L) / 1000L).toInt() else 0
    }

    fun verifyChallenge(
        challenge: OtpChallenge?,
        code: String,
        now: Long,
        lockedUntil: Long?
    ): OtpVerifyResult {
        if (lockedUntil != null && lockedUntil > now) {
            return OtpVerifyResult.Failure(
                reason = "ورود موقتاً قفل شده است؛ ۱۵ دقیقه بعد دوباره امتحان کنید",
                retryAfterSeconds = ((lockedUntil - now + 999L) / 1000L).toInt()
            )
        }
        if (challenge == null || challenge.consumedAt != null) {
            return OtpVerifyResult.Failure("کدی برای این شماره فعال نیست؛ دوباره درخواست دهید")
        }
        if (challenge.expiresAt <= now) {
            return OtpVerifyResult.Failure("کد منقضی شده است؛ کد جدید دریافت کنید")
        }
        if (challenge.attempts >= MAX_VERIFY_ATTEMPTS) {
            return OtpVerifyResult.Failure("تلاش‌های ناموفق زیاد بود؛ ۱۵ دقیقه بعد دوباره امتحان کنید")
        }
        val candidateHash = hashCode(code, challenge.salt)
        if (!safeEqual(candidateHash, challenge.codeHash)) {
            val remaining = MAX_VERIFY_ATTEMPTS - (challenge.attempts + 1)
            return OtpVerifyResult.Failure(
                reason = if (remaining <= 0) {
                    "تلاش‌های ناموفق زیاد بود؛ ۱۵ دقیقه بعد دوباره امتحان کنید"
                } else {
                    "کد نادرست است؛ ${PersianFormat.faDigits(remaining)} تلاش دیگر باقی مانده"
                },
                remainingAttempts = remaining
            )
        }
        return OtpVerifyResult.Success(challenge.phone)
    }
}
