package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.ParadoxHaze
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Paradox Haze (TSP #71) — {2}{U} Enchantment — Aura
 *
 * "Enchant player
 *  At the beginning of enchanted player's first upkeep each turn, that player gets an additional
 *  upkeep step after this step."
 *
 * An "at the beginning of your upkeep, gain 1 life" witness counts the upkeep steps: two on the
 * enchanted player's turn (not an endless chain — the extra step isn't the turn's first upkeep),
 * with the turn's single draw only after both (CR 500.9: the step is inserted directly after this
 * one, so no draw step comes between them).
 */
class ParadoxHazeScenarioTest : FunSpec({

    val upkeepWitness = card("Upkeep Witness") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "At the beginning of your upkeep, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.GainLife(1)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ParadoxHaze)
        driver.registerCard(upkeepWitness)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Paradox Haze under [controller], attached straight to [enchanted] — enchant player needs a player host. */
    fun GameTestDriver.haze(controller: EntityId, enchanted: EntityId) {
        val haze = putPermanentOnBattlefield(controller, "Paradox Haze")
        replaceState(state.updateEntity(haze) { it.with(AttachedToComponent(enchanted)) })
    }

    test("the enchanted player gets one extra upkeep step, before their draw step") {
        val driver = newDriver()
        val me = driver.player1
        driver.haze(controller = me, enchanted = me)
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        driver.passPriorityUntil(Step.UPKEEP, me)
        val lifeBefore = driver.getLifeTotal(me)
        val handBefore = driver.getHandSize(me)

        driver.passPriorityUntil(Step.DRAW, me)

        withClue("two upkeep steps, so the witness triggered twice — and only twice") {
            driver.getLifeTotal(me) shouldBe lifeBefore + 2
        }
        withClue("a single draw step, after both upkeeps") {
            driver.getHandSize(me) shouldBe handBefore + 1
        }
    }

    test("an opponent's Haze on you still gives you the step — that player gets it") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.haze(controller = opponent, enchanted = me)
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        driver.passPriorityUntil(Step.UPKEEP, me)
        val lifeBefore = driver.getLifeTotal(me)
        val handBefore = driver.getHandSize(me)
        driver.passPriorityUntil(Step.DRAW, me)

        driver.getLifeTotal(me) shouldBe lifeBefore + 2
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("two Hazes on the same player give two extra upkeep steps") {
        val driver = newDriver()
        val me = driver.player1
        driver.haze(controller = me, enchanted = me)
        driver.haze(controller = me, enchanted = me)
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        driver.passPriorityUntil(Step.UPKEEP, me)
        val lifeBefore = driver.getLifeTotal(me)
        val handBefore = driver.getHandSize(me)
        driver.passPriorityUntil(Step.DRAW, me)

        withClue("both trigger in the first upkeep; neither re-triggers in the extra ones") {
            driver.getLifeTotal(me) shouldBe lifeBefore + 3
        }
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("other players' turns are untouched") {
        val driver = newDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.haze(controller = me, enchanted = me)
        driver.putPermanentOnBattlefield(opponent, "Upkeep Witness")

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        val lifeBefore = driver.getLifeTotal(opponent)
        driver.passPriorityUntil(Step.DRAW, opponent)

        driver.getLifeTotal(opponent) shouldBe lifeBefore + 1
    }
})
