package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Headless Rider (VOW #372) — {2}{B} Creature — Zombie, 3/1.
 *
 *   Whenever this creature or another nontoken Zombie you control dies, create a 2/2 black
 *   Zombie creature token.
 *
 * Exercises the shared dies-trigger filter: killing Headless Rider itself creates a 2/2 black
 * Zombie token, and killing another nontoken Zombie you control also creates one.
 */
class HeadlessRiderScenarioTest : ScenarioTestBase() {

    private fun zombieTokenCount(game: TestGame, playerId: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getBattlefield(playerId).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Zombie Token"
        }

    init {
        context("Headless Rider — shared dies trigger") {

            test("Headless Rider dying to itself creates a 2/2 black Zombie token") {
                // Headless Rider is black, so removal must not be color-restricted; Lightning Bolt's
                // 3 damage is lethal to its 1 toughness.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Headless Rider", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rider = game.findPermanent("Headless Rider")!!
                val tokensBefore = zombieTokenCount(game, game.player1Id)

                game.castSpell(2, "Lightning Bolt", rider).error shouldBe null
                game.resolveStack() // Lightning Bolt resolves, Headless Rider dies (SBA)
                game.resolveStack() // dies trigger resolves, token created

                withClue("Headless Rider died") {
                    game.isOnBattlefield("Headless Rider") shouldBe false
                    game.isInGraveyard(1, "Headless Rider") shouldBe true
                }
                withClue("a 2/2 black Zombie token is created") {
                    zombieTokenCount(game, game.player1Id) shouldBe tokensBefore + 1
                }
            }

            test("another nontoken Zombie you control dying also creates a 2/2 black Zombie token") {
                // Diregraf Scavenger (a Zombie) is black — use unrestricted removal. It's a 2/3, so
                // Lightning Bolt's 3 damage is lethal.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Headless Rider", summoningSickness = false)
                    .withCardOnBattlefield(1, "Diregraf Scavenger", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val scavenger = game.findPermanent("Diregraf Scavenger")!!
                val tokensBefore = zombieTokenCount(game, game.player1Id)

                game.castSpell(2, "Lightning Bolt", scavenger).error shouldBe null
                game.resolveStack()
                game.resolveStack()

                withClue("Diregraf Scavenger died") {
                    game.isOnBattlefield("Diregraf Scavenger") shouldBe false
                }
                withClue("Headless Rider is still alive to see another Zombie die") {
                    game.isOnBattlefield("Headless Rider") shouldBe true
                }
                withClue("a 2/2 black Zombie token is created from another Zombie dying") {
                    zombieTokenCount(game, game.player1Id) shouldBe tokensBefore + 1
                }
            }
        }
    }
}
