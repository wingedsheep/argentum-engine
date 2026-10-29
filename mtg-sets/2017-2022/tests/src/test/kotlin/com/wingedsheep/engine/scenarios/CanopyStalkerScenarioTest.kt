package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.CanopyStalker
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Canopy Stalker — {3}{G} Creature — Cat 4/2
 * This creature must be blocked if able.
 * When this creature dies, you gain 1 life for each creature that died this turn.
 *
 * Pins the 2020-06-23 ruling: the dies trigger counts the Stalker itself, plus any other creature
 * (either player's) that died earlier this turn.
 */
class CanopyStalkerScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + CanopyStalker)
        initMirrorMatch(deck = Deck.of("Mountain" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun bolt(d: GameTestDriver, target: EntityId) {
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, targets = listOf(target)).outcome shouldBe Outcome.Done
        d.bothPass()
    }

    test("dying alone gains 1 life for itself") {
        val d = driver()
        val stalker = d.putCreatureOnBattlefield(d.player1, CanopyStalker.name)

        bolt(d, stalker)
        d.bothPass() // resolve the dies trigger

        d.getLifeTotal(d.player1) shouldBe 21
    }

    test("counts every creature that died this turn, including an opponent's") {
        val d = driver()
        val stalker = d.putCreatureOnBattlefield(d.player1, CanopyStalker.name)
        val lions = d.putCreatureOnBattlefield(d.player2, "Savannah Lions")

        bolt(d, lions)
        bolt(d, stalker)
        d.bothPass() // resolve the dies trigger

        d.getLifeTotal(d.player1) shouldBe 22
    }
})
