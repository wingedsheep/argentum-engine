package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.DonAndRaphHardScience
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Test for Don & Raph, Hard Science (TMT) — attacking grants the next noncreature spell you cast
 * this turn affinity for artifacts (GrantNextSpellAffinity + the PendingNextSpellAffinity rider read
 * by the cost calculator). With three artifacts out, a {3} noncreature spell becomes free.
 */
class DonAndRaphHardScienceTest : FunSpec({

    val testArtifact = card("Test Artifact") {
        manaCost = "{1}"; typeLine = "Artifact"
    }
    val testInstant = card("Test Instant Three") {
        manaCost = "{3}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DonAndRaphHardScience, testArtifact, testInstant))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("attacking grants the next noncreature spell affinity for artifacts (a {3} spell becomes free)") {
        val d = createDriver()
        val me = d.player1
        val opp = d.player2

        val donRaph = d.putCreatureOnBattlefield(me, "Don & Raph, Hard Science")
        d.removeSummoningSickness(donRaph)
        repeat(3) { d.putPermanentOnBattlefield(me, "Test Artifact") }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(donRaph), opp)
        // Resolve the attack trigger, which arms the affinity rider.
        var guard = 0
        while (d.state.pendingNextSpellAffinities.isEmpty() && guard < 20) {
            if (d.state.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
            guard++
        }
        d.state.pendingNextSpellAffinities.size shouldBe 1

        // A {3} noncreature spell is reduced by 3 (one per artifact) — castable with no mana.
        val spell = d.putCardInHand(me, "Test Instant Three")
        val cast = d.castSpell(me, spell)
        cast.error shouldBe null
        // The rider is one-shot: consumed by that cast.
        d.state.pendingNextSpellAffinities.size shouldBe 0
    }
})
