package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Reflecting Mirror (DRK #106).
 *
 * {4} Artifact
 * "{X}, {T}: Change the target of target spell with a single target if that target is you. The new
 *  target must be a player. X is twice the mana value of that spell."
 *
 * Two things worth pinning: the redirect works and only offers players, and an underpaid X does
 * nothing — the engine enforces "twice the mana value" at resolution, since ability cost
 * calculation can't see the chosen target.
 */
class ReflectingMirrorScenarioTest : ScenarioTestBase() {

    init {
        fun mirrorAbilityId() =
            cardRegistry.getCard("Reflecting Mirror")!!.script.activatedAbilities[0].id

        context("Reflecting Mirror") {

            test("paying twice the spell's mana value redirects it to another player") {
                // Lightning Bolt is mana value 1, so X must be at least 2.
                val game = scenario()
                    .withPlayers("Mirror", "Burner")
                    .withCardOnBattlefield(1, "Reflecting Mirror")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority()

                val bolt = game.state.stack.last()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Reflecting Mirror")!!,
                        abilityId = mirrorAbilityId(),
                        targets = listOf(entityIdToChosenTarget(game.state, bolt)),
                        xValue = 2,
                    )
                ).error shouldBe null
                game.resolveStack()

                // Choose the new target: only players are offered.
                val choice = game.state.pendingDecision
                choice.shouldNotBeNull()
                choice.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only players are legal new targets") {
                    choice.options.all { it in game.state.turnOrder } shouldBe true
                }
                game.submitDecision(
                    CardsSelectedResponse(choice.id, listOf(game.player2Id))
                )
                game.resolveStack()

                withClue("the Bolt was redirected to its own caster") {
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("underpaying X does nothing — the redirect keeps its printed price") {
                val game = scenario()
                    .withPlayers("Mirror", "Burner")
                    .withCardOnBattlefield(1, "Reflecting Mirror")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.passPriority()

                val bolt = game.state.stack.last()
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Reflecting Mirror")!!,
                        abilityId = mirrorAbilityId(),
                        targets = listOf(entityIdToChosenTarget(game.state, bolt)),
                        xValue = 1,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("X of 1 is under twice the Bolt's mana value, so nothing is redirected") {
                    game.getLifeTotal(1) shouldBe 17
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
