package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Desperate Sentry (EMN #21) — {2}{W} Creature — Human Soldier, 1/2.
 *
 *   When this creature dies, create a 3/2 colorless Eldrazi Horror creature token.
 *   Delirium — This creature gets +3/+0 as long as there are four or more card types among cards
 *   in your graveyard.
 */
class DesperateSentryScenarioTest : ScenarioTestBase() {

    init {
        context("Desperate Sentry delirium") {

            test("with three card types in the graveyard it is a 1/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desperate Sentry")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Desperate Sentry")!!
                game.state.projectedState.getPower(sentry) shouldBe 1
                game.state.projectedState.getToughness(sentry) shouldBe 2
            }

            test("with four card types in the graveyard it is a 4/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Desperate Sentry")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature
                    .withCardInGraveyard(1, "Lightning Bolt")  // instant
                    .withCardInGraveyard(1, "Lava Spike")      // sorcery
                    .withCardInGraveyard(1, "Ornithopter")     // artifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sentry = game.findPermanent("Desperate Sentry")!!
                game.state.projectedState.getPower(sentry) shouldBe 4
                game.state.projectedState.getToughness(sentry) shouldBe 2
            }
        }

        test("when it dies it leaves a 3/2 Eldrazi Horror token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Desperate Sentry")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sentry = game.findPermanent("Desperate Sentry")!!
            game.castSpell(1, "Lightning Bolt", sentry).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Desperate Sentry") shouldBe true
            val tokens = game.findAllPermanents("Eldrazi Horror Token")
            withClue("the dies trigger created one 3/2 Eldrazi Horror") {
                tokens shouldHaveSize 1
                game.state.projectedState.getPower(tokens.single()) shouldBe 3
                game.state.projectedState.getToughness(tokens.single()) shouldBe 2
            }
        }
    }
}
