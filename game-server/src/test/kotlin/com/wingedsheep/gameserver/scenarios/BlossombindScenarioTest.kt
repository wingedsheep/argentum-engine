package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Blossombind.
 *
 * Card reference:
 * - Blossombind ({1}{U}): Enchantment — Aura
 *   "Enchant creature
 *    When this Aura enters, tap enchanted creature.
 *    Enchanted creature can't become untapped and can't have counters put on it."
 */
class BlossombindScenarioTest : ScenarioTestBase() {

    init {
        context("Blossombind") {

            test("ETB trigger taps the enchanted creature") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Blossombind")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Glory Seeker")!!

                val castResult = game.castSpell(1, "Blossombind", creature)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                val container = game.state.getEntity(creature)!!
                withClue("Glory Seeker should be tapped by Blossombind's ETB trigger") {
                    container.has<TappedComponent>() shouldBe true
                }
            }

            test("enchanted creature doesn't untap during its controller's untap step") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Blossombind")
                    .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Blossombind")!!
                val creature = game.findPermanent("Glory Seeker")!!
                game.state = game.state.updateEntity(aura) { container ->
                    container.with(AttachedToComponent(creature))
                }.updateEntity(creature) { container ->
                    container.with(AttachmentsComponent(listOf(aura)))
                }

                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val container = game.state.getEntity(creature)!!
                withClue("Glory Seeker should remain tapped due to DOESNT_UNTAP") {
                    container.has<TappedComponent>() shouldBe true
                }
            }

            test("enchanted creature can't have counters put on it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(2, "Blossombind")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Unspeakable Symbol")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val aura = game.findPermanent("Blossombind")!!
                val creature = game.findPermanent("Glory Seeker")!!
                val symbol = game.findPermanent("Unspeakable Symbol")!!
                game.state = game.state.updateEntity(aura) { container ->
                    container.with(AttachedToComponent(creature))
                }.updateEntity(creature) { container ->
                    container.with(AttachmentsComponent(listOf(aura)))
                }

                val symbolDef = cardRegistry.getCard("Unspeakable Symbol")!!
                val abilityId = symbolDef.activatedAbilities.first().id

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = symbol,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(creature))
                    )
                )
                withClue("Activating Unspeakable Symbol should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                game.resolveStack()

                val counters = game.state.getEntity(creature)?.get<CountersComponent>()
                val plusOneCount = counters?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("Glory Seeker should have 0 +1/+1 counters (CANT_RECEIVE_COUNTERS)") {
                    plusOneCount shouldBe 0
                }
            }
        }
    }
}
