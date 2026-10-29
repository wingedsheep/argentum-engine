package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `Effects.AddAdditionalUpkeepSteps(n, afterThisStep = true)` and `Conditions.IsFirstUpkeepStepOfTurn`.
 *
 * CR 500.9: steps added after a step are inserted directly after it, in the same phase — so the
 * extra upkeeps come before the draw step, and each re-fires "at the beginning of your upkeep".
 * Only the natural upkeep is the turn's first; an upkeep after it (added after this step, or after
 * a later phase as Obeka does) is not.
 */
class AdditionalUpkeepStepAfterThisStepTest : FunSpec({

    val doubler = card("Upkeep Doubler") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "At the beginning of your upkeep, if it's the first upkeep step of the turn, " +
            "you get two additional upkeep steps after this step."
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            interveningIf = Conditions.IsFirstUpkeepStepOfTurn
            effect = Effects.AddAdditionalUpkeepSteps(2, afterThisStep = true)
        }
    }

    val witness = card("Upkeep Witness") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "At the beginning of your upkeep, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            effect = Effects.GainLife(1)
        }
    }

    val firstOnlyWitness = card("First Upkeep Witness") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "At the beginning of your upkeep, if it's the first upkeep step of the turn, you gain 10 life."
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.UPKEEP)
            interveningIf = Conditions.IsFirstUpkeepStepOfTurn
            effect = Effects.GainLife(10)
        }
    }

    val phaseSplitter = card("Phase Splitter") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "You get an additional upkeep step after this phase."
        spell { effect = Effects.AddAdditionalUpkeepSteps(1) }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(doubler, witness, firstOnlyWitness, phaseSplitter))
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("two steps after this step: three upkeeps, then a single draw step") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Upkeep Doubler")
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        driver.passPriorityUntil(Step.UPKEEP, me)
        val lifeBefore = driver.getLifeTotal(me)
        val handBefore = driver.getHandSize(me)
        driver.passPriorityUntil(Step.DRAW, me)

        withClue("the witness fires in the natural upkeep and both added ones") {
            driver.getLifeTotal(me) shouldBe lifeBefore + 3
        }
        driver.getHandSize(me) shouldBe handBefore + 1
    }

    test("the next turn's first upkeep is first again") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "First Upkeep Witness")

        driver.passPriorityUntil(Step.UPKEEP, me)
        val lifeBefore = driver.getLifeTotal(me)
        driver.passPriorityUntil(Step.DRAW, me)
        driver.getLifeTotal(me) shouldBe lifeBefore + 10

        driver.passPriorityUntil(Step.UPKEEP, me)
        val lifeBeforeNext = driver.getLifeTotal(me)
        driver.passPriorityUntil(Step.DRAW, me)
        driver.getLifeTotal(me) shouldBe lifeBeforeNext + 10
    }

    test("an upkeep added after a later phase is not the turn's first") {
        val driver = newDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "First Upkeep Witness")
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        // Turn 1's natural upkeep is already behind us.
        val lifeBefore = driver.getLifeTotal(me)

        val splitter = driver.putCardInHand(me, "Phase Splitter")
        driver.giveMana(me, Color.BLUE)
        driver.castSpell(me, splitter).error shouldBe null
        driver.bothPass()

        driver.passPriorityUntil(Step.END, me)
        withClue("only the plain witness fires in the inserted upkeep") {
            driver.getLifeTotal(me) shouldBe lifeBefore + 1
        }
    }
})
