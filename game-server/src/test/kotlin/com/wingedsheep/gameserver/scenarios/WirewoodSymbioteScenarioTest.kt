package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Wirewood Symbiote.
 *
 * Card reference:
 * - Wirewood Symbiote ({G}): Creature — Insect 1/1
 *   "Return an Elf you control to its owner's hand: Untap target creature. Activate only once each turn."
 */
class WirewoodSymbioteScenarioTest : ScenarioTestBase() {

    init {
        context("Wirewood Symbiote - bounce Elf to untap creature") {

            test("untaps target creature when bouncing an Elf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wirewood Symbiote")
                    .withCardOnBattlefield(1, "Elvish Warrior", tapped = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val symbiote = game.findPermanent("Wirewood Symbiote")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Wirewood Symbiote")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Return the Elvish Warrior (an Elf) to untap Grizzly Bears
                val costPayment = AdditionalCostPayment(
                    bouncedPermanents = listOf(warrior)
                )

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = symbiote,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = costPayment
                    )
                )

                withClue("Ability should activate successfully") {
                    result.error shouldBe null
                }

                // Elvish Warrior should have been returned to hand
                withClue("Elvish Warrior should be in hand") {
                    val hand = game.state.getZone(game.player1Id, Zone.HAND)
                    hand.any { id ->
                        game.state.getEntity(id)?.get<CardComponent>()?.name == "Elvish Warrior"
                    } shouldBe true
                }

                // Elvish Warrior should not be on battlefield
                withClue("Elvish Warrior should not be on battlefield") {
                    game.findPermanent("Elvish Warrior") shouldBe null
                }

                // Resolve the ability from the stack
                game.resolveStack()

                // Grizzly Bears should be untapped
                withClue("Grizzly Bears should be untapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }

                // Once-per-turn tracking: symbiote should have the tracking component
                withClue("Symbiote should have ability activation tracked") {
                    val tracker = game.state.getEntity(symbiote)?.get<AbilityActivatedThisTurnComponent>()
                    tracker shouldNotBe null
                    tracker!!.hasActivated(ability.id) shouldBe true
                }
            }

            test("cannot activate more than once per turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wirewood Symbiote")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withCardOnBattlefield(1, "Wirewood Elf")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val symbiote = game.findPermanent("Wirewood Symbiote")!!
                val warrior = game.findPermanent("Elvish Warrior")!!
                val wirewoodElf = game.findPermanent("Wirewood Elf")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Wirewood Symbiote")!!
                val ability = cardDef.script.activatedAbilities.first()

                // First activation: bounce Elvish Warrior to untap Bears
                val firstResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = symbiote,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(warrior))
                    )
                )

                withClue("First activation should succeed") {
                    firstResult.error shouldBe null
                }

                game.resolveStack()

                // Second activation: try to bounce Wirewood Elf - should fail
                val secondResult = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = symbiote,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(wirewoodElf))
                    )
                )

                withClue("Second activation should fail due to once-per-turn restriction") {
                    secondResult.error shouldNotBe null
                }
            }

            test("cannot bounce a non-Elf creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wirewood Symbiote")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val symbiote = game.findPermanent("Wirewood Symbiote")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Wirewood Symbiote")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to bounce Grizzly Bears (not an Elf) - should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = symbiote,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(bears))
                    )
                )

                withClue("Should fail because Grizzly Bears is not an Elf") {
                    result.error shouldNotBe null
                }
            }

            test("can bounce itself (it is an Insect, not an Elf) - should fail") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wirewood Symbiote")
                    .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val symbiote = game.findPermanent("Wirewood Symbiote")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cardDef = cardRegistry.getCard("Wirewood Symbiote")!!
                val ability = cardDef.script.activatedAbilities.first()

                // Try to bounce Wirewood Symbiote itself (Insect, not Elf) - should fail
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = symbiote,
                        abilityId = ability.id,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        costPayment = AdditionalCostPayment(bouncedPermanents = listOf(symbiote))
                    )
                )

                withClue("Should fail because Wirewood Symbiote is an Insect, not an Elf") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
