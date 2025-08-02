package com.astralx.browser.network

import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificatePinningInterceptor @Inject constructor() : Interceptor {
    
    private val certificatePinner = CertificatePinner.Builder()
        // Add your pins here
        .add("*.google.com", "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=")
        .add("*.youtube.com", "sha256/hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=")
        .add("*.github.com", "sha256/uyPYgclc5Jt69vKu92vci6etcBDY8UNTyrHQZJpVoZY=")
        .build()
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        return try {
            // Apply certificate pinning for HTTPS requests
            if (request.url.isHttps) {
                certificatePinner.check(request.url.host, chain.connection()?.handshake()?.peerCertificates ?: emptyList())
            }
            
            chain.proceed(request)
        } catch (e: Exception) {
            Timber.e(e, "Certificate pinning failed for ${request.url.host}")
            throw e
        }
    }
}