package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Relic Bane — "Enchant artifact. Enchanted artifact has 'At the beginning of your upkeep, you
 * lose 2 life.'" ({1}{B}{B} Aura, MRD #76)
 *
 * The upkeep trigger is *granted to the enchanted artifact*, not printed on the Aura, which is
 * what makes "your" mean the artifact's controller rather than Relic Bane's. These tests pin
 * that distinction: the same Aura drains its own controller when it enchants their artifact, and
 * drains the *opponent* when it enchants theirs. Granting onto a non-creature host is the part
 * worth proving — `GroupFilter.attachedCreature()` is scope-by-attachment despite the name, and
 * a filter that actually required a creature would make this card do nothing at all.
 */
class RelicBaneScenarioTest : ScenarioTestBase() {

    init {
        context("Relic Bane's granted upkeep trigger drains the enchanted artifact's controller") {

            test("enchanting an opponent's artifact drains the opponent, not Relic Bane's controller") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    // Bob controls the artifact; Alice controls the Aura on it.
                    .withCardOnBattlefield(2, "Steel Wall")
                    .withCardAttachedTo(1, "Relic Bane", "Steel Wall")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20

                // Advance into Bob's upkeep and let the granted trigger resolve.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Bob controls the enchanted artifact, so 'you lose 2 life' is Bob's") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Alice merely controls the Aura — the granted ability is not hers") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("it does not fire during the other player's upkeep") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Steel Wall")
                    .withCardAttachedTo(1, "Relic Bane", "Steel Wall")
                    // Start on Bob's turn so the next upkeep reached is Alice's, not Bob's.
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("the artifact is Alice's, so Alice pays at her own upkeep") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("Bob never controlled the artifact and loses nothing") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
