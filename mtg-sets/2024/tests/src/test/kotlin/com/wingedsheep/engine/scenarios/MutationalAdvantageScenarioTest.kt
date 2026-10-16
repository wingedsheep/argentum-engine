package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mutational Advantage (PIP #111): the permanents you control with counters are locked in at
 * resolution — each gains hexproof and indestructible and has all damage to it prevented this
 * turn — then you proliferate. A permanent that only gains a counter afterwards (here, from a
 * later effect) is not protected.
 */
class MutationalAdvantageScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun damage(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("protects the locked set of countered permanents, then proliferates") {
            val game = scenario().withPlayers("Player1", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, "Mutational Advantage")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val lions = game.findPermanent("Savannah Lions")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)
            seed(game, lions, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Mutational Advantage").error shouldBe null
            game.resolveStack()

            // Proliferate: the opponent's countered Lions is a legal choice too; pick only Bears.
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            count(game, lions, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            val projected = game.state.projectedState
            projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            // Only permanents you control are affected.
            projected.hasKeyword(lions, Keyword.HEXPROOF) shouldBe false
            projected.hasKeyword(giant, Keyword.HEXPROOF) shouldBe false

            // Damage to the protected Bears is prevented.
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            damage(game, bears) shouldBe 0

            // Hill Giant gains a counter after resolution: it is not part of the locked set.
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE, 1)
            game.state.projectedState.hasKeyword(giant, Keyword.HEXPROOF) shouldBe false
            game.state.projectedState.hasKeyword(giant, Keyword.INDESTRUCTIBLE) shouldBe false
            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()
            damage(game, giant) shouldBe 3
        }

        test("a protected permanent stays protected after losing its counters") {
            val game = scenario().withPlayers("Player1", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Mutational Advantage")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            game.castSpell(1, "Mutational Advantage").error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) game.selectCards(emptyList()).error shouldBe null
            game.resolveStack()

            game.state = game.state.updateEntity(bears) { it.without<CountersComponent>() }
            game.state.projectedState.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            damage(game, bears) shouldBe 0
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
