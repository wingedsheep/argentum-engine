package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Life Matrix (LEG #284) — "{4}, {T}: Put a matrix counter on target creature and that creature
 * gains 'Remove a matrix counter from this creature: Regenerate this creature.' Activate only
 * during your upkeep."
 *
 * Covers the upkeep-only restriction, the gained ability outliving the turn it was granted in, its
 * counter-removal cost paid from the creature (not Life Matrix), and the shield it makes.
 */
class LifeMatrixScenarioTest : ScenarioTestBase() {

    private val matrixAbilityId =
        cardRegistry.getCard("Life Matrix")!!.activatedAbilities.single().id

    init {
        context("Life Matrix") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Matrix Player", "Opponent")
                    .withCardOnBattlefield(1, "Life Matrix")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Forest")
                }
                return builder.build()
            }

            fun TestGame.activateMatrix(): ExecutionResult = execute(
                ActivateAbility(
                    player1Id,
                    findPermanent("Life Matrix")!!,
                    matrixAbilityId,
                    targets = listOf(ChosenTarget.Permanent(findPermanent("Grizzly Bears")!!))
                )
            )

            fun TestGame.matrixCounters(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MATRIX) ?: 0

            fun TestGame.grantedRegenId(id: EntityId): AbilityId? =
                state.grantedActivatedAbilities.firstOrNull { it.entityId == id }?.ability?.id

            test("can't be activated outside your upkeep") {
                val game = setup()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                game.activateMatrix().error shouldNotBe null
            }

            test("the gained ability lasts beyond the turn and spends the creature's matrix counter to regenerate") {
                val game = setup()
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id

                val activation = game.activateMatrix()
                withClue("activation during upkeep should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.matrixCounters(bears) shouldBe 1
                withClue("Bears gained the regenerate ability") { game.grantedRegenId(bears) shouldNotBe null }

                // The grant has no duration, so it survives the cleanup step and the opponent's turn.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                val regenId = game.grantedRegenId(bears)
                withClue("the gained ability is still there on a later turn") { regenId shouldNotBe null }

                val regen = game.execute(ActivateAbility(game.player1Id, bears, regenId!!))
                withClue("regenerating should succeed: ${regen.error}") { regen.error shouldBe null }
                game.resolveStack()
                withClue("the matrix counter paid the cost") { game.matrixCounters(bears) shouldBe 0 }

                withClue("with no matrix counter left the ability can't be activated again") {
                    game.execute(ActivateAbility(game.player1Id, bears, regenId)).error shouldNotBe null
                }

                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                withClue("the regeneration shield saved the Bears, tapped") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
