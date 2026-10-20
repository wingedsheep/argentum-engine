package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cosmic Horror (LEG #92).
 *
 * "At the beginning of your upkeep, destroy this creature unless you pay {3}{B}{B}{B}. If this
 *  creature is destroyed this way, it deals 7 damage to you."
 *
 * The rider is conditional on the destruction actually happening: a regenerated Horror survives
 * and deals no damage.
 */
class CosmicHorrorScenarioTest : ScenarioTestBase() {

    private fun atPlayer1Upkeep(swamps: Int, shield: Boolean = false): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Cosmic Horror", summoningSickness = false)
            .withLifeTotal(1, 20)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (swamps > 0) builder = builder.withLandsOnBattlefield(1, "Swamp", swamps)
        repeat(5) { builder = builder.withCardInLibrary(1, "Swamp") }
        repeat(5) { builder = builder.withCardInLibrary(2, "Swamp") }
        val game = builder.build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        if (shield) {
            val horror = game.findPermanent("Cosmic Horror")!!
            game.state = game.state.copy(
                floatingEffects = game.state.floatingEffects + regenerationShield(horror, game.player1Id)
            )
        }
        game.resolveStack()
        return game
    }

    init {
        context("Cosmic Horror upkeep tax") {

            test("declining destroys the Horror and it deals 7 damage to its controller") {
                val game = atPlayer1Upkeep(swamps = 6)
                game.answerYesNo(false)
                game.resolveStack()

                withClue("the Horror is destroyed") {
                    game.isOnBattlefield("Cosmic Horror") shouldBe false
                    game.isInGraveyard(1, "Cosmic Horror") shouldBe true
                }
                withClue("destroyed this way, it deals 7 damage to you") {
                    game.getLifeTotal(1) shouldBe 13
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("paying {3}{B}{B}{B} keeps the Horror and costs no life") {
                val game = atPlayer1Upkeep(swamps = 6)
                game.answerYesNo(true)
                game.resolveStack()

                game.isOnBattlefield("Cosmic Horror") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }

            test("a regenerated Horror wasn't destroyed this way, so it deals no damage") {
                val game = atPlayer1Upkeep(swamps = 0, shield = true)
                game.getPendingDecision()?.let { game.answerYesNo(false); game.resolveStack() }

                withClue("regeneration replaces the destruction") {
                    game.isOnBattlefield("Cosmic Horror") shouldBe true
                }
                withClue("no destruction, no 7 damage") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}

private fun regenerationShield(entityId: EntityId, controllerId: EntityId) = ActiveFloatingEffect(
    id = EntityId.generate(),
    effect = FloatingEffectData(
        layer = Layer.ABILITY,
        modification = SerializableModification.RegenerationShield,
        affectedEntities = setOf(entityId)
    ),
    duration = Duration.EndOfTurn,
    sourceId = null,
    controllerId = controllerId,
    timestamp = 1L
)
