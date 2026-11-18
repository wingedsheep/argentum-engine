package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.jou.cards.AkroanMastiff
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Akroan Mastiff (JOU #3) — {3}{W} Creature — Dog 2/2.
 *
 *   {W}, {T}: Tap target creature.
 */
class AkroanMastiffScenarioTest : ScenarioTestBase() {

    private val tapAbilityId = AkroanMastiff.activatedAbilities.first().id

    init {
        test("{W}, {T} taps the targeted opposing creature and the Mastiff itself") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Akroan Mastiff")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val mastiff = game.findPermanent("Akroan Mastiff")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = mastiff,
                    abilityId = tapAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the targeted creature is tapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
            withClue("the Mastiff tapped as part of the cost") {
                game.state.getEntity(mastiff)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
