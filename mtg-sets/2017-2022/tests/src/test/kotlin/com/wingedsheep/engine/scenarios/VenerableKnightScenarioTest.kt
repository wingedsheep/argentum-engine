package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Venerable Knight (ELD #35) — {W} Creature — Human Knight 2/1.
 *
 * "When this creature dies, put a +1/+1 counter on target Knight you control."
 */
class VenerableKnightScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Venerable Knight") {

            test("dying puts a +1/+1 counter on a Knight you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Venerable Knight")
                    .withCardOnBattlefield(1, "Knight of the Keep")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val venerable = game.findPermanent("Venerable Knight")!!
                val keep = game.findPermanent("Knight of the Keep")!!
                game.castSpell(1, "Lightning Bolt", venerable).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Venerable Knight") shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(keep)).error shouldBe null
                game.resolveStack()

                game.plusOneCounters(keep) shouldBe 1
                game.state.projectedState.getPower(keep) shouldBe 4
                game.state.projectedState.getToughness(keep) shouldBe 3
            }

            test("only Knights you control are legal targets") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Venerable Knight")
                    .withCardOnBattlefield(1, "Knight of the Keep")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Belle of the Brawl")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val venerable = game.findPermanent("Venerable Knight")!!
                val keep = game.findPermanent("Knight of the Keep")!!
                game.castSpell(1, "Lightning Bolt", venerable).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.legalTargets[0].shouldNotBeNull() shouldContainExactly listOf(keep)
            }
        }
    }
}
