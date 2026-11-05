package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Imminent Doom (HOU) — enters with a doom counter; whenever you cast a spell whose mana value
 * equals the doom-counter count, it deals that much damage to any target, then gains a doom counter.
 */
class ImminentDoomScenarioTest : ScenarioTestBase() {
    init {
        test("a spell matching the doom count triggers and advances the count; a stale count does not") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Imminent Doom")
                .withCardInHand(1, "Raging Goblin")
                .withCardInHand(1, "Savannah Lions")
                .withCardInHand(1, "Goblin Piker")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Imminent Doom").error shouldBe null
            game.resolveStack()
            val doom = game.findPermanent("Imminent Doom")!!
            fun doomCounters() = game.state.getEntity(doom)?.get<CountersComponent>()?.getCount(CounterType.DOOM) ?: 0
            doomCounters() shouldBe 1
            val startLife = game.getLifeTotal(2)

            // Mana value 1 == one doom counter: 1 damage, then a second counter.
            game.castSpell(1, "Raging Goblin").error shouldBe null
            game.hasPendingDecision() shouldBe true
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe startLife - 1
            doomCounters() shouldBe 2

            // Another mana value 1 spell no longer matches.
            game.castSpell(1, "Savannah Lions").error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.resolveStack()
            game.getLifeTotal(2) shouldBe startLife - 1
            doomCounters() shouldBe 2

            // Mana value 2 now matches: 2 damage, third counter.
            game.castSpell(1, "Goblin Piker").error shouldBe null
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe startLife - 3
            doomCounters() shouldBe 3
        }
    }
}
