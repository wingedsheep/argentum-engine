package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.HeraldOfTheSun
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Herald of the Sun (M20 #23) — {4}{W}{W} Creature — Angel 4/4.
 * "Flying / {3}{W}: Put a +1/+1 counter on another target creature with flying."
 */
class HeraldOfTheSunScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy { HeraldOfTheSun.activatedAbilities.single().id }

    private fun setup(): TestGame = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Herald of the Sun")
        .withCardOnBattlefield(1, "Wind Drake")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.activateOn(target: EntityId) = execute(
        ActivateAbility(
            player1Id,
            findPermanent("Herald of the Sun")!!,
            abilityId,
            listOf(ChosenTarget.Permanent(target))
        )
    )

    init {
        context("Herald of the Sun") {
            test("puts a +1/+1 counter on another creature with flying") {
                val game = setup()
                val drake = game.findPermanent("Wind Drake")!!

                game.activateOn(drake).error shouldBe null
                game.resolveStack()

                withClue("Wind Drake gets one +1/+1 counter and becomes 3/3") {
                    game.plusOnes(drake) shouldBe 1
                    game.state.projectedState.getPower(drake) shouldBe 3
                    game.state.projectedState.getToughness(drake) shouldBe 3
                }
            }

            test("cannot target itself or a creature without flying") {
                val game = setup()
                val herald = game.findPermanent("Herald of the Sun")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("\"another\" excludes Herald of the Sun") {
                    game.activateOn(herald).error shouldNotBe null
                }
                withClue("Grizzly Bears has no flying") {
                    game.activateOn(bears).error shouldNotBe null
                }
            }
        }
    }
}
