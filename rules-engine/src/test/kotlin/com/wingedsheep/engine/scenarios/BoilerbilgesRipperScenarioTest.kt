package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Boilerbilges Ripper (DSK #127) — {4}{R} Human Assassin 4/4.
 *
 * "When this creature enters, you may sacrifice another creature or enchantment. When you do,
 *  this creature deals 2 damage to any target."
 *
 * The ETB is a [ReflexiveTriggerEffect]: "sacrifice another creature or enchantment" is a
 * resolution-time optional choice; the "When you do" reflexive trigger then deals 2 damage to
 * any target as it goes on the stack.
 */
class BoilerbilgesRipperScenarioTest : ScenarioTestBase() {

    init {
        context("Boilerbilges Ripper ETB sacrifice") {

            test("sacrificing another creature deals 2 damage to a chosen target (opponent)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Boilerbilges Ripper")
                    .withCardOnBattlefield(1, "Grizzly Bears") // fodder to sacrifice
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fodder = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Boilerbilges Ripper").error shouldBe null
                game.resolveStack() // resolve the creature spell + queue the ETB trigger

                // "You may sacrifice another creature or enchantment." — accept.
                if (game.hasPendingDecision()) game.answerYesNo(true)
                // Choose the fodder creature to sacrifice.
                if (game.hasPendingDecision()) game.selectTargets(listOf(fodder))
                game.resolveStack()

                // The reflexive "When you do" trigger now wants a damage target — the opponent.
                if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("Grizzly Bears was sacrificed to the graveyard") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 1
                }
                withClue("the opponent took 2 damage from the reflexive trigger") {
                    game.state.getEntity(game.player2Id)?.get<LifeTotalComponent>()?.life shouldBe 18
                }
            }

            test("declining the optional sacrifice deals no damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Boilerbilges Ripper")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Boilerbilges Ripper").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) game.answerYesNo(false)
                game.resolveStack()

                withClue("nothing was sacrificed") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                }
                withClue("the opponent took no damage") {
                    game.state.getEntity(game.player2Id)?.get<LifeTotalComponent>()?.life shouldBe 20
                }
            }
        }
    }
}
