package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Impostor Syndrome.
 *
 * Card reference:
 * - Impostor Syndrome ({4}{U}{U}): Enchantment
 *   "Whenever a nontoken creature you control deals combat damage to a player, create a
 *    token that's a copy of it, except it isn't legendary."
 */
class ImpostorSyndromeScenarioTest : ScenarioTestBase() {

    init {
        context("Impostor Syndrome cast") {

            test("lands on the battlefield as an enchantment when cast paying {4}{U}{U}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Impostor Syndrome")
                    .withLandsOnBattlefield(1, "Island", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Impostor Syndrome")
                withClue("Casting Impostor Syndrome should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Impostor Syndrome should be on the battlefield") {
                    game.isOnBattlefield("Impostor Syndrome") shouldBe true
                }
                withClue("Impostor Syndrome should not remain in the caster's hand") {
                    game.isInHand(1, "Impostor Syndrome") shouldBe false
                }
                withClue("Caster's mana pool should be empty after paying {4}{U}{U}") {
                    val manaPool = game.state.getEntity(game.player1Id)
                        ?.get<ManaPoolComponent>()
                    manaPool?.isEmpty shouldBe true
                }
                withClue("Impostor Syndrome should be an Enchantment on the battlefield") {
                    val permanent = game.findPermanent("Impostor Syndrome")
                    val cardComponent = game.state.getEntity(permanent!!)?.get<CardComponent>()
                    cardComponent?.typeLine?.isEnchantment shouldBe true
                }
            }
        }

        context("Impostor Syndrome trigger: combat damage to a player") {

            test("creates a non-legendary token copy when a legendary nontoken attacker connects") {
                // GIVEN P1 controls Impostor Syndrome and Rorix Bladewing (legendary, flying, haste, 6/5).
                // P2 has no blockers. P1 will attack and connect.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Impostor Syndrome")
                    .withCardOnBattlefield(1, "Rorix Bladewing")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.state.getBattlefield(game.player1Id).toSet()

                // WHEN Rorix attacks P2 alone, with no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attackResult = game.declareAttackers(mapOf("Rorix Bladewing" to 2))
                withClue("Declaring attacker should succeed: ${attackResult.error}") {
                    attackResult.error shouldBe null
                }

                // Advance through combat: damage step fires the Impostor Syndrome trigger,
                // both players auto-pass, the trigger resolves and creates the token copy.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // THEN exactly one new permanent is on P1's battlefield — a token copy of Rorix
                val after = game.state.getBattlefield(game.player1Id).toSet()
                val newEntities = after - before
                withClue("Exactly one new permanent should have appeared (the token copy)") {
                    newEntities.size shouldBe 1
                }

                val tokenId = newEntities.single()
                val tokenContainer = game.state.getEntity(tokenId)!!
                val tokenCard = tokenContainer.get<CardComponent>()!!

                withClue("New permanent must be marked as a token") {
                    (tokenContainer.get<TokenComponent>() != null) shouldBe true
                }
                withClue("Token must be controlled by the active player (the Impostor Syndrome controller)") {
                    tokenContainer.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }
                withClue("Token copies Rorix Bladewing's name") {
                    tokenCard.name shouldBe "Rorix Bladewing"
                }
                withClue("Token must NOT have the LEGENDARY supertype — 'except it isn't legendary'") {
                    (Supertype.LEGENDARY in tokenCard.typeLine.supertypes) shouldBe false
                }
                withClue("Token is still a creature (the Dragon copy)") {
                    tokenCard.typeLine.isCreature shouldBe true
                }
                withClue("Original Rorix is unchanged — still legendary") {
                    val originalId = game.state.getBattlefield(game.player1Id).first { id ->
                        val c = game.state.getEntity(id)!!
                        c.get<CardComponent>()?.name == "Rorix Bladewing" && !c.has<TokenComponent>()
                    }
                    val originalCard = game.state.getEntity(originalId)!!.get<CardComponent>()!!
                    (Supertype.LEGENDARY in originalCard.typeLine.supertypes) shouldBe true
                }
            }
        }
    }
}
