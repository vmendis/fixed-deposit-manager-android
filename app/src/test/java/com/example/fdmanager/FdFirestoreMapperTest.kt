package com.example.fdmanager

import com.example.fdmanager.data.FdFirestoreMapper
import com.example.fdmanager.data.model.CompoundingFrequency
import com.example.fdmanager.data.model.FdStatus
import com.example.fdmanager.data.model.FixedDeposit
import com.example.fdmanager.data.model.PayoutFrequency
import com.example.fdmanager.data.model.RenewOption
import com.example.fdmanager.data.model.TenorUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * #10 — Tier1 coverage for the FixedDeposit ⇄ Firestore map (CLOUD_GATE = cloud-1000r1).
 * Pure JVM: the mapper deliberately has no Firestore types in its signatures.
 */
class FdFirestoreMapperTest {

    private val fd = FixedDeposit(
        id = "fd-uuid-1",
        fdNumber = "NSB-1",
        bank = "National Savings Bank (NSB)",
        amount = 250_000.0,
        openedDate = LocalDate.of(2026, 1, 10),
        durationMonths = 12,
        interestRate = 10.5,
        maturityDate = LocalDate.of(2027, 1, 10),
        branch = "Colombo Main",
        branchCode = "001",
        payoutFrequency = PayoutFrequency.MONTHLY,
        renewOption = RenewOption.CAPITALIZE,
        autoRenew = true,
        isActive = false,
        status = FdStatus.RENEWED,
        parentFdId = "fd-uuid-0",
        createdAt = 1_700_000_000_000L,
        isDeleted = true,
        durationDays = 100,
        tenorUnit = TenorUnit.DAYS,
        compounding = CompoundingFrequency.QUARTERLY
    )

    @Test
    fun `round trip preserves every field`() {
        val map = FdFirestoreMapper.toMap(fd) + mapOf("createdAt" to fd.createdAt)
        val back = FdFirestoreMapper.fromMap(fd.id, map)!!
        assertEquals(fd, back)
    }

    @Test
    fun `dates are stored as epoch-day numbers per spec`() {
        val map = FdFirestoreMapper.toMap(fd)
        assertEquals(fd.openedDate.toEpochDay(), map["openedDate"])
        assertEquals(fd.maturityDate.toEpochDay(), map["maturityDate"])
    }

    @Test
    fun `legacy duration field is written for spec back-compat`() {
        assertEquals(12, FdFirestoreMapper.toMap(fd)["duration"])
    }

    @Test
    fun `enums are stored as names`() {
        val map = FdFirestoreMapper.toMap(fd)
        assertEquals("MONTHLY", map["payoutFrequency"])
        assertEquals("CAPITALIZE", map["renewOption"])
        assertEquals("RENEWED", map["status"])
        assertEquals("DAYS", map["tenorUnit"])
        assertEquals("QUARTERLY", map["compounding"])
    }

    @Test
    fun `unusable document is skipped not crashed`() {
        assertNull(FdFirestoreMapper.fromMap("x", emptyMap()))
        assertNull(FdFirestoreMapper.fromMap("x", mapOf("fdNumber" to "FD-1"))) // no bank/amount
    }

    @Test
    fun `bad enum and missing optionals fall back to model defaults`() {
        val back = FdFirestoreMapper.fromMap(
            "x",
            mapOf(
                "fdNumber" to "FD-1", "bank" to "Bank X", "amount" to 1000,
                "openedDate" to LocalDate.of(2026, 1, 1).toEpochDay(),
                "maturityDate" to LocalDate.of(2027, 1, 1).toEpochDay(),
                "status" to "NOT_A_STATUS", "compounding" to 42
            )
        )!!
        assertEquals(FdStatus.ACTIVE, back.status)
        assertEquals(CompoundingFrequency.SIMPLE, back.compounding)
        assertEquals(PayoutFrequency.AT_MATURITY, back.payoutFrequency)
        assertEquals(RenewOption.PAYOUT, back.renewOption)
        assertEquals(TenorUnit.MONTHS, back.tenorUnit)
        assertTrue(back.isActive)
        assertEquals(0, back.durationMonths)
        assertNull(back.parentFdId)
    }

    @Test
    fun `read accepts firestore integer types for numbers`() {
        // Firestore hands back Long for integers and Double for decimals.
        val back = FdFirestoreMapper.fromMap(
            "x",
            mapOf(
                "fdNumber" to "FD-1", "bank" to "Bank X", "amount" to 1000L,
                "openedDate" to 20464L, "maturityDate" to 20829L,
                "durationMonths" to 12L, "interestRate" to 10L, "durationDays" to 100L,
                "createdAt" to 1_700_000_000_000L
            )
        )!!
        assertEquals(1000.0, back.amount, 0.0)
        assertEquals(12, back.durationMonths)
        assertEquals(10.0, back.interestRate, 0.0)
        assertEquals(100, back.durationDays)
        assertEquals(1_700_000_000_000L, back.createdAt)
    }

    @Test
    fun `missing durationMonths falls back to legacy duration`() {
        val back = FdFirestoreMapper.fromMap(
            "x",
            mapOf(
                "fdNumber" to "FD-1", "bank" to "Bank X", "amount" to 1000,
                "openedDate" to 20464L, "maturityDate" to 20829L,
                "duration" to 24L
            )
        )!!
        assertEquals(24, back.durationMonths)
    }
}
