package com.wingedsheep.gameserver.stats

import com.wingedsheep.engine.core.BottomCards
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class StatsEligibilityTest : FunSpec({

    val alice = EntityId("alice")
    val bob = EntityId("bob")
    val card = EntityId("card")

    test("mulligans, concessions, passes and empty combat declarations are not plays of one's own") {
        // The last three are exactly what the server's auto-pass submits for a seat nobody is in.
        val idle = listOf(
            TakeMulligan(alice), BottomCards(alice, listOf(card)), KeepHand(alice),
            PassPriority(alice), DeclareAttackers(alice, emptyMap()), DeclareBlockers(alice, emptyMap()),
            Concede(alice),
        )
        StatsEligibility.playersWhoActed(idle) shouldBe emptySet()
    }

    test("a land, a real attack or a real block is a play") {
        val actions = listOf(
            KeepHand(alice), KeepHand(bob),
            PlayLand(alice, card),
            DeclareBlockers(bob, mapOf(card to listOf(card))),
        )
        StatsEligibility.playersWhoActed(actions) shouldBe setOf("alice", "bob")
        StatsEligibility.isOwnPlay(DeclareAttackers(alice, mapOf(card to bob))) shouldBe true
    }

    test("a no-show's loss and the win it hands over both drop out of a two-player game") {
        StatsEligibility.countsInStats(mapOf("alice" to false, "bob" to true)) shouldBe
            mapOf("alice" to false, "bob" to false)
    }

    test("in multiplayer the players who kept playing still have a result among themselves") {
        StatsEligibility.countsInStats(mapOf("a" to true, "b" to true, "c" to false)) shouldBe
            mapOf("a" to true, "b" to true, "c" to false)
    }

    test("an unknown seat reads as having played, so games from before tracking keep counting") {
        StatsEligibility.countsInStats(mapOf("a" to null, "b" to null)).values.toList() shouldContainExactly
            listOf(true, true)
    }
})
