package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Curious Inquiry (MKM #51, {U} Enchantment — Aura).
 *
 *   Enchant creature
 *   Enchanted creature gets +1/+1 and has "Whenever this creature deals combat damage to a
 *   player, investigate."
 *
 * The interesting part is that the trigger is *granted to the enchanted creature* rather than
 * bound to the Aura, so the Clue belongs to the creature's controller. This is the first Aura
 * in the card pool to use `GrantTriggeredAbility` (the shape previously only appeared on
 * Equipment), so both the self-enchant and the enchant-an-opponent's-creature cases are covered.
 */
class CuriousInquiryScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Curious Inquiry") {

            test("enchanted creature gets +1/+1 and investigates on combat damage to a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Curious Inquiry", "Grizzly Bears")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                run {
                    val projected = stateProjector.project(game.state)
                    withClue("Enchanted Grizzly Bears should be 3/3 (2/2 base +1/+1)") {
                        projected.getPower(bears) shouldBe 3
                        projected.getToughness(bears) shouldBe 3
                    }
                }

                clueCount(game, 1) shouldBe 0

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passPriority()
                game.resolveStack()

                withClue("Opponent took 3 combat damage from the enchanted 3/3") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("Dealing combat damage to a player investigated for the Aura's controller") {
                    clueCount(game, 1) shouldBe 1
                }
            }

            test("no combat damage to a player means no Clue") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Curious Inquiry", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 blocker eats the 3/3 attacker's damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears")))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passPriority()
                game.resolveStack()

                withClue("Damage went to a blocker, not a player — the granted trigger stays silent") {
                    clueCount(game, 1) shouldBe 0
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("enchanting an opponent's creature gives the Clue to that creature's controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Curious Inquiry", "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passPriority()
                game.resolveStack()

                withClue("Player 1 took 3 combat damage from the enchanted 3/3") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("The granted ability belongs to the enchanted creature's controller") {
                    clueCount(game, 2) shouldBe 1
                    clueCount(game, 1) shouldBe 0
                }
            }
        }
    }

    /** Clue tokens on the battlefield controlled by [playerNumber]. */
    private fun clueCount(game: TestGame, playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield().count { id ->
            val entity = game.state.getEntity(id) ?: return@count false
            entity.get<CardComponent>()?.name == "Clue" &&
                entity.get<ControllerComponent>()?.playerId == playerId
        }
    }
}
