package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Courier Bat (VOW #102) — {2}{B} Creature — Bat, 2/2, Flying.
 *
 *   When this creature enters, if you gained life this turn, return up to one target creature
 *   card from your graveyard to your hand.
 *
 * Exercises the intervening-if ETB trigger: without life gained this turn the trigger doesn't
 * return anything; after gaining life this turn, it returns a targeted creature card from the
 * graveyard to hand.
 */
class CourierBatScenarioTest : ScenarioTestBase() {

    init {
        // A simple life-gain spell to turn the "if you gained life this turn" condition on.
        cardRegistry.register(
            CardDefinition.instant(
                name = "Healing Salve Test",
                manaCost = ManaCost.parse("{W}"),
                oracleText = "You gain 3 life.",
                script = CardScript.spell(effect = GainLifeEffect(3, EffectTarget.Controller)),
            )
        )

        context("Courier Bat") {

            test("without life gained this turn, the ETB trigger does not return a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Courier Bat")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Courier Bat").error shouldBe null
                game.resolveStack()

                withClue("Courier Bat resolved onto the battlefield") {
                    game.isOnBattlefield("Courier Bat") shouldBe true
                }
                withClue("no life gained this turn -> Grizzly Bears stays in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }

            test("after gaining life this turn, returns a targeted creature card from the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Courier Bat")
                    .withCardInHand(1, "Healing Salve Test")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Healing Salve Test").error shouldBe null
                game.resolveStack()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.castSpell(1, "Courier Bat").error shouldBe null
                game.resolveStack() // Courier Bat enters -> ETB trigger asks for a target

                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("life was gained this turn -> Grizzly Bears returned to hand") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
