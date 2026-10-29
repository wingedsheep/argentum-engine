package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m21.cards.BasriDevotedPaladin
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Basri, Devoted Paladin (M21 #320).
 *
 * −1: Whenever a creature attacks this turn, put a +1/+1 counter on it.
 *
 * Pins the −1's delayed trigger: it puts the counter on the attacking creature itself before
 * blockers (ruling) and skips creatures that didn't attack.
 */
class BasriDevotedPaladinScenarioTest : ScenarioTestBase() {

    private val minusOne = BasriDevotedPaladin.activatedAbilities[1].id

    private fun TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.loyalty(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        test("−1: an attacking creature gets a +1/+1 counter before blockers; a non-attacker doesn't") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Basri, Devoted Paladin")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val basri = game.findPermanent("Basri, Devoted Paladin")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = basri, abilityId = minusOne)
            ).error shouldBe null
            game.resolveStack()
            game.loyalty(basri) shouldBe 3

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            withClue("the attacker gets the counter as the trigger resolves, before blockers") {
                game.plusOneCounters(bears) shouldBe 1
                game.state.projectedState.getPower(bears) shouldBe 3
            }
            withClue("a creature that didn't attack gets nothing") {
                game.plusOneCounters(giant) shouldBe 0
            }
        }
    }
}
