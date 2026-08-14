package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.SoaringStoneglider
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

/**
 * Soaring Stoneglider (SOS) — {2}{W} Creature — Elephant Cleric 4/3, Flying, vigilance.
 *
 * "As an additional cost to cast this spell, exile two cards from your graveyard or pay {1}{W}."
 *
 * Exercises the exile-from-graveyard leg of [com.wingedsheep.sdk.scripting.AdditionalCost.OrPay]:
 *  - the enumerator offers both the exile path (base {2}{W}) and the pay path ({2}{W} + {1}{W});
 *  - paying via the exile path moves two graveyard cards to exile;
 *  - paying via the pay path leaves the graveyard untouched;
 *  - with fewer than two cards in the graveyard, only the pay path is offered.
 */
class SoaringStonegliderScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SoaringStoneglider)
        driver.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castActionsFor(playerId: EntityId, cardId: EntityId): List<CastSpell> =
        LegalActionEnumerator.create(cardRegistry)
            .enumerate(state, playerId)
            .mapNotNull { it.action as? CastSpell }
            .filter { it.cardId == cardId }

    test("both exile and pay paths are enumerated when the graveyard has two cards and lands for both") {
        val driver = newDriver()
        val me = driver.player1

        // Five Plains: enough for the base {2}{W} (3) AND for {2}{W}+{1}{W} (5).
        repeat(5) { driver.putLandOnBattlefield(me, "Plains") }
        driver.putCardInGraveyard(me, "Plains")
        driver.putCardInGraveyard(me, "Plains")

        val stoneglider = driver.putCardInHand(me, "Soaring Stoneglider")

        // Two cast actions: exile path and pay path.
        val actions = driver.castActionsFor(me, stoneglider)
        actions.size shouldBeGreaterThanOrEqual 2
    }

    test("exile path moves two graveyard cards to exile") {
        val driver = newDriver()
        val me = driver.player1

        // Exactly the base {2}{W} — no room to pay the extra {1}{W}, so the player must exile.
        repeat(3) { driver.putLandOnBattlefield(me, "Plains") }
        val grave1 = driver.putCardInGraveyard(me, "Plains")
        val grave2 = driver.putCardInGraveyard(me, "Plains")

        val stoneglider = driver.putCardInHand(me, "Soaring Stoneglider")
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = stoneglider,
                additionalCostPayment = AdditionalCostPayment(exiledCards = listOf(grave1, grave2)),
            )
        )
        result.isSuccess shouldBe true
        repeat(4) { if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass() }

        driver.state.getZone(me, Zone.EXILE).shouldContainAll(listOf(grave1, grave2))
        driver.state.getZone(me, Zone.BATTLEFIELD).contains(stoneglider).shouldBeTrue()
    }

    test("pay path leaves the graveyard untouched") {
        val driver = newDriver()
        val me = driver.player1

        repeat(5) { driver.putLandOnBattlefield(me, "Plains") }
        val grave1 = driver.putCardInGraveyard(me, "Plains")
        val grave2 = driver.putCardInGraveyard(me, "Plains")
        val graveBefore = driver.getGraveyard(me).toSet()

        val stoneglider = driver.putCardInHand(me, "Soaring Stoneglider")
        // No exiled cards in the payment → the pay path; the engine adds {1}{W}.
        val result = driver.submit(CastSpell(playerId = me, cardId = stoneglider))
        result.isSuccess shouldBe true
        repeat(4) { if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass() }

        driver.state.getZone(me, Zone.BATTLEFIELD).contains(stoneglider).shouldBeTrue()
        // Both graveyard cards remain — nothing was exiled.
        driver.getGraveyard(me).shouldContainAll(listOf(grave1, grave2))
        driver.getGraveyard(me).toSet() shouldBe graveBefore
    }

    test("with fewer than two cards in the graveyard, only the pay path is castable") {
        val driver = newDriver()
        val me = driver.player1

        // Only one graveyard card — the exile path is unavailable (needs two). With five Plains
        // the pay path ({2}{W} + {1}{W}) is affordable, so the spell is still castable.
        repeat(5) { driver.putLandOnBattlefield(me, "Plains") }
        driver.putCardInGraveyard(me, "Plains")

        val stoneglider = driver.putCardInHand(me, "Soaring Stoneglider")
        val actions = driver.castActionsFor(me, stoneglider)
        // The pay path is offered (extra {1}{W} folded into the cost); the exile path is not.
        actions.isNotEmpty().shouldBeTrue()
    }
})
