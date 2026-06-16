package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ObekaSplitterOfSeconds
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Obeka, Splitter of Seconds (OTJ) — {1}{U}{B}{R} Legendary Creature — Ogre Warlock 2/5
 *
 * "Menace
 *  Whenever Obeka deals combat damage to a player, you get that many additional upkeep
 *  steps after this phase."
 *
 * Exercises the new [Effects.AddAdditionalUpkeepSteps] / `AdditionalUpkeepStepsComponent` path:
 * after Obeka (power 2) deals 2 combat damage, the controller gets two additional beginning
 * phases each containing an upkeep step (untap/draw skipped, CR 500.10). We prove this with an
 * "at the beginning of your upkeep" life-gain trigger on a witness permanent — it fires once in
 * the normal upkeep and twice more in the two inserted upkeep steps (CR 503.1a).
 */
class ObekaSplitterOfSecondsScenarioTest : FunSpec({

    // Witness permanent: gains 1 life at the beginning of each of its controller's upkeeps.
    val upkeepWitness = card("Upkeep Witness") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = "At the beginning of your upkeep, you gain 1 life."
        triggeredAbility {
            trigger = Triggers.YourUpkeep
            effect = Effects.GainLife(1)
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ObekaSplitterOfSeconds)
        driver.registerCard(upkeepWitness)
        driver.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("dealing 2 combat damage grants two additional upkeep steps after combat") {
        val driver = newDriver()
        val me = driver.player1

        val obeka = driver.putCreatureOnBattlefield(me, "Obeka, Splitter of Seconds")
        driver.removeSummoningSickness(obeka)
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        val lifeBefore = driver.getLifeTotal(me)

        // Attack the opponent. Obeka is 2/2 -> deals 2 combat damage.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(obeka), driver.player2)
        driver.bothPass() // first-strike / combat damage
        // Resolve the Obeka combat-damage trigger (queues 2 additional upkeep steps).
        driver.bothPass()

        // Advance through the rest of the turn. The two additional beginning phases (each an
        // upkeep step) are inserted after combat; the Upkeep Witness trigger fires in each, so the
        // controller gains 2 life beyond the normal upkeep before reaching the end step.
        driver.passPriorityUntil(Step.END)

        // 2 extra upkeep steps => +2 life from the witness (the normal upkeep already passed
        // before combat this turn, so these are purely the inserted ones).
        driver.getLifeTotal(me) shouldBe lifeBefore + 2
    }

    test("no combat damage means no additional upkeep steps") {
        val driver = newDriver()
        val me = driver.player1

        driver.putCreatureOnBattlefield(me, "Obeka, Splitter of Seconds")
        driver.putPermanentOnBattlefield(me, "Upkeep Witness")

        val lifeBefore = driver.getLifeTotal(me)

        // Never attack; just move to the end step.
        driver.passPriorityUntil(Step.END)

        driver.getLifeTotal(me) shouldBe lifeBefore
    }
})
