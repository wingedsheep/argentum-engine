package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Binding Mummy (AKH #6) — {1}{W} Creature — Zombie 2/2.
 *
 *   Whenever another Zombie you control enters, you may tap target artifact or creature.
 */
class BindingMummyScenarioTest : ScenarioTestBase() {

    init {
        context("Binding Mummy") {

            fun game() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Binding Mummy")
                .withCardInHand(1, "Festering Mummy") // {B} Zombie
                .withCardInHand(1, "Grizzly Bears") // non-Zombie
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("another Zombie entering lets you tap target creature") {
                val game = game()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Festering Mummy").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is tapped by the trigger") {
                    game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("declining the may leaves the target untapped") {
                val game = game()
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Festering Mummy").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.state.getEntity(giant)!!.has<TappedComponent>() shouldBe false
            }

            test("a non-Zombie entering does not trigger") {
                val game = game()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("no target prompt for a non-Zombie") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
