package com.wingedsheep.gameserver.session

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.Duration
import java.time.Instant

class AbandonedGamePolicyTest : FunSpec({
    val t0 = Instant.parse("2026-10-09T12:00:00Z")
    fun at(minutes: Long): Instant = t0 + Duration.ofMinutes(minutes)

    fun facts(
        hasHumanSeat: Boolean = true,
        hasConnectedHuman: Boolean = false,
        lastActivityAt: Instant = t0,
        unattendedSince: Instant? = t0,
    ) = AbandonedGamePolicy.Facts(hasHumanSeat, hasConnectedHuman, lastActivityAt, unattendedSince)

    test("a game with a connected human is never ended, however idle") {
        val f = facts(hasConnectedHuman = true, unattendedSince = null)
        AbandonedGamePolicy.endsAt(f).shouldBeNull()
        AbandonedGamePolicy.verdict(f, at(24 * 60)).shouldBeNull()
    }

    test("an idle game without a connected human ends after the idle limit") {
        val f = facts(lastActivityAt = t0, unattendedSince = t0)
        AbandonedGamePolicy.endsAt(f) shouldBe at(30)
        AbandonedGamePolicy.verdict(f, at(29)).shouldBeNull()
        AbandonedGamePolicy.verdict(f, at(30)).shouldNotBeNull() shouldContain "30 minutes without a move"
    }

    test("AIs still acting at a table the human left are ended after the unattended limit") {
        val f = facts(lastActivityAt = at(59), unattendedSince = t0)
        AbandonedGamePolicy.endsAt(f) shouldBe at(60)
        AbandonedGamePolicy.verdict(f, at(59)).shouldBeNull()
        AbandonedGamePolicy.verdict(f, at(60)).shouldNotBeNull() shouldContain "60 minutes without a connected player"
    }

    test("an all-AI game is only ended for idleness — being unattended is its point") {
        val f = facts(hasHumanSeat = false, lastActivityAt = at(100), unattendedSince = t0)
        AbandonedGamePolicy.endsAt(f) shouldBe at(130)
        AbandonedGamePolicy.verdict(f, at(129)).shouldBeNull()
        AbandonedGamePolicy.verdict(f, at(130)).shouldNotBeNull()
    }
})
