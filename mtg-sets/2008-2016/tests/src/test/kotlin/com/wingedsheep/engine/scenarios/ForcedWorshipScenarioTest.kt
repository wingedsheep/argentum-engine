package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Forced Worship (NPH #11, reprinted in JMP and J22) — {1}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack.
 *   {2}{W}: Return this Aura to its owner's hand.
 */
class ForcedWorshipScenarioTest : ScenarioTestBase() {

    private val bounceAbilityId =
        cardRegistry.getCard("Forced Worship")!!.script.activatedAbilities.single().id

    init {
        context("Forced Worship") {

            test("enchanted creature can't attack but can still block") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Forced Worship", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                withClue("Enchanted creature can't attack") {
                    game.state.projectedState.cantAttack(bears) shouldBe true
                }
                withClue("Enchanted creature can still block") {
                    game.state.projectedState.cantBlock(bears) shouldBe false
                }
                withClue("An unenchanted creature is unaffected") {
                    game.state.projectedState.cantAttack(giant) shouldBe false
                }
                withClue("Declaring the enchanted creature as an attacker is rejected") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldNotBe null
                }
            }

            test("{2}{W} returns the Aura to its owner's hand and frees the creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Forced Worship", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Forced Worship")!!
                val result = game.execute(ActivateAbility(game.player1Id, aura, bounceAbilityId))
                withClue("Activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Forced Worship is back in its owner's hand") {
                    game.isInHand(1, "Forced Worship") shouldBe true
                    game.isOnBattlefield("Forced Worship") shouldBe false
                }
                withClue("The formerly enchanted creature can attack again") {
                    game.state.projectedState.cantAttack(game.findPermanent("Grizzly Bears")!!) shouldBe false
                }
            }

            test("a second activation does nothing once the Aura has already left (ruling)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Forced Worship", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Forced Worship")!!
                game.execute(ActivateAbility(game.player1Id, aura, bounceAbilityId)).error shouldBe null
                game.execute(ActivateAbility(game.player1Id, aura, bounceAbilityId)).error shouldBe null
                game.resolveStack()

                withClue("Forced Worship ends in its owner's hand exactly once") {
                    game.findCardsInHand(1, "Forced Worship").size shouldBe 1
                    game.isOnBattlefield("Forced Worship") shouldBe false
                }
            }
        }
    }
}
