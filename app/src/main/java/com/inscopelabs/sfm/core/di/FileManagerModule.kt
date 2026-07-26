package com.inscopelabs.sfm.core.di

import android.content.Context
import com.inscopelabs.sfm.config.SecurityConfig
import com.inscopelabs.sfm.security.audit.AuditLogger
import com.inscopelabs.sfm.security.encryption.EncryptionManager
import com.inscopelabs.sfm.security.policy.PolicyEngine
import com.inscopelabs.sfm.security.session.SessionManager
import com.inscopelabs.sfm.util.ChecksumCalculator

/**
 * Dependency injection provider module for file manager components.
 */
object FileManagerModule {

    fun provideContext(context: Context): Context = context

    fun provideSessionManager(): SessionManager {
        return SessionManager()
    }

    fun provideAuditLogger(): AuditLogger {
        return AuditLogger().apply {
            setSignedEntries(SecurityConfig.ENABLE_AUDIT_LOGGING)
        }
    }

    fun providePolicyEngine(auditLogger: AuditLogger): PolicyEngine {
        return PolicyEngine(auditLogger)
    }

    fun provideEncryptionManager(context: Context): EncryptionManager {
        return EncryptionManager(context)
    }

    fun provideChecksumCalculator(): ChecksumCalculator {
        return ChecksumCalculator()
    }
}
