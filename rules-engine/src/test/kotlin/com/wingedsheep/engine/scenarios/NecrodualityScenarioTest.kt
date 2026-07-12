package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Necroduality (VOW #70) — {3}{U} Enchantment.
 *
 *   Whenever a nontoken Zombie you control enters, create a token that's a copy of that creature.
 *
 * Exercises the ETB observer trigger for nontoken Zombies you control: casting a plain Zombie
 * creature spawns a token copy alongside the original. Also verifies a nonZombie creature entering
 * does not trigger it, and that the created token (itself a Zombie) does not recursively trigger
 * another copy.
 */
class NecrodualityScenarioTest : ScenarioTestBase() {

    init {
        // A vanilla nontoken Zombie to cast — avoids the extra costs/triggers on the real VOW
        // Zombies (Cobbled Lancer's exile-a-creature-card additional cost, Undead Butler's own
        // ETB mill trigger) that would complicate isolating Necroduality's behavior.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Plain Zombie",
                manaCost = ManaCost.parse("{2}{B}"),
                subtypes = setOf(Subtype("Zombie")),
                power = 2,
                toughness = 2
            )
        )

        context("Necroduality") {

            test("a nontoken Zombie you control entering creates a token copy of it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Necroduality")
                    .withCardInHand(1, "Plain Zombie")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Plain Zombie").error shouldBe null
                game.resolveStack()

                val zombies = game.findPermanents("Plain Zombie")
                withClue("the original Zombie plus one token copy are on the battlefield") {
                    zombies.size shouldBe 2
                }

                val tokens = zombies.filter { game.state.getEntity(it)?.has<TokenComponent>() == true }
                withClue("exactly one of the two is a token") {
                    tokens.size shouldBe 1
                }
                val token = tokens.single()
                withClue("the token is controlled by the same player") {
                    game.state.getEntity(token)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                withClue("the token copy has the same power/toughness as the original") {
                    game.state.projectedState.getPower(token) shouldBe 2
                    game.state.projectedState.getToughness(token) shouldBe 2
                }
            }

            test("a non-Zombie creature entering does not trigger a token copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Necroduality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("only the original Grizzly Bears is on the battlefield, no copy") {
                    game.findPermanents("Grizzly Bears").size shouldBe 1
                }
            }
        }
    }
}
