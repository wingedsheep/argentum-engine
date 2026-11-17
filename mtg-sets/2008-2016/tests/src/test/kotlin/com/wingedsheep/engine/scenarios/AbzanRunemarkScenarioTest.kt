package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Abzan Runemark (FRF #3) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+2.
 *   Enchanted creature has vigilance as long as you control a black or green permanent.
 *
 * The vigilance gate reads the *projected* colours of the permanents the Aura's controller
 * controls: a colourless land turned green by Lifelace switches it on, and an opponent's green
 * creature never does.
 */
class AbzanRunemarkScenarioTest : ScenarioTestBase() {

    init {
        context("Abzan Runemark") {

            test("no vigilance without a black or green permanent; a land made green turns it on") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardAttachedTo(1, "Abzan Runemark", "Savannah Lions")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardInHand(1, "Lifelace")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!
                val forest = game.findPermanent("Forest")!!

                // The test registry's Savannah Lions (TestCards) is a 1/1, so +2/+2 makes it 3/3.
                game.state.projectedState.getPower(lions) shouldBe 3
                game.state.projectedState.getToughness(lions) shouldBe 3
                withClue("a white creature, a colourless land and an opponent's green creature don't count") {
                    game.state.projectedState.hasKeyword(lions, Keyword.VIGILANCE) shouldBe false
                }

                val cast = game.castSpell(1, "Lifelace", targetId = forest)
                withClue("Lifelace should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.state.projectedState.hasColor(forest, Color.GREEN) shouldBe true
                withClue("the Forest is now a green permanent you control") {
                    game.state.projectedState.hasKeyword(lions, Keyword.VIGILANCE) shouldBe true
                }
                game.state.projectedState.getPower(lions) shouldBe 3
            }

            test("a black permanent you control grants vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardAttachedTo(1, "Abzan Runemark", "Savannah Lions")
                    .withCardOnBattlefield(1, "Scathe Zombies")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!
                game.state.projectedState.hasKeyword(lions, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
