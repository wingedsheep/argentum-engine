package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Biogenic Upgrade (RNA #123): "Distribute three +1/+1 counters among one, two, or three target
 * creatures, then double the number of +1/+1 counters on each of those creatures."
 *
 * It used to be scripted as divided *damage*; it puts counters, announced as it is cast, then
 * doubles every +1/+1 counter each target holds — ones it already had included.
 */
class BiogenicUpgradeScenarioTest : ScenarioTestBase() {
    init {
        test("distributes the three counters as announced, then doubles each target's +1/+1 counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Biogenic Upgrade")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!
            game.state = game.state.updateEntity(courser) { it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1)) }
            val spell = game.findCardsInHand(1, "Biogenic Upgrade").single()

            game.execute(CastSpell(game.player1Id, spell,
                targets = listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser)),
                damageDistribution = mapOf(bears to 2, courser to 1))).error shouldBe null
            game.resolveStack()

            plusOnes(game, bears) shouldBe 4
            plusOnes(game, courser) shouldBe 4
            game.getLifeTotal(2) shouldBe 20
        }
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
}
