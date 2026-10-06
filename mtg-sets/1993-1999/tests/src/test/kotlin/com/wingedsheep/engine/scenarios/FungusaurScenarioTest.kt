package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fungusaur (LEA): "Whenever this creature is dealt damage, put a +1/+1 counter on it." Its ruling:
 * if more than one creature damages it at one time, it only gets one counter.
 */
class FungusaurScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(): Int =
        state.getEntity(findPermanent("Fungusaur")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("being dealt damage puts a +1/+1 counter on it") {
            // Holy Strength keeps the 3 damage short of lethal, so the counter has somewhere to go.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fungusaur")
                .withCardAttachedTo(1, "Holy Strength", "Fungusaur")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Fungusaur")!!).error shouldBe null
            game.resolveStack()

            game.plusCounters() shouldBe 1
            game.state.projectedState.getPower(game.findPermanent("Fungusaur")!!) shouldBe 4
        }

        test("two blockers damaging it at once give it one counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fungusaur")
                .withCardAttachedTo(1, "Holy Strength", "Fungusaur")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Fungusaur" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Llanowar Elves" to listOf("Fungusaur"), "Mons's Goblin Raiders" to listOf("Fungusaur"))
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.isOnBattlefield("Fungusaur") shouldBe true
            game.plusCounters() shouldBe 1
        }
    }
}
