package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Repulsor Blast (MSH #150) — {3}{R} Sorcery.
 *
 *   Teamwork 2
 *   Repulsor Blast deals 5 damage to target creature. If this spell was cast using teamwork, it
 *   also deals 2 damage to that creature's controller.
 *
 * Both branches are pinned: the 5 damage always lands, and the extra 2 to the creature's controller
 * only when the teamwork cost was declared (CR 702.194b).
 */
class RepulsorBlastScenarioTest : ScenarioTestBase() {

    init {
        context("Repulsor Blast") {

            test("cast without teamwork kills the creature and leaves its controller alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpell(1, "Repulsor Blast", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("5 damage kills the 3/3") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                }
                withClue("no teamwork, so its controller takes nothing") {
                    game.state.lifeTotal(game.player2Id) shouldBe 20
                }
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }

            test("cast using teamwork also deals 2 to that creature's controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Repulsor Blast")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpellWithTeamwork(
                    1, "Repulsor Blast", "Grizzly Bears", targetId = giant,
                ).error shouldBe null
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe false
                withClue("the teamwork rider burns the creature's controller for 2") {
                    game.state.lifeTotal(game.player2Id) shouldBe 18
                }
            }
        }
    }
}
