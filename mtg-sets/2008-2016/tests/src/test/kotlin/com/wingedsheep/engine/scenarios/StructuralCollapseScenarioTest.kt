package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Structural Collapse (GTC #107) — {5}{R} Sorcery.
 *
 *   Target player sacrifices an artifact and a land of their choice. Structural Collapse deals 2
 *   damage to that player.
 *
 * Covers the plain artifact-and-land edict on the targeted player, the artifact-land case (one
 * permanent can't answer both halves, so the artifact land must be the land), and the damage
 * landing on a player with nothing to sacrifice.
 */
class StructuralCollapseScenarioTest : ScenarioTestBase() {

    init {
        context("Structural Collapse") {

            test("the target player sacrifices an artifact and a land and takes 2 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Structural Collapse")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Structural Collapse", 2).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.isInGraveyard(2, "Island") shouldBe true
                game.findPermanents("Grizzly Bears").size shouldBe 1
                withClue("the caster's own lands are untouched") {
                    game.findPermanents("Mountain").size shouldBe 6
                }
                game.getLifeTotal(2) shouldBe 18
            }

            test("an artifact land can't be both, so the other artifact goes with it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Structural Collapse")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Seat of the Synod")
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Structural Collapse", 2).error shouldBe null
                game.resolveStack()

                withClue("only one assignment keeps two picks, so nothing is asked") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isInGraveyard(2, "Seat of the Synod") shouldBe true
                game.isInGraveyard(2, "Ornithopter") shouldBe true
                game.getLifeTotal(2) shouldBe 18
            }

            test("a player with no artifacts or lands still takes 2 damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Structural Collapse")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Structural Collapse", 2).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.findPermanents("Grizzly Bears").size shouldBe 1
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
