package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Smoldering Werewolf // Erupting Dreadwolf (EMN #142).
 *
 *   Front (3/2) — "When this creature enters, it deals 1 damage to each of up to two target
 *                  creatures." / "{4}{R}{R}: Transform this creature."
 *   Back  (6/4) — "Whenever this creature attacks, it deals 2 damage to any target."
 *
 * Covers the up-to-two-target ETB (both slots used, and zero slots used), the activated flip, and
 * the back face's attack trigger.
 */
class SmolderingWerewolfScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature("Test Weakling", ManaCost.parse("{1}"), emptySet(), power = 1, toughness = 1)
        )

        context("Smoldering Werewolf") {

            test("entering deals 1 damage to each of two target creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Smoldering Werewolf")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Test Weakling")
                    .withCardOnBattlefield(2, "Test Weakling")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victims = game.findAllPermanents("Test Weakling")
                victims.size shouldBe 2

                game.castSpell(1, "Smoldering Werewolf").error shouldBe null
                game.resolveStack()

                // The enters trigger asks for its (up to two) targets.
                withClue("the enters trigger should ask for targets") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(victims)
                game.resolveStack()

                withClue("1 damage killed both 1/1s") {
                    game.findAllPermanents("Test Weakling") shouldBe emptyList()
                    game.isInGraveyard(2, "Test Weakling") shouldBe true
                }
            }

            test("the enters trigger may choose no targets at all") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Smoldering Werewolf")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(2, "Test Weakling")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Smoldering Werewolf").error shouldBe null
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.skipTargets()
                    game.resolveStack()
                }

                withClue("'up to two' allows zero — the 1/1 survives") {
                    game.findAllPermanents("Test Weakling").size shouldBe 1
                }
                game.isOnBattlefield("Smoldering Werewolf") shouldBe true
            }

            test("{4}{R}{R} transforms it into a 6/4 Erupting Dreadwolf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Smoldering Werewolf")!!
                val abilityId = cardRegistry.getCard("Smoldering Werewolf")!!.activatedAbilities.first().id

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = wolf, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("transformed to the back face") {
                    game.state.getEntity(wolf)!!.get<CardComponent>()!!.name shouldBe "Erupting Dreadwolf"
                    game.state.projectedState.getPower(wolf) shouldBe 6
                    game.state.projectedState.getToughness(wolf) shouldBe 4
                }
            }

            test("Erupting Dreadwolf's attack trigger deals 2 damage to any target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Smoldering Werewolf", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Smoldering Werewolf")!!
                val abilityId = cardRegistry.getCard("Smoldering Werewolf")!!.activatedAbilities.first().id
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = wolf, abilityId = abilityId)
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.state.getEntity(wolf)!!.get<CardComponent>()!!.name shouldBe "Erupting Dreadwolf"

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Erupting Dreadwolf" to 2)).error shouldBe null

                // The attack trigger targets the defending player.
                if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("2 damage from the attack trigger (combat damage has not happened yet)") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
