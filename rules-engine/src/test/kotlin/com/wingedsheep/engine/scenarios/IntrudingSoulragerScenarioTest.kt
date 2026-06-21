package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Intruding Soulrager (DSK #218) — {U}{R} Creature — Spirit 2/2.
 *
 * "Vigilance. {T}, Sacrifice a Room: This creature deals 2 damage to each opponent. Draw a card."
 *
 * Exercises the composite activated-ability cost ({T} + sacrifice a Room) and the resolution
 * effect (2 damage to each opponent + draw a card). The sacrifice cost is filtered to permanents
 * with the "Room" subtype the controller controls.
 */
class IntrudingSoulragerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("tap + sacrifice a Room deals 2 to each opponent and draws a card") {
        val driver = newDriver()
        val me = driver.player1
        val opp = driver.player2

        val soulrager = driver.putCreatureOnBattlefield(me, "Intruding Soulrager")
        driver.removeSummoningSickness(soulrager)
        // A Room to sacrifice as part of the cost.
        val room = driver.putPermanentOnBattlefield(me, "Unholy Annex // Ritual Chamber")

        val oppLifeBefore = driver.getLifeTotal(opp)
        val handBefore = driver.getHandSize(me)
        val abilityId = driver.cardRegistry.requireCard("Intruding Soulrager").activatedAbilities[0].id

        driver.submitSuccess(
            ActivateAbility(playerId = me, sourceId = soulrager, abilityId = abilityId)
        )

        // The sacrifice cost prompts a selection of which Room to sacrifice.
        var guard = 0
        while (guard++ < 20) {
            val pd = driver.pendingDecision
            if (pd is SelectCardsDecision) {
                driver.submitCardSelection(me, listOf(room))
            } else if (driver.state.stack.isNotEmpty()) {
                driver.bothPass()
            } else break
        }

        driver.getLifeTotal(opp) shouldBe oppLifeBefore - 2
        // Drew exactly one card.
        driver.getHandSize(me) shouldBe handBefore + 1
        // The Room is gone (sacrificed).
        driver.findPermanent(me, "Unholy Annex // Ritual Chamber") shouldBe null
    }
})
