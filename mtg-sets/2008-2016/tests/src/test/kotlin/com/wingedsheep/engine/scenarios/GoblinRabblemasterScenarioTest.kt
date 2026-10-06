package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Goblin Rabblemaster (M15 #145, {2}{R}, Creature — Goblin Warrior 2/2).
 *
 *   Other Goblin creatures you control attack each combat if able.
 *   At the beginning of combat on your turn, create a 1/1 red Goblin creature token with haste.
 *   Whenever this creature attacks, it gets +1/+0 until end of turn for each other attacking Goblin.
 *
 * Worth proving: the attack requirement binds the other Goblins (including the fresh hasty token) but
 * not Rabblemaster itself, and the attack bonus counts only *other* attacking Goblins.
 */
class GoblinRabblemasterScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Goblin Rabblemaster") {

            fun setUp(): TestGame {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Goblin Rabblemaster", summoningSickness = false)
                    .withCardOnBattlefield(1, "Goblin Bully", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                return game
            }

            fun TestGame.goblinToken() = state.getBattlefield()
                .filter { state.getEntity(it)?.get<TokenComponent>() != null }
                .also { it shouldHaveSize 1 }
                .single()

            test("beginning of combat makes a hasty Goblin, and other Goblins must attack but Rabblemaster need not") {
                val game = setUp()
                val token = game.goblinToken()
                val bully = game.findPermanent("Goblin Bully")!!
                val bob = game.player2Id

                withClue("No attackers is illegal — the Bully and the token must attack") {
                    game.execute(
                        DeclareAttackers(game.state.activePlayerId!!, emptyMap())
                    ).error shouldNotBe null
                }
                withClue("Rabblemaster and the Bears may stay home") {
                    game.execute(
                        DeclareAttackers(
                            game.state.activePlayerId!!,
                            mapOf(bully to bob, token to bob)
                        )
                    ).error shouldBe null
                }
            }

            test("it gets +1/+0 for each other attacking Goblin, not for itself or a non-Goblin") {
                val game = setUp()
                val token = game.goblinToken()
                val rabblemaster = game.findPermanent("Goblin Rabblemaster")!!
                val bully = game.findPermanent("Goblin Bully")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val bob = game.player2Id

                game.execute(
                    DeclareAttackers(
                        game.state.activePlayerId!!,
                        mapOf(rabblemaster to bob, bully to bob, token to bob, bears to bob)
                    )
                ).error shouldBe null
                game.resolveStack()

                val after = stateProjector.project(game.state)
                after.getPower(rabblemaster) shouldBe 4
                after.getToughness(rabblemaster) shouldBe 2
            }
        }
    }
}
