package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.CaughtInTheCrossfire
import com.wingedsheep.mtg.sets.definitions.otj.cards.ExplosiveDerailment
import com.wingedsheep.mtg.sets.definitions.otj.cards.ThunderSalvo
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Explosive Derailment. */
class ExplosiveDerailmentScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + ThunderSalvo + ExplosiveDerailment + CaughtInTheCrossfire
        )
        return driver
    }

    // ---------------------------------------------------------------------
    // Explosive Derailment (Spree)
    // ---------------------------------------------------------------------

    test("Explosive Derailment damage mode deals 4 to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        val spell = driver.putCardInHand(player, "Explosive Derailment")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2) // {R} base + {2} for the damage mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(giant)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(giant)))
            )
        )
        driver.bothPass()

        // 4 damage to a 3/3 -> dies.
        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
    }

    test("Explosive Derailment destroy mode destroys target artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val artifact = driver.putPermanentOnBattlefield(opponent, "Artifact Creature")

        val spell = driver.putCardInHand(player, "Explosive Derailment")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2) // {R} base + {2} for the destroy mode
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(artifact)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(artifact)))
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
    }

    test("Explosive Derailment both modes: 4 damage to a creature AND destroy an artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val artifact = driver.putPermanentOnBattlefield(opponent, "Artifact Creature")

        val spell = driver.putCardInHand(player, "Explosive Derailment")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 4) // {R} base + {2} + {2}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(artifact)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(giant)),
                    listOf(ChosenTarget.Permanent(artifact))
                )
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Centaur Courser") shouldBe null
        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
    }
})
