package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RushOfDread
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rush of Dread — {1}{B}{B} Sorcery (Spree).
 *
 * + {1} — Target opponent sacrifices half the creatures they control of their choice, rounded up.
 * + {2} — Target opponent discards half the cards in their hand, rounded up.
 * + {2} — Target opponent loses half their life, rounded up.
 *
 * Exercises the new dynamic-count `ForceSacrificeEffect` ("half … rounded up") alongside the
 * existing dynamic Discard / LoseLife, all gated by Spree's per-mode additional costs.
 */
class OtjRushOfDreadScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RushOfDread)
        driver.initMirrorMatch(Deck.of("Swamp" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("Mode 0: target opponent sacrifices half their creatures, rounded up") {
        val driver = newDriver()
        val caster = driver.player1
        val foe = driver.player2

        // Opponent controls 3 creatures → half rounded up = 2.
        repeat(3) { driver.putCreatureOnBattlefield(foe, "Grizzly Bears") }

        val spell = driver.putCardInHand(caster, "Rush of Dread")
        driver.giveMana(caster, Color.BLACK, 2) // {B}{B}
        driver.giveColorlessMana(caster, 2)      // {1} base + {1} mode 0
        driver.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(foe)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(foe)))
            )
        )
        driver.bothPass() // begin resolving → opponent's sacrifice decision

        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        // Must sacrifice exactly 2 of the 3 creatures.
        decision.minSelections shouldBe 2
        driver.submitCardSelection(foe, decision.options.take(2))

        driver.getCreatures(foe).size shouldBe 1
    }

    test("Mode 1: target opponent discards half their hand, rounded up") {
        val driver = newDriver()
        val caster = driver.player1
        val foe = driver.player2

        repeat(5) { driver.putCardInHand(foe, "Grizzly Bears") }
        val handBefore = driver.getHandSize(foe)
        val expectedDiscard = (handBefore + 1) / 2 // half, rounded up

        val spell = driver.putCardInHand(caster, "Rush of Dread")
        driver.giveMana(caster, Color.BLACK, 2)
        driver.giveColorlessMana(caster, 3) // {1} base + {2} mode 1
        driver.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(foe)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(foe)))
            )
        )
        driver.bothPass() // begin resolving → opponent's discard decision

        val decision = driver.pendingDecision
        (decision is SelectCardsDecision) shouldBe true
        decision as SelectCardsDecision
        decision.minSelections shouldBe expectedDiscard
        driver.submitCardSelection(foe, decision.options.take(expectedDiscard))

        driver.getHandSize(foe) shouldBe handBefore - expectedDiscard
    }

    test("Mode 2: target opponent loses half their life, rounded up") {
        val driver = newDriver()
        val caster = driver.player1
        val foe = driver.player2
        driver.setLifeTotal(foe, 21) // half rounded up = 11

        val spell = driver.putCardInHand(caster, "Rush of Dread")
        driver.giveMana(caster, Color.BLACK, 2)
        driver.giveColorlessMana(caster, 3) // {1} base + {2} mode 2
        driver.submitSuccess(
            CastSpell(
                playerId = caster,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(foe)),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(foe)))
            )
        )
        driver.bothPass()

        driver.getLifeTotal(foe) shouldBe 21 - 11
    }
})
