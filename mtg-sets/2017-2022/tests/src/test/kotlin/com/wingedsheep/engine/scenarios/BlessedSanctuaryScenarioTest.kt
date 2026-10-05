package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Blessed Sanctuary (JMP #1, reprinted in J22).
 *
 * {3}{W}{W} Enchantment
 * "Prevent all noncombat damage that would be dealt to you and creatures you control.
 *  Whenever a nontoken creature you control enters, create a 2/2 white Unicorn creature token."
 */
class BlessedSanctuaryScenarioTest : ScenarioTestBase() {

    private fun damageOn(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    private fun unicornTokens(game: TestGame): Int =
        game.state.getBattlefield().count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.typeLine?.subtypes
                ?.any { it.value == "Unicorn" } == true
        }

    init {
        context("Blessed Sanctuary") {

            test("noncombat damage that would be dealt to you is prevented") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blessed Sanctuary")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shock = game.findCardsInHand(2, "Shock").single()
                game.execute(
                    CastSpell(game.player2Id, shock, listOf(ChosenTarget.Player(game.player1Id)))
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
            }

            test("noncombat damage that would be dealt to a creature you control is prevented") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blessed Sanctuary")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                damageOn(game, "Grizzly Bears") shouldBe 0
            }

            test("an opponent's creatures are not shielded") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blessed Sanctuary")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("combat damage dealt to you is not prevented") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blessed Sanctuary")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.getLifeTotal(1) shouldBe 18
            }

            test("a nontoken creature entering creates exactly one Unicorn; the token entering does not trigger") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blessed Sanctuary")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                // The Unicorn token is itself a creature you control entering — but it is a token,
                // so it does not trigger the Sanctuary again.
                unicornTokens(game) shouldBe 1
                game.state.stack.isEmpty() shouldBe true

                val unicorn = game.state.getBattlefield().single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.typeLine?.subtypes
                        ?.any { it.value == "Unicorn" } == true
                }
                val projected = game.state.projectedState
                projected.getPower(unicorn) shouldBe 2
                projected.getToughness(unicorn) shouldBe 2
                projected.getColors(unicorn) shouldBe setOf("WHITE")
            }

            test("an opponent's nontoken creature entering does not trigger") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blessed Sanctuary")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                unicornTokens(game) shouldBe 0
            }
        }
    }
}
