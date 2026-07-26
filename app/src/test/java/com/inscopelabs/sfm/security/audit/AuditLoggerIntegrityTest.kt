package com.inscopelabs.sfm.security.audit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.sfm.data.AppDatabase
import com.inscopelabs.sfm.data.AuditDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuditLoggerIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var auditDao: AuditDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auditDao = db.auditDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testUntamperedChainIsValid() {
        val logger = AuditLogger(auditDao = auditDao)

        logger.logFileOperation(
            sessionId = "session-1",
            operation = "read",
            resource = "/test/file.txt",
            result = OperationResult.ALLOWED
        )

        logger.logAuthentication(
            sessionId = "session-1",
            userId = "user1",
            success = true,
            method = "password"
        )

        val result = logger.verifyIntegrity()
        assertTrue("Untampered chain should be valid", result is AuditLogger.IntegrityResult.VALID)
    }

    @Test
    fun testTamperedEntryIsInvalid() = runBlocking {
        val logger = AuditLogger(auditDao = auditDao)

        logger.logFileOperation(
            sessionId = "session-1",
            operation = "read",
            resource = "/test/file.txt",
            result = OperationResult.ALLOWED
        )

        logger.logAuthentication(
            sessionId = "session-1",
            userId = "user1",
            success = true,
            method = "password"
        )

        // Directly tamper with one entry in storage
        val entries = auditDao.getAllEntries()
        val first = entries.first()
        val tampered = first.copy(resource = "/tampered/path.txt")
        auditDao.insert(tampered)

        val result = logger.verifyIntegrity()
        assertTrue("Tampered entry should make chain invalid", result is AuditLogger.IntegrityResult.INVALID)
    }

    @Test
    fun testPersistenceSurvivesLoggerRecreation() {
        val logger1 = AuditLogger(auditDao = auditDao)

        logger1.logFileOperation(
            sessionId = "session-123",
            operation = "write",
            resource = "/test/doc.pdf",
            result = OperationResult.ALLOWED
        )

        // Wait briefly for async persist
        Thread.sleep(100)

        // Simulate process death / new logger instance using same DAO
        val logger2 = AuditLogger(auditDao = auditDao)
        val recent = logger2.getRecentEntries(10)

        assertEquals(1, recent.size)
        assertEquals("session-123", recent.first().sessionId)
        assertEquals("/test/doc.pdf", recent.first().resource)
    }
}
