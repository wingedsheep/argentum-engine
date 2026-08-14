package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.TrollsOfTelJilad
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Trolls of Tel-Jilad (MRD #136, {5}{G}{G}, Creature — Troll Shaman 5/6).
 *
 *   {1}{G}: Regenerate target green creature.
 *
 * The interesting part is the colour restriction on the target: any green creature qualifies —
 * including the Trolls themselves and creatures an opponent controls — while a non-green creature
 * is never a legal target.
 */
class TrollsOfTelJiladScenarioTest : ScenarioTestBase() {

    private val abilityId = TrollsOfTelJilad.activatedAbilities.single().id

    private fun TestGame.hasRegenShield(entityId: EntityId): Boolean =
        state.floatingEffects.any {
            it.effect.modification is SerializableModification.RegenerationShield &&
                entityId in it.effect.affectedEntities
        }

    init {
        context("Trolls of Tel-Jilad") {

            test("regenerates a green creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Trolls of Tel-Jilad", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val trolls = game.findPermanent("Trolls of Tel-Jilad")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.hasRegenShield(bears) shouldBe false

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = trolls,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating on a green creature should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Grizzly Bears should be carrying a regeneration shield") {
                    game.hasRegenShield(bears) shouldBe true
                }
            }

            test("a non-green creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Trolls of Tel-Jilad", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val trolls = game.findPermanent("Trolls of Tel-Jilad")!!
                val hillGiant = game.findPermanent("Hill Giant")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = trolls,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(hillGiant))
                    )
                )
                withClue("Targeting the red Hill Giant should be rejected") {
                    result.error shouldNotBe null
                }
                withClue("No shield should have been handed out") {
                    game.hasRegenShield(hillGiant) shouldBe false
                }
            }
        }
    }
}
